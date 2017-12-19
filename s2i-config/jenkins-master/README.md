# Jenkins Master Configuration
This repo is used to build a customized OpenShift Jenkins 2 image with [source to image (S2I)](https://github.com/openshift/source-to-image). The base OpenShift Jenkins S2I can be found at `registry.access.redhat.com/openshift3/jenkins-2-rhel7`. The resulting image is a Jenkins master, and should be used in a master / slaves architecture. This image is configured to provide slaves as k8s pods via the [k8s Jenkins plugin](https://docs.openshift.com/container-platform/3.5/using_images/other_images/jenkins.html#using-the-jenkins-kubernetes-plug-in-to-run-jobs). Thus, this repo doesn't define any build tools or the like, as they are the responsibility of the slaves.


## How This Repo Works

The directory structure is dictated by [OpenShift Jenkins S2I image](https://docs.openshift.com/container-platform/3.5/using_images/other_images/jenkins.html#jenkins-as-s2i-builder). In particular:

- [plugins.txt](plugins.txt) is used to install plugins during the S2I build. If you want the details, here is the [S2I assemble script](https://github.com/openshift/jenkins/blob/master/2/contrib/s2i/assemble), which calls the [install jenkins plugins script](https://github.com/openshift/jenkins/blob/master/2/contrib/jenkins/install-plugins.sh).
- files in the [configuration](configuration) directory will have comments describing exactly what they do

## Hubot Integration

The jenkins s2i build includes the Hubot Steps plugin allowing communication to a running hubot instance. Currently, this repository supports Slack integration through Hubot to allow messages to be sent and to approve/abort builds through Slack.

To more easily integrate with Hubot set the following optional env variables in your Jenkins OpenShift template:
1. The url to HUBOT `HUBOT_URL`
2. The default Slack channel `HUBOT_DEFAULT_ROOM`

Once this is complete you can send Slack messages through Hubot.

Example
```
node {
   def url = env.HUBOT_URL
   def room = env.HUBOT_DEFAULT_ROOM

   stage('Hubot Test Pipeline') {
        hubotApprove message: "Proceed?", failOnError: true, room: room, url: url
    }
}
```

## Slack Integration

If you are running Slack integration through Hubot then you can skip this set up. This only enables sending messages directory to slack via the `slackSend` method.

To integrate with slack follow the steps at https://github.com/jenkinsci/slack-plugin. Particularly, create a webhook at  https://customteamname.slack.com/services/new/jenkins-ci. After the webhook setup is complete at slack, record and add the below environmental variables. You can retrieve the values on your [slack dashboard](https://my.slack.com/services/new/jenkins-ci). Make sure you are logged into the correct team.
1. The base url as `SLACK_BASE_URL`
2. The slack token as `SLACK_TOKEN`
3. The slack room you selected as the default slack channel as `SLACK_ROOM`
4. optionally, a jenkins credential can be used for the token and referenced by a custom id at `SLACK_TOKEN_CREDENTIAL_ID`. This takes precedences over the `SLACK_TOKEN`

## SonarQube Integration
 
 By default the deployment will attempt to connect to SonarQube and configure its setup including an authentication token. The default url is http://sonarqube:9000. This can be overriden adding an environment variable named `SONARQUBE_URL`. To disable SonarQube entirely set an environment variable named `DISABLE_SONAR` with any value.

## Contributing

There are some [helpers](helpers/README.MD) to get configuration out of a running Jenkins. 

