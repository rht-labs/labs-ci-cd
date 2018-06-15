node() {
    try {
        stage('Initialization') {
            env.OCP_API_SERVER = "${env.OPENSHIFT_API_URL}"
            env.OCP_TOKEN = readFile('/var/run/secrets/kubernetes.io/serviceaccount/token').trim()
            timeout(time: 1, unit: 'HOURS') {
                def userInput = input(
                        id: 'userInput', message: 'Which PR # do you want to test?', parameters: [
                        [$class: 'StringParameterDefinition', description: 'PR #', name: 'pr']
                ])
                env.PR_ID = userInput
                if (env.PR_ID == null || env.PR_ID == ""){
                    error('PR_ID cannot be null or empty')
                }
            }
        }

        node('jenkins-slave-ansible') {


            stage('Merge PR') {

                sh '''
                    git config --global user.email "labs.robot@gmail.com"
                    git config --global user.name "Labs Robot"
                    cd $HOME
                    mkdir .ssh
                    oc get secret labs-robot-ssh-privatekey --template=\'{{index .data \"ssh-privatekey\"}}\' | base64 -d >> .ssh/id_rsa
                    chmod 0600 .ssh/id_rsa
                    echo -e \"Host github.com\n\tStrictHostKeyChecking no\n\" >> .ssh/config
                '''

                sh '''
                    git clone https://github.com/rht-labs/labs-ci-cd
                    cd labs-ci-cd
                    git remote add ci git@github.com:labs-robot/labs-ci-cd.git
                '''

                env.PR_GITHUB_TOKEN = sh (returnStdout: true, script : 'oc get secret labs-robot-github-oauth-token --template=\'{{.data.password}}\' | base64 -d')
                env.PR_GITHUB_USERNAME = 'labs-robot'
                env.PR_CI_CD_PROJECT_NAME = "labs-ci-cd-pr-${env.PR_ID}"
                env.PR_DEV_PROJECT_NAME = "labs-dev-pr-${env.PR_ID}"
                env.PR_TEST_PROJECT_NAME = "labs-test-pr-${env.PR_ID}"

                // Delete projects if they already exist (In order to prevent issues with the projects already existing). 
                // Then wait some time to prevent trying to create a project when the delete command is still being tried as this can take a while
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

                if (env.PR_GITHUB_TOKEN == null || env.PR_GITHUB_TOKEN == ""){
                    error('PR_GITHUB_TOKEN cannot be null or empty')
                }
                if (env.PR_GITHUB_USERNAME == null || env.PR_GITHUB_USERNAME == ""){
                    error('PR_GITHUB_USERNAME cannot be null or empty')
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
                    sh 'ansible-galaxy install -r requirements.yml --roles-path=roles'
                    sh "ansible-playbook ci-playbook.yml -i inventory/ -e \"target=bootstrap project_name_postfix=-pr-${env.PR_ID} scm_ref=pr-${env.PR_ID}\""
                    sh "ansible-playbook ci-playbook.yml -i inventory/ -e \"target=tools project_name_postfix=-pr-${env.PR_ID} scm_ref=pr-${env.PR_ID}\""
                    sh "ansible-playbook ci-playbook.yml -i inventory/ -e \"target=ci-for-labs project_name_postfix=-pr-${env.PR_ID} scm_ref=pr-${env.PR_ID}\""
                    sh "ansible-playbook ci-playbook.yml -i inventory/ -e \"target=apps project_name_postfix=-pr-${env.PR_ID} scm_ref=pr-${env.PR_ID}\""
                    sh """
                        oc adm policy add-role-to-group admin labs-ci-cd-contributors -n ${env.PR_CI_CD_PROJECT_NAME}
                        oc adm policy add-role-to-group admin labs-ci-cd-contributors -n ${env.PR_DEV_PROJECT_NAME}
                        oc adm policy add-role-to-group admin labs-ci-cd-contributors -n ${env.PR_TEST_PROJECT_NAME}
                    """
                }
            }

        }

        stage('Test Slaves') {
            try { 
                def pending = '''
                    {
                        "state": "pending",
                        "description": "test are running...",
                        "context": "Jenkins Slave Tests"
                    }
                '''
                sh "curl -u ${env.USER_PASS} -d '${pending}' -H 'Content-Type: application/json' -X POST ${env.PR_STATUS_URI}"


                sh """
                    oc start-build test-slaves-pipeline -w -n ${env.PR_CI_CD_PROJECT_NAME}
                """
                openshiftVerifyBuild(
                        apiURL: "${env.OCP_API_SERVER}",
                        authToken: "${env.OCP_TOKEN}",
                        buildConfig: "test-slaves-pipeline",
                        namespace: "${env.PR_CI_CD_PROJECT_NAME}",
                        waitTime: '10',
                        waitUnit: 'min'
                )   
                def json = '''\
                {
                    "state": "success",
                    "description": "the job passed! :)",
                    "context": "Jenkins Slave Tests"
                }'''

                sh "curl -u ${env.USER_PASS} -d '${json}' -H 'Content-Type: application/json' -X POST ${env.PR_STATUS_URI}"

             }
            catch(err) {
                def failed = '''
                {
                    "state": "failure",
                    "description": "the job failed :(",
                    "context": "Jenkins Slave Tests"
                }'''
                sh "curl -u ${env.USER_PASS} -d '${json}' -H 'Content-Type: application/json' -X POST ${env.PR_STATUS_URI}"
            }           
        }
        
        stage('Verify Results') {
            parallel(
                    'CI Builds': {
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
                    },
                    'CI Deploys': {
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
                    },
                    'App Deploys': {
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
        stage('Clean up projects') {
            sh '''
                oc delete project $PR_CI_CD_PROJECT_NAME || rc=\$?
                oc delete project $PR_DEV_PROJECT_NAME || rc=\$?
                oc delete project $PR_TEST_PROJECT_NAME || rc=\$?
            '''
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