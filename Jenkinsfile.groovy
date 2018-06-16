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
        timeout(time: 20, unit: 'MINUTES')
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
        stage('clear projects') {
            steps {
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
                            "description": "ALL CI jobs are running U+1F914 ...",
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
                            "description": "job is running U+1F914 ...",
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
                                "description": "job completed U+1F600",
                                "context": "Apply Inventory"
                            }''')
                }
                failure {
                    notifyGitHub('''{
                                "state": "failure",
                                "description": "job failed U+1F615",
                                "context": "Apply Inventory"
                            }''')
                }
            }
        }
    }
    //  global post hook
    post {
        success {
            notifyGitHub('''{
                        "state": "success",
                        "description": "master ci job completed U+1F60B",
                        "context": "Jenkins"
                    }''')
        }
        failure {
            notifyGitHub('''{
                        "state": "failure",
                        "description": "master ci job failed U+1F62D",
                        "context": "Jenkins"
                    }''')
        }
    }
}