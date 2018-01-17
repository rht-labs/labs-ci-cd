node(){
    stage('Initialize'){
        env.OCP_API_SERVER = "${env.OPENSHIFT_API_URL}"
        env.OCP_TOKEN = readFile('/var/run/secrets/kubernetes.io/serviceaccount/token').trim()
        env.NAMESPACE = env.OPENSHIFT_BUILD_NAMESPACE

    }
    node ('jenkins-slave-ansible'){
        stage ('Wait for Nexus'){
            openshiftVerifyDeployment(
                    apiURL: "${env.OCP_API_SERVER}",
                    authToken: "${env.OCP_TOKEN}",
                    depCfg: 'nexus',
                    namespace: "${env.NAMESPACE}",
                    verifyReplicaCount: true,
                    waitTime: '10',
                    waitUnit: 'min'
            )
        }
        stage ('Run Playbook'){
            sh '''
            git clone https://github.com/rht-labs/ansible-stacks
            cd ansible-stacks/roles/configure-nexus/tests/
            ansible-playbook -i inventory ocp-test.yml
        '''
        }
    }
}