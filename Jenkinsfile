node() {
    try {
        stage('Initialization') {
            env.OCP_API_SERVER = "${env.OPENSHIFT_API_URL}"
            env.OCP_TOKEN = readFile('/var/run/secrets/kubernetes.io/serviceaccount/token').trim()
        }

        node('jenkins-slave-ansible') {

            stage('Merge PR') {

                sh '''
                    git clone https://github.com/rht-labs/labs-ci-cd
                    git config --global user.email "robot@example.com"
                    git config --global user.name "A Robot"
                '''

                timeout(time: 1, unit: 'HOURS') {
                    def userInput = input(
                            id: 'userInput', message: 'Which PR # do you want to test?', parameters: [
                            [$class: 'StringParameterDefinition', description: 'PR #', name: 'pr'],
                            [$class: 'StringParameterDefinition', description: 'github token', name: 'token'],
                            [$class: 'StringParameterDefinition', description: 'github user name', name: 'username']
                    ])
                    env.PR_ID = userInput['pr']
                    env.PR_GITHUB_TOKEN = userInput['token']
                    env.PR_GITHUB_USERNAME = userInput['username']


                    if (env.PR_ID == null || env.PR_ID == ""){
                        error('PR_ID cannot be null or empty')
                    }
                    if (env.PR_GITHUB_TOKEN == null || env.PR_GITHUB_TOKEN == ""){
                        error('PR_GITHUB_TOKEN cannot be null or empty')
                    }
                    if (env.PR_GITHUB_USERNAME == null || env.PR_GITHUB_USERNAME == ""){
                        error('PR_GITHUB_USERNAME cannot be null or empty')
                    }
                }

                dir('labs-ci-cd') {

                    String getCommitShaScript = """
                        git fetch origin pull/${env.PR_ID}/head:pr
                        git checkout pr
                        git rev-parse HEAD
                    """

                    // set the vars
                    env.COMMIT_SHA = sh(returnStdout: true, script: getCommitShaScript)
                    env.USER_PASS = "${env.PR_GITHUB_USERNAME}:${env.PR_GITHUB_TOKEN}"
                    env.PR_STATUS_URI = "https://api.github.com/repos/rht-labs/labs-ci-cd/statuses/${env.COMMIT_SHA}"


                    def json = '''
                        {
                            "state": "pending",
                            "description": "job is running...",
                            "context": "Jenkins"
                        }
                    '''

                    sh "curl -u ${env.USER_PASS} -d '${json}' -H 'Content-Type: application/json' -X POST ${env.PR_STATUS_URI}"


                    sh """
                        git checkout master
                        git fetch origin pull/${env.PR_ID}/head:pr
                        git merge pr --ff
                    """
                }


            }

            stage('Apply Inventory') {
                dir('labs-ci-cd'){
                    sh '''
                        ansible-galaxy install -r requirements.yml --roles-path=roles
                        git clone https://github.com/sherl0cks/labs-demo
                        mv labs-demo/roles/make-demo-unique roles
                        rm -rf labs-demo
                        cp -r roles/casl-ansible/roles/* roles/
                        ls roles/
                    '''
                    sh 'ansible-playbook ci-playbook.yaml -i inventory/'
                }
            }

        }

        stage('Verify Results') {
            parallel(
                    'CI Builds': {
                        String[] builds = ['mvn-build-pod', 'npm-build-pod', 'zap-build-pod']

                        for (String build : builds) {
                            openshiftVerifyBuild(
                                    apiURL: "${env.OCP_API_SERVER}",
                                    authToken: "${env.OCP_TOKEN}",
                                    buildConfig: build,
                                    namespace: "labs-ci-cd-pr",
                                    waitTime: '10',
                                    waitUnit: 'min'
                            )
                        }
                    },
                    'CI Deploys': {
                        String[] ciDeployments = ['jenkins', 'nexus', 'sonarqube']

                        for (String deploy : ciDeployments) {
                            openshiftVerifyDeployment(
                                    apiURL: "${env.OCP_API_SERVER}",
                                    authToken: "${env.OCP_TOKEN}",
                                    depCfg: deploy,
                                    namespace: "labs-ci-cd-pr",
                                    verifyReplicaCount: true,
                                    waitTime: '10',
                                    waitUnit: 'min'
                            )
                        }
                    },
                    'App Deploys': {
                        String[] appDeployments = ['java-app']

                        for (String app : appDeployments) {
                            openshiftVerifyDeployment(
                                    apiURL: "${env.OCP_API_SERVER}",
                                    authToken: "${env.OCP_TOKEN}",
                                    depCfg: app,
                                    namespace: "labs-dev-pr",
                                    verifyReplicaCount: true,
                                    waitTime: '10',
                                    waitUnit: 'min'
                            )
                        }
                    }, failFast: true
            )

            def json = '''\
            {
                "state": "success",
                "description": "the job passed!",
                "context": "Jenkins"
            }'''

            sh "curl -u ${env.USER_PASS} -d '${json}' -H 'Content-Type: application/json' -X POST ${env.PR_STATUS_URI}"
        }

    }
    catch (e) {

        // we don't have the info to post a status, so short circuit
        if (env.COMMIT_SHA == null || env.COMMIT_SHA == "") {
            error("The pull request ID ${env.PR_ID} is invalid.")
        } else {

            def json = '''
            {
                "state": "failure",
                "description": "the job failed",
                "context": "Jenkins"
            }'''

            sh "curl -u ${env.USER_PASS} -d '${json}' -H 'Content-Type: application/json' -X POST ${env.PR_STATUS_URI}"

            throw e
        }
    }
}