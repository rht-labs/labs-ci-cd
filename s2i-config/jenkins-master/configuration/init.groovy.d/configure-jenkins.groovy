#!/usr/bin/env groovy
import jenkins.model.*
import com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView
import groovy.json.JsonSlurper
import hudson.plugins.sonar.SonarInstallation
import hudson.plugins.sonar.model.TriggersConfig

import java.util.logging.Level
import java.util.logging.Logger
import static hudson.plugins.sonar.utils.SQServerVersions.SQ_5_3_OR_HIGHER

final def LOG = Logger.getLogger("LABS")

LOG.log(Level.INFO,  'running configure-jenkins.groovy' )

try {
    // delete default OpenShift job
    Jenkins.instance.items.findAll {
        job -> job.name == 'OpenShift Sample'
    }.each {
        job -> job.delete()
    }
} catch (NullPointerException npe) {
   LOG.log(Level.INFO, 'Failed to delete sample job', npe)
}
// create a default build monitor view that includes all jobs
// https://wiki.jenkins-ci.org/display/JENKINS/Build+Monitor+Plugin
if ( Jenkins.instance.views.findAll{ view -> view instanceof com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView }.size == 0){
  view = new BuildMonitorView('Build Monitor','Build Monitor')
  view.setIncludeRegex('.*')
  Jenkins.instance.addView(view)
}

LOG.log(Level.INFO, 'Get SonarQube config')
def sonarConfig = Jenkins.instance.getDescriptor('hudson.plugins.sonar.SonarGlobalConfiguration')

if (System.getenv("OPENSHIFT_SONARQUBE")!=null) {
    def tokenName = 'Jenkins'

    def sonarHost = "http://sonarqube:9000"

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
            "Sonar", sonarHost, SQ_5_3_OR_HIGHER, token, "", "", "", "", "", new TriggersConfig(), "", "", ""
        )
        sonarConfig.setInstallations(sonarInst)
        sonarConfig.setBuildWrapperEnabled(true)
        sonarConfig.save()
        LOG.log(Level.INFO, 'SonarQube plugin configuration saved')
    } else {
        LOG.log(Level.INFO, "Request failed: ${rc}")
        LOG.log(Level.INFO, generateToken.getErrorStream().getText())
    }
}

// support custom CSS for htmlreports
// https://stackoverflow.com/questions/35783964/jenkins-html-publisher-plugin-no-css-is-displayed-when-report-is-viewed-in-j
System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")

// This is a helper to delete views in the Jenkins script console if needed
// Jenkins.instance.views.findAll{ view -> view instanceof com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView }.each{ view -> Jenkins.instance.deleteView( view ) }