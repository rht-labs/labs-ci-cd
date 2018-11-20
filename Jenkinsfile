@NonCPS
def notifyGitHub(state) {
    sh "curl -u ${env.USER_PASS} -d '${state}' -H 'Content-Type: application/json' -X POST ${env.PR_STATUS_URI}"
}

def clearProjects(){
    sh """
        oc delete project $PR_CI_CD_PROJECT_NAME || rc=\$?
        oc delete project $PR_DEV_PROJECT_NAME || rc=\$?
        oc delete project $PR_TEST_PROJECT_NAME || rc=\$?
        while \${unfinished}
        do
            oc get project $PR_CI_CD_PROJECT_NAME || \
            oc get project $PR_DEV_PROJECT_NAME || \
            oc get project $PR_TEST_PROJECT_NAME || unfinished=false
        done
    """
}

pipeline {

    agent {
        label "master"
    }

    environment {
        // GLobal Vars
        JOB_NAME = "${JOB_NAME}".replace("/", "-")
        GIT_SSL_NO_VERIFY = true
        URL_TO_TEST = "https://google.com"
        PR_GITHUB_USERNAME = "labs-robot"

        def groupVars = readYaml file: 'inventory/group_vars/all.yml'

        def CI_CD_NAMESPACE = """${groupVars.ci_cd_namespace}"""
        def DEV_NAMESPACE = """${groupVars.dev_namespace}"""
        def TEST_NAMESPACE = """${groupVars.dev_namespace}"""

    }

    // The options directive is for configuration that applies to the whole job.
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        timeout(time: 35, unit: 'MINUTES')
        // ansiColor('xterm')
        // timestamps()
    }

    //  global post hook
    stages {
        // prepare environment and ask user for the PR-ID
        stage("prepare environment") {
            agent {
                node {
                    label "master"
                }
            }
            steps {
                echo "Setting up environment variables"
                script {
                    env.OCP_API_SERVER = "${env.OPENSHIFT_API_URL}"
                    env.OCP_TOKEN = readFile('/var/run/secrets/kubernetes.io/serviceaccount/token').trim()

                    timeout(time: 1, unit: 'HOURS') {
                        env.PR_ID = input(
                                id: 'userInput', message: 'Which PR # do you want to test?', parameters: [
                                [$class: 'StringParameterDefinition', description: 'PR #', name: 'pr']
                        ])
                        if (env.PR_ID == null || env.PR_ID == ""){
                            error('PR_ID cannot be null or empty')
                        }
                    }

                    // TODO: store this as and encrypted string in the repo
                    env.PR_GITHUB_TOKEN = new String("oc get secret labs-robot-github-oauth-token --template='{{.data.password}}'".execute().text.minus("'").minus("'").decodeBase64())

                    if (env.PR_GITHUB_TOKEN == null || env.PR_GITHUB_TOKEN == ""){
                        error('PR_GITHUB_TOKEN cannot be null or empty')
                    }

                    env.PR_CI_CD_PROJECT_NAME = "${env.CI_CD_NAMESPACE}-pr-${env.PR_ID}"
                    env.PR_DEV_PROJECT_NAME = "${env.DEV_NAMESPACE}-pr-${env.PR_ID}"
                    env.PR_TEST_PROJECT_NAME = "${env.TEST_NAMESPACE}-pr-${env.PR_ID}"
                    env.USER_PASS = "${env.PR_GITHUB_USERNAME}:${env.PR_GITHUB_TOKEN}"
                }
            }
        }

        // Clear any old or existing projects from the cluster to ensure a clean slate to test against
        stage('clear existing projects') {
            steps {
                echo "Removing old PR projects if they exist"
                clearProjects()
            }
        }

        // Merge PR to labs robot branch
        // uses sequential stages so same slave / workspace is preserved i.e. no need for stash
        // https://jenkins.io/blog/2018/07/02/whats-new-declarative-piepline-13x-sequential-stages/
        stage ('spin up shared ansible slave') {
            agent {
                node {
                    label "jenkins-slave-ansible"
                }
            }
            when {
                expression { return env.PR_ID }
            }
            stages {
                stage("merge PR") {

                    steps {
                        echo "Configuring Git, cloning labs-ci-cd & pushing revision to labs-robot/labs-ci-cd.git"

                        sshagent (credentials: ["${env.PR_CI_CD_PROJECT_NAME}-labs-robot-ssh-privatekey"]) {
                            status = sh script: """
                                git clone https://github.com/rht-labs/labs-ci-cd.git 
                                cd labs-ci-cd
                                git remote add ci git@github.com:labs-robot/labs-ci-cd.git
                                git fetch origin pull/${env.PR_ID}/head:pr
                                git checkout pr
                                git rev-parse HEAD
                            """, returnStatus: true
                        }

                        echo "Pushing build state to the PR"
                        dir('labs-ci-cd') {
                            script {
                                env.COMMIT_SHA = sh(returnStdout: true, script: "git rev-parse HEAD")

                                if (env.COMMIT_SHA == null || env.COMMIT_SHA == ""){
                                    error('could not get COMMIT_SHA')
                                }
                                env.PR_STATUS_URI = "https://api.github.com/repos/rht-labs/labs-ci-cd/statuses/${env.COMMIT_SHA}"
                            }
                        }

                        notifyGitHub('''{
                                    "state": "pending",
                                    "description": "ALL CI jobs are running...",
                                    "context": "Jenkins"
                                }''')

                        sshagent (credentials: ["${env.PR_CI_CD_PROJECT_NAME}-labs-robot-ssh-privatekey"]) {
                            sh """ 
                                cd labs-ci-cd
                                git checkout master
                                git fetch origin pull/${env.PR_ID}/head:pr
                                git merge pr --ff
                                git push ci master:pr-${env.PR_ID} -f
                            """
                        }
                    }
                }

                stage("Apply ansible inventory of ci-cd and it's ci-for-ci-cd") {
                    steps {

                        echo "Applying inventory"
                        dir('labs-ci-cd') {
                            // each its own line to that in blue ocean UI they show separately
                            sh "ansible-galaxy install -r requirements.yml --roles-path=roles"

                            //TODO: this would be the command to NOT use regex replace stuff:
                            // sh "ansible-playbook ci-playbook.yml -i inventory/ -e \"target=bootstrap ci_cd_namespace=${env.PR_CI_CD_PROJECT_NAME} dev_namespace=${env.PR_DEV_PROJECT_NAME} test_namespace=${env.PR_TEST_PROJECT_NAME} scm_ref=pr-${env.PR_ID}\""

                            sh "ansible-playbook ci-playbook.yml -i inventory/ -e \"target=bootstrap project_name_postfix=-pr-${env.PR_ID} scm_ref=pr-${env.PR_ID}\""
                            sh "ansible-playbook ci-playbook.yml -i inventory/ -e \"target=tools project_name_postfix=-pr-${env.PR_ID} scm_ref=pr-${env.PR_ID}\""
                            sh "ansible-playbook ci-playbook.yml -i inventory/ -e \"target=ci-for-labs project_name_postfix=-pr-${env.PR_ID} scm_ref=pr-${env.PR_ID}\""
                        }

                    }

                    post {
                        success {
                            notifyGitHub('''{
                                        "state": "success",
                                        "description": "job completed :)",
                                        "context": "Apply Inventory"
                                    }''')
                        }
                        failure {
                            notifyGitHub('''{
                                        "state": "failure",
                                        "description": "job failed :(",
                                        "context": "Apply Inventory"
                                    }''')
                        }
                    }
                }
            }
        }


        // Run a start-build of the tests/slave/Jenkinsfile in the newly created Jenkins namespace
        // it contains a simple Jenkinsfile that starts each slave in parallel
        // and verifies the type of on it is eg that the npm slave has npm on the path 
        stage("test slaves") {
            steps {
                notifyGitHub('''{
                        "state": "pending",
                        "description": "test are running...",
                        "context": "Jenkins Slave Tests"
                    }''')

                echo "Trigger builds of all the slaves"
                sh """oc get bc -n ${env.PR_CI_CD_PROJECT_NAME} -o name | grep jenkins-slave | cut -d'/'  -f 2 |awk -v namespace=\$PR_CI_CD_PROJECT_NAME {'print "oc start-build "\$0" -n "namespace '}  | sh"""

                echo "Running test-slaves-pipeline and verifying it's been successful"
                sh """oc start-build test-slaves-pipeline -w -n ${env.PR_CI_CD_PROJECT_NAME}"""

                openshiftVerifyBuild(
                        apiURL: "${env.OCP_API_SERVER}",
                        authToken: "${env.OCP_TOKEN}",
                        bldCfg: "test-slaves-pipeline",
                        namespace: "${env.PR_CI_CD_PROJECT_NAME}",
                        waitTime: '10',
                        waitUnit: 'min'
                )
            }

            post {
                success {
                    notifyGitHub('''{
                                "state": "success",
                                "description": "job completed :)",
                                "context": "Jenkins Slave Tests"
                            }''')
                }
                failure {
                    notifyGitHub('''{
                                "state": "failure",
                                "description": "job failed :(",
                                "context": "Jenkins Slave Tests"
                            }''')
                }
            }
        }

        // parallel execution to validate the JAVA App has deployed correctly and the
        // sonar, nexus and jenkins instances have come alive as expected.
        stage("Verifying Inventory") {
            parallel {
                stage("CI Builds") {
                    steps {
                        notifyGitHub('''{
                                "state": "pending",
                                "description": " job is running...",
                                "context": "CI Builds"
                            }''')

                        echo "Verifying the CI Builds have completed successfully"
                        script {
                            def ciPipelineResponse = sh(returnStdout: true, script:"oc get bc -l type=pipeline -n ${env.PR_CI_CD_PROJECT_NAME} -o name")
                            def ciPipelineList = []
                            for (entry in ciPipelineResponse.split()){
                                ciPipelineList += entry.replace('buildconfigs/','').replace('-pipeline','')
                            }

                            def ciBuildsResponse = sh(returnStdout: true, script:"oc get bc -l type=image -n ${env.PR_CI_CD_PROJECT_NAME} -o name")
                            def ciBuildsList = ciBuildsResponse.split()

                            for (String build : ciBuildsList) {
                                String buildName = build.replace('buildconfigs/','')
                                if( ciPipelineList.contains(buildName) ){
                                    // we have an image build with a corresponding pipeline build
                                    // for now, these builds aren't compatible with the openshiftVerifyBuild test we are going to do
                                    // so skip them here, but we'll test them indirectly via the deploy tests
                                } else {
                                    openshiftVerifyBuild(
                                            apiURL: "${env.OCP_API_SERVER}",
                                            authToken: "${env.OCP_TOKEN}",
                                            buildConfig: buildName,
                                            namespace: "${env.PR_CI_CD_PROJECT_NAME}",
                                            waitTime: '10',
                                            waitUnit: 'min'
                                    )
                                }
                            }
                        }
                    }
                    // Post can be used both on individual stages and for the entire build.
                    post {
                        success {
                            notifyGitHub('''{
                                        "state": "success",
                                        "description": "job completed :)",
                                        "context": "CI Builds"
                                    }''')
                        }
                        failure {
                            notifyGitHub('''{
                                        "state": "failure",
                                        "description": "job failed :(",
                                        "context": "CI Builds"
                                    }''')
                        }
                    }
                }
                stage("CI Deploys") {
                    steps {
                        notifyGitHub('''{
                                "state": "pending",
                                "description": " job is running...",
                                "context": "CI Deploys"
                            }''')

                        echo "Verifying the CI Deploys have completed successfully"
                        script {
                            def ciDeploysResponse = sh(returnStdout: true, script:"oc get dc -n ${env.PR_CI_CD_PROJECT_NAME} -o name")
                            def ciDeploysList = ciDeploysResponse.split()

                            for (String deploy : ciDeploysList) {
                                def deployName = deploy.replace('deploymentconfigs/','')
                                openshiftVerifyDeployment(
                                        apiURL: "${env.OCP_API_SERVER}",
                                        authToken: "${env.OCP_TOKEN}",
                                        depCfg: deployName,
                                        namespace: "${env.PR_CI_CD_PROJECT_NAME}",
                                        verifyReplicaCount: true,
                                        waitTime: '10',
                                        waitUnit: 'min'
                                )
                            }
                        }
                    }
                    // Post can be used both on individual stages and for the entire build.
                    post {
                        success {
                            notifyGitHub('''{
                                        "state": "success",
                                        "description": "job completed :)",
                                        "context": "CI Deploys"
                                    }''')
                        }
                        failure {
                            notifyGitHub('''{
                                        "state": "failure",
                                        "description": "job failed :(",
                                        "context": "CI Deploys"
                                    }''')
                        }
                    }
                }
                // TODO: potentially delete this because of removing "apps" from this project or get these from another repo and apply them
                stage("App Deploys") {
                    steps {
                        notifyGitHub('''{
                                "state": "pending",
                                "description": " job is running...",
                                "context": "App Deploys"
                            }''')

                        echo "Verifying the App Deploys (JAVA) have completed successfully"
                        script {
                            def appDeploysResponse = sh(returnStdout: true, script:"oc get dc -n ${env.PR_DEV_PROJECT_NAME} -o name")
                            def appDeploysList = appDeploysResponse.split()

                            for (String app : appDeploysList) {
                                def appName = app.replace('deploymentconfigs/','')
                                openshiftVerifyDeployment(
                                        apiURL: "${env.OCP_API_SERVER}",
                                        authToken: "${env.OCP_TOKEN}",
                                        depCfg: appName,
                                        namespace: "${env.PR_DEV_PROJECT_NAME}",
                                        verifyReplicaCount: true,
                                        waitTime: '15',
                                        waitUnit: 'min'
                                )
                            }
                        }
                    }
                    // Post can be used both on individual stages and for the entire build.
                    post {
                        success {
                            notifyGitHub('''{
                                        "state": "success",
                                        "description": "job completed :)",
                                        "context": "App Deploys"
                                    }''')
                        }
                        failure {
                            notifyGitHub('''{
                                        "state": "failure",
                                        "description": "job failed :(",
                                        "context": "App Deploys"
                                    }''')
                        }
                    }
                }
            }
        }

        // Clear any old or existing projects from the cluster to ensure a clean slate to test against
        stage('clean up CI projects created') {
            steps {
                echo "Removing created PR projects"
                clearProjects()
            }
        }
    }
    post {
        success {
            notifyGitHub('''{
                        "state": "success",
                        "description": "master ci job completed :)",
                        "context": "Jenkins"
                    }''')
        }
        failure {
            notifyGitHub('''{
                        "state": "failure",
                        "description": "master ci job failed :(",
                        "context": "Jenkins"
                    }''')
        }
    }
}
