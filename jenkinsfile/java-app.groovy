#!/usr/bin/groovy

/**
 this section of the pipeline executes on the master, which has a lot of useful variables that we can leverage to configure our pipeline
 **/
node (''){
    stage ('Initialize'){
        env.DEV_PROJECT = env.OPENSHIFT_BUILD_NAMESPACE.replace('ci-cd','dev')
        env.DEMO_PROJECT = env.OPENSHIFT_BUILD_NAMESPACE.replace('ci-cd','demo')


        /**
         these are used to configure which repository maven deploys
         the ci-cd starter will create a nexus that has this repos available
         **/
        env.MVN_SNAPSHOT_DEPLOYMENT_REPOSITORY = "nexus::default::http://nexus:8081/repository/maven-snapshots"
        env.MVN_RELEASE_DEPLOYMENT_REPOSITORY = "nexus::default::http://nexus:8081/repository/maven-releases"

        /**
         this value assumes the following convention, which is enforced by our default templates:
         - there are two build configs: one for s2i, one for this pipeline
         - the buildconfig for this pipeline is called my-app-name-pipeline
         - both buildconfigs are in the same project
         **/
        env.APP_NAME = "${env.JOB_NAME}".replaceAll(/-?${env.PROJECT_NAME}-?/, '').replaceAll(/-?pipeline-?/, '').replaceAll('/','')

        // these are defaults that will help run openshift automation
        env.OCP_API_SERVER = "${env.OPENSHIFT_API_URL}"
        env.OCP_TOKEN = readFile('/var/run/secrets/kubernetes.io/serviceaccount/token').trim()
    }
}


/**
 this section of the pipeline executes on a custom mvn build slave.
 you should not need to change anything below unless you need new stages or new integrations (e.g. Cucumber Reports or Sonar)
 **/
node('jenkins-slave-mvn') {

    stage('SCM Checkout') {
        git 'https://github.com/redhat-cop/spring-rest.git'
    }

    stage('Wait for Nexus') {
        // verify nexus is up or the build will fail with a strange error
        openshiftVerifyDeployment(
                apiURL: "${env.OCP_API_SERVER}",
                authToken: "${env.OCP_TOKEN}",
                depCfg: 'nexus',
                namespace: "${env.OPENSHIFT_BUILD_NAMESPACE}",
                verifyReplicaCount: true,
                waitTime: '3',
                waitUnit: 'min'
        )
        // now make sure the automation completed to set up the repos
        while (true) {
            def returnCode = sh(returnStdout: true, script: "curl -s -o /dev/null -w \"%{http_code}\" http://nexus:8081/service/siesta/repository/browse/redhat-public/")
            if (returnCode == '200') {
                break
            } else {
                echo "${returnCode}"
                sleep time: 10, unit: 'SECONDS'
            }
        }
    }

    stage('Build App') {
        sh "mvn clean install -DaltDeploymentRepository=${MVN_SNAPSHOT_DEPLOYMENT_REPOSITORY}"
    }

    // assumes uber jar is created
    stage('Build Image') {
        sh "oc start-build ${env.APP_NAME} --from-dir=target/ --follow"
    }
}

node (''){
    // no user changes should be needed below this point
    stage ('Deploy to Dev') {

        openshiftTag (apiURL: "${env.OCP_API_SERVER}", authToken: "${env.OCP_TOKEN}", destStream: "${env.APP_NAME}", destTag: 'latest', destinationAuthToken: "${env.OCP_TOKEN}", destinationNamespace: "${env.DEV_PROJECT}", namespace: "${env.OPENSHIFT_BUILD_NAMESPACE}", srcStream: "${env.APP_NAME}", srcTag: 'latest')

        openshiftVerifyDeployment (apiURL: "${env.OCP_API_SERVER}", authToken: "${env.OCP_TOKEN}", depCfg: "${env.APP_NAME}", namespace: "${env.DEV_PROJECT}", verifyReplicaCount: true)
    }

    stage ('Deploy to Demo') {
        input "Promote Application to Demo?"

        openshiftTag (apiURL: "${env.OCP_API_SERVER}", authToken: "${env.OCP_TOKEN}", destStream: "${env.APP_NAME}", destTag: 'latest', destinationAuthToken: "${env.OCP_TOKEN}", destinationNamespace: "${env.DEMO_PROJECT}", namespace: "${env.DEV_PROJECT}", srcStream: "${env.APP_NAME}", srcTag: 'latest')

        openshiftVerifyDeployment (apiURL: "${env.OCP_API_SERVER}", authToken: "${env.OCP_TOKEN}", depCfg: "${env.APP_NAME}", namespace: "${env.DEMO_PROJECT}", verifyReplicaCount: true)
    }

}