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

    }

    // The options directive is for configuration that applies to the whole job.
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        timeout(time: 35, unit: 'MINUTES')
        // ansiColor('xterm')
        // timestamps()
    }

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
                    // taken from original j-file
                    timeout(time: 1, unit: 'HOURS') {
                        env.PR_ID = input(
                                id: 'userInput', message: 'Which PR # do you want to test?', parameters: [
                                [$class: 'StringParameterDefinition', description: 'PR #', name: 'pr']
                        ])
                        if (env.PR_ID == null || env.PR_ID == ""){
                            error('PR_ID cannot be null or empty')
                        }
                    }

                    // env.PR_GITHUB_TOKEN = sh (returnStdout: true, script : 'oc get secret labs-robot-github-oauth-token --template=\'{{.data.password}}\' | base64 -d')
                    env.PR_GITHUB_TOKEN = new String("oc get secret labs-robot-github-oauth-token --template='{{.data.password}}'".execute().text.minus("'").minus("'").decodeBase64())
                    env.PR_CI_CD_PROJECT_NAME = "labs-ci-cd-pr-${env.PR_ID}"
                    env.PR_DEV_PROJECT_NAME = "labs-dev-pr-${env.PR_ID}"
                    env.PR_TEST_PROJECT_NAME = "labs-test-pr-${env.PR_ID}"
                    if (env.PR_GITHUB_TOKEN == null || env.PR_GITHUB_TOKEN == ""){
                        error('PR_GITHUB_TOKEN cannot be null or empty')
                    }
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
         stage("merge PR") {
            agent {
                node {
                    label "jenkins-slave-ansible"
                }
            }
            when { 
                expression { return env.PR_ID }
            }
            steps {
                echo "Configuring Git, cloning labs-ci-cd & pushing revision to labs-robot/labs-ci-cd.git"
                sh """
                    git config --global user.email "labs.robot@gmail.com"
                    git config --global user.name "Labs Robot"
                    mkdir $HOME/.ssh
                    oc get secret labs-robot-ssh-privatekey --template=\'{{index .data \"ssh-privatekey\"}}\' | base64 -d >> $HOME/.ssh/id_rsa
                    chmod 0600 $HOME/.ssh/id_rsa
                    echo -e \"Host github.com\n\tStrictHostKeyChecking no\n\" >> $HOME/.ssh/config

                    git clone https://github.com/rht-labs/labs-ci-cd.git .
                    git remote add ci git@github.com:labs-robot/labs-ci-cd.git
                    git fetch origin pull/${env.PR_ID}/head:pr
                    git checkout pr
                    git rev-parse HEAD
                """

                echo "Pushing build state to the PR"
                dir('labs-ci-cd') {
                    script {
                        // set the vars
                        env.COMMIT_SHA = sh(returnStdout: true, script: "git rev-parse HEAD")
                        // env.COMMIT_SHA = "git rev-parse HEAD".execute().text.minus("'").minus("'")
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

                sh """
                    git checkout master
                    git fetch origin pull/${env.PR_ID}/head:pr
                    git merge pr --ff
                    git push ci master:pr-${env.PR_ID} -f
                """
                // save git source for next stage
                stash 'source'
            }
        }

        // Apply ansible inventory of ci-cd and it's ci-fo-ci-cd
        stage("apply inventory") {
            agent {
                node {
                    label "jenkins-slave-ansible"
                }
            }
            steps {
                notifyGitHub('''{
                            "state": "pending",
                            "description": "job is running...",
                            "context": "Apply Inventory"
                        }''')

                unstash 'source'
                echo "Applying inventory"
                sh 'ansible-galaxy install -r requirements.yml --roles-path=roles'
                sh "ansible-playbook ci-playbook.yml -i inventory/ -e \"target=bootstrap project_name_postfix=-pr-${env.PR_ID} scm_ref=pr-${env.PR_ID}\""
                sh "ansible-playbook ci-playbook.yml -i inventory/ -e \"target=tools project_name_postfix=-pr-${env.PR_ID} scm_ref=pr-${env.PR_ID}\""
                sh "ansible-playbook ci-playbook.yml -i inventory/ -e \"target=ci-for-labs project_name_postfix=-pr-${env.PR_ID} scm_ref=pr-${env.PR_ID}\""
                sh "ansible-playbook ci-playbook.yml -i inventory/ -e \"target=apps project_name_postfix=-pr-${env.PR_ID} scm_ref=pr-${env.PR_ID}\""
            }
            // Post can be used both on individual stages and for the entire build.
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

        // Run a start-build of the tests/slave/Jenkinsfile in the newly created Jenkins namespace
        // it contains a simple Jenkinsfile that starts each slave in paralle
        // and verifies the type of on it is eg that the npm slave has npm on the path 
        stage("test slaves") {
            steps {
                notifyGitHub('''{
                        "state": "pending",
                        "description": "test are running...",
                        "context": "Jenkins Slave Tests"
                    }''')

                echo "Running test-slaves-pipeline and verifyin it's been successful"
                sh """
                    oc start-build test-slaves-pipeline -w -n ${env.PR_CI_CD_PROJECT_NAME}
                """
                openshiftVerifyBuild(
                        apiURL: "${env.OCP_API_SERVER}",
                        authToken: "${env.OCP_TOKEN}",
                        bldCfg: "test-slaves-pipeline",
                        namespace: "${env.PR_CI_CD_PROJECT_NAME}",
                        waitTime: '10',
                        waitUnit: 'min'
                )       
            }
            // Post can be used both on individual stages and for the entire build.
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
        
        // parallel executiont to validate the JAVA App has deployed correctly and the 
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
                stage("App Deploys") {
                    steps {
                        notifyGitHub('''{
                                "state": "pending",
                                "description": " job is running...",
                                "context": "CI Deploys"
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
                echo "Removing old PR projects if they exist"
                clearProjects()
            }
        }
    }
    //  global post hook
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