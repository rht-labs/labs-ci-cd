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

// Make a POST request to delete any existing admin tokens named "Jenkins"
LOG.log(Level.INFO, 'Delete existing SonarQube Jenkins token')
def revokeToken = new URL("${sonarHost}/api/user_tokens/revoke").openConnection()
def message = "name=Jenkins&login=admin"
revokeToken.setRequestMethod("POST")
revokeToken.setDoOutput(true)
revokeToken.setRequestProperty("Accept", "application/json")
def authString = "admin:admin".bytes.encodeBase64().toString()
revokeToken.setRequestProperty("Authorization", "Basic ${authString}")
revokeToken.getOutputStream().write(message.getBytes("UTF-8"))
def rc = revokeToken.getResponseCode()

// Create a new admin token named "Jenkins" and capture the value
LOG.log(Level.INFO, 'Generate new auth token for SonarQube/Jenkins integration')
def generateToken = new URL("${sonarHost}/api/user_tokens/generate").openConnection()
message = "name=${tokenName}&login=admin"
generateToken.setRequestMethod("POST")
generateToken.setDoOutput(true)
generateToken.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
generateToken.setRequestProperty("Authorization", "Basic ${authString}")
generateToken.getOutputStream().write(message.getBytes("UTF-8"))
rc = generateToken.getResponseCode()

def token = null

if (rc == 200) {
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

    LOG.log(Level.INFO, 'SonarQube configuration complete')
} else {
    LOG.log(Level.INFO, "Request failed: ${rc}")
    LOG.log(Level.INFO, generateToken.getErrorStream().getText())
}