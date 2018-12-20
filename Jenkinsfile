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
                    //TODO GET THIS FROM A WEBHOOK NOT MANUALLY.
//                    timeout(time: 1, unit: 'HOURS') {
//                        env.PR_ID = input(
//                                id: 'userInput', message: 'Which PR # do you want to test?', parameters: [
//                                [$class: 'StringParameterDefinition', description: 'PR #', name: 'pr']
//                        ])
//                        if (env.PR_ID == null || env.PR_ID == ""){
//                            error('PR_ID cannot be null or empty')
//                        }
//                    }
                    env.PR_ID = '242'
                    // TODO PULL THIS INTO CURRENT PROJECT, POTENTIALLY PUT IT INTO THE SECRETS EXAMPLE
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
                        // origin isn't correct here because the PR is on the main repo
                        // commenting out to pretend that is it, since we've set this to my PR branch anyways
//                        git fetch origin pull/${env.PR_ID}/head:pr
//                        git checkout pr
                        sh """
                            git rev-parse HEAD
                        """

                        //TODO GET THIS FROM THE WEBHOOK
                        echo "Pushing build state to the PR"
                        script {
                            // set the vars
                            env.COMMIT_SHA = sh(returnStdout: true, script: "git rev-parse HEAD")
                            // env.COMMIT_SHA = "git rev-parse HEAD".execute().text.minus("'").minus("'")
                            if (env.COMMIT_SHA == null || env.COMMIT_SHA == ""){
                                error('could not get COMMIT_SHA')
                            }
                            env.PR_STATUS_URI = "https://api.github.com/repos/rht-labs/labs-ci-cd/statuses/${env.COMMIT_SHA}"
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
                        """
//                        git push ci master:pr-${env.PR_ID} -f
                    }
                }

                // Apply ansible inventory of ci-cd and it's ci-for-ci-cd
                stage("apply inventory") {
                    steps {

                        echo "Applying inventory"
                        dir('labs-ci-cd') {
                            // each its own line to that in blue ocean UI they show seperately
                            sh "ansible-galaxy install -r requirements.yml --roles-path=roles"
                            //TODO what about for secrets??? do i omit them? or run them separately? or put dummy/default values in there?
                            sh "ansible-playbook site.yml -e ci_cd_namespace=${env.PR_CI_CD_PROJECT_NAME} -e dev_namespace=${env.PR_DEV_PROJECT_NAME} -e test_namespace=${env.PR_TEST_PROJECT_NAME} -e role=admin"
                        }

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
            }
        }


        // TODO jenkins-slave-test pipeline needs to be run in the containers-quickstarts repo.

        // Validate the CI Builds & Deployments and App Deployments have deployed
        // correctly and come alive as expected.
        stage("Verifying CI Builds") {
            steps {
                notifyGitHub('''{
                        "state": "pending",
                        "description": " job is running...",
                        "context": "CI Builds"
                    }''')

                node('master') {
                    echo "Verifying the CI Builds have completed successfully"
                    script {
                        openshift.withCluster() {
                            openshift.withProject("${env.PR_CI_CD_PROJECT_NAME}") {
                                // Let's timeout after 5 minutes
                                timeout(5) {
                                    def pipelineBuildConfigs = openshift.selector('bc', [ type:'pipeline'])
                                    def imageBuildConfigs = openshift.selector('bc', [ type:'image'])
                                    def allDone = true
                                    imageBuildConfigs.withEach {
                                        echo "CI Builds: Checking ${it.name()}"
                                        def imageBuildName = it.name()
                                        def isPipelineBuild = false
                                        pipelineBuildConfigs.withEach {
                                            if (it.name() == imageBuildName) {
                                                isPipelineBuild = true
                                            }
                                        }

                                        if (isPipelineBuild == false) {
                                            if (it.object().status.phase != "Complete") {
                                                allDone = false
                                            }
                                        }
                                    }
                                    return allDone;
                                }
                            }
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
        stage("Verifying CI Deploys") {
            steps {
                notifyGitHub('''{
                        "state": "pending",
                        "description": " job is running...",
                        "context": "CI Deploys"
                    }''')

                node('master') {
                    echo "Verifying the CI Deploys have completed successfully"
                    script {
                        openshift.withCluster() {
                            openshift.withProject("${env.PR_CI_CD_PROJECT_NAME}") {
                                // Let's timeout after 5 minutes
                                timeout(5) {
                                    def deploymentConfigs = openshift.selector('dc')
                                    deploymentConfigs.withEach {
                                        echo "Checking ${env.PR_CI_CD_PROJECT_NAME}:${it.name()}"
                                        // this will wait until the desired replicas are available
                                        // - or be terminated at timeout
                                        it.rollout().status()
                                    }
                                }
                            }
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

        // TODO pull an example repo (probably containers-quickstarts or an example repo in rht-labs) with applier and run that. Instead of using an example app in this repository

        // Clear any old or existing projects from the cluster to ensure a clean slate to test against
        stage('Cleaning up CI projects created') {
            steps {
                echo "Removing created PR projects"
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
