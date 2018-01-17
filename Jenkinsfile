node() {
    try {
        stage('Initialization') {
            env.OCP_API_SERVER = "${env.OPENSHIFT_API_URL}"
            env.OCP_TOKEN = readFile('/var/run/secrets/kubernetes.io/serviceaccount/token').trim()
        }

        node('jenkins-slave-ansible') {


            stage('Merge PR') {

                sh '''
                    git config --global user.email "labs.robot@gmail.com"
                    git config --global user.name "Labs Robot"
                    cd $HOME
                    mkdir .ssh
                    oc get secret labs-robot-ssh-key --template=\'{{index .data \"ssh-privatekey\"}}\' | base64 -d >> .ssh/id_rsa
                    chmod 0600 .ssh/id_rsa
                    echo -e \"Host github.com\n\tStrictHostKeyChecking no\n\" >> .ssh/config
                '''

                sh '''
                    git clone https://github.com/rht-labs/labs-ci-cd
                    cd labs-ci-cd
                    git remote add ci git@github.com:labs-robot/labs-ci-cd.git
                '''

                timeout(time: 1, unit: 'HOURS') {
                    def userInput = input(
                            id: 'userInput', message: 'Which PR # do you want to test?', parameters: [
                            [$class: 'StringParameterDefinition', description: 'PR #', name: 'pr']
                    ])
                    env.PR_ID = userInput
                    env.PR_GITHUB_TOKEN = sh (returnStdout: true, script : 'oc get secret github-oauth-token --template=\'{{.data.password}}\' | base64 -d')
                    env.PR_GITHUB_USERNAME = 'labs-robot'


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
                        git push ci master:pr-${env.PR_ID} -f
                    """
                }


            }

            stage('Apply Inventory') {
                dir('labs-ci-cd'){
                    sh '''
                        ansible-galaxy install -r requirements.yml --roles-path=roles
                        cp -r roles/casl-ansible/roles/* roles/
                        ls roles/
                    '''
                    sh "ansible-playbook ci-playbook.yaml -i inventory/ -e \"scm_ref=pr-${env.PR_ID}\""
                }
            }

        }

        stage('Verify Results') {
            parallel(
                    'CI Builds': {
                        String[] builds = ['mvn-build-pod', 'npm-build-pod', 'zap-build-pod', 'jenkins-slave-ansible']

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