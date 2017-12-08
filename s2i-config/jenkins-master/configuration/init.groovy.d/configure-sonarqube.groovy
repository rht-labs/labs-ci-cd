#!/usr/bin/env groovy
import jenkins.model.*
import groovy.json.JsonSlurper
import hudson.plugins.sonar.SonarInstallation
import hudson.plugins.sonar.SonarRunnerInstallation
import hudson.plugins.sonar.SonarRunnerInstaller
import hudson.plugins.sonar.model.TriggersConfig
import hudson.tools.InstallSourceProperty

import java.util.logging.Level
import java.util.logging.Logger
import static hudson.plugins.sonar.utils.SQServerVersions.SQ_5_3_OR_HIGHER

final def LOG = Logger.getLogger("LABS")

def disableSonar = System.getenv("DISABLE_SONAR");
if(disableSonar != null && disableSonar.toUpperCase() == "TRUE") {
    LOG.log(Level.INFO, 'Skipping SonarQube configuration')
    return
}

LOG.log(Level.INFO, 'Configuring SonarQube')
def sonarConfig = Jenkins.instance.getDescriptor('hudson.plugins.sonar.SonarGlobalConfiguration')

def sonarHost = System.getenv("SONARQUBE_URL");
if(sonarHost == null) {
    //default
    sonarHost = "http://sonarqube:9000"
}

def tokenName = 'Jenkins'

def token = null
def failCount = 0

def rc = 0

while (rc != 200) {
    def testConnect = new URL("${sonarHost}").openConnection()
    testConnect.setRequestMethod("GET")

    rc = testConnect.getResponseCode()
    if (rc != 200) {
        failCount += 1
        if (failCount>=20) {
            break
        }
        sleep(6000)
    }
}

if (rc == 200) {
    // Make a POST request to delete any existing admin tokens named "Jenkins"
    LOG.log(Level.INFO, 'Delete existing SonarQube Jenkins token')
    def revokeToken = new URL("${sonarHost}/api/user_tokens/revoke").openConnection()
    def message = "name=${tokenName}&login=admin"
    revokeToken.setRequestMethod("POST")
    revokeToken.setDoOutput(true)
    revokeToken.setRequestProperty("Accept", "application/json")
    def authString = "admin:admin".bytes.encodeBase64().toString()
    revokeToken.setRequestProperty("Authorization", "Basic ${authString}")
    revokeToken.getOutputStream().write(message.getBytes("UTF-8"))
    rc = revokeToken.getResponseCode()

    // Create a new admin token named "Jenkins" and capture the value
    LOG.log(Level.INFO, 'Generate new auth token for SonarQube/Jenkins integration')
    def generateToken = new URL("${sonarHost}/api/user_tokens/generate").openConnection()
    message = "name=${tokenName}&login=admin"
    generateToken.setRequestMethod("POST")
    generateToken.setDoOutput(true)
    generateToken.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    generateToken.setRequestProperty("Authorization", "Basic ${authString}")
    generateToken.getOutputStream().write(message.getBytes("UTF-8"))

    LOG.log(Level.INFO, 'Successfully generated SonarQube auth token')
    def jsonBody = generateToken.getInputStream().getText()
    def jsonParser = new JsonSlurper()
    def data = jsonParser.parseText(jsonBody)
    token = data.token

    // Add the SonarQube server config to Jenkins
    SonarInstallation sonarInst = new SonarInstallation(
        "sonar", sonarHost, SQ_5_3_OR_HIGHER, token, "", "", "", "", "", new TriggersConfig(), "", "", ""
    )
    sonarConfig.setInstallations(sonarInst)
    sonarConfig.setBuildWrapperEnabled(true)
    sonarConfig.save()

    // Sonar Runner
    // Source: http://pghalliday.com/jenkins/groovy/sonar/chef/configuration/management/2014/09/21/some-useful-jenkins-groovy-scripts.html
    def inst = Jenkins.getInstance()

    def sonarRunner = inst.getDescriptor("hudson.plugins.sonar.SonarRunnerInstallation")

    def installer = new SonarRunnerInstaller("3.0.3.778")
    def prop = new InstallSourceProperty([installer])
    def sinst = new SonarRunnerInstallation("sonar-scanner-tool", "", [prop])
    sonarRunner.setInstallations(sinst)

    sonarRunner.save()
    
    def addWebhook = new URL("${sonarHost}/api/settings/set").openConnection()
    def fieldValues = URLEncoder.encode('{"name":"Jenkins","url":"http://jenkins/sonarqube-webhook/"}', 'UTF-8')
    def postBody = "key=sonar.webhooks.global&fieldValues=${fieldValues}"
    addWebhook.setRequestMethod('POST')
    addWebhook.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    addWebhook.setRequestProperty("Authorization", "Basic ${authString}")
    addWebhook.setDoOutput(true)
    addWebhook.setDoInput(true)
    addWebhook.getOutputStream().write(postBody.getBytes())
    rc = addWebhook.getResponseCode()
    if (rc == 204) {
        LOG.log(Level.INFO, 'SonarQube Webhook successfully configured')
    } else {
        LOG.log(Level.WARNING, 'SonarQube Webhook configuration FAILED')
        LOG.log(Level.WARNING, addWebhook.getInputStream().text)
    }

    LOG.log(Level.INFO, 'SonarQube configuration complete')
} else {
    LOG.log(Level.INFO, "Request failed: ${rc}")
    LOG.log(Level.INFO, generateToken.getErrorStream().getText())
}
