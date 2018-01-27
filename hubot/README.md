# Hubot Application For OpenShift, Slack and Jenkins 

This application will deploy hubot with the ability to automated build activity (eg proceed, abort) from Slack. When deploying hubot to OpenShift, it needs sufficient permission to invoke builds on Jenkins. This is accomplished by passing in the service account token that is running hubot to the API call to Jenkins as the Bearer token. 

## Configuration

1. Create the slack integration token for your slack team through the slack application at http://{yourslackteam}.slack.com. In the `Search App Directory` text box search for `hubot`. Follow the instructions on the page. You will need this piece of information from this configuration:
- The slack token
When this is complete, invite (through slack) the bot user to the channel you wish to be the default slack channel.

2. Use the template found [here](../openshift-templates/hubot/template.json) to deploy hubot to OpenShift in a project where Jenkins exists. 
- In the param file ensure you have set the `HUBOT_SLACK_TOKEN` that was created in step one. 
- Also ensure that the rest of the parameters are configured correctly. See the example file here (../params/hubot/build-and-deploy)

3. Use the jenkins-s2i-build template found [here](../openshift-templates/jenkins-s2i-build/template.json) to build the jenkins image.

4. Use the jenkins-deploy template found [here](../openshift-templates/jenkins-deploy/template.json) to deploy to OpenShift in the same project as hubot.
- In the param file for the jenkins deployment add values for `HUBOT_URL` (by default http://hubot:8080/) and `HUBOT_DEFAULT_ROOM` (by default jenkins). These values can be overridden in the Jenkinsfile.

## Usage

Configure a Jenkins pipeline that uses a Jenkinsfile. Below is an example:
```
node {
   def url = env.HUBOT_URL
   def room = env.HUBOT_DEFAULT_ROOM
   
   stage('Hubot Test Pipeline') {
        hubotApprove message: "Proceed?", failOnError: true, room: room, url: url
    }
}
```
