import jenkins.model.Jenkins
import net.sf.json.JSONObject

import java.util.logging.Level
import java.util.logging.Logger

def slackBaseUrl = System.getenv('SLACK_BASE_URL')
final def LOG = Logger.getLogger("LABS")

if(slackBaseUrl != null) {

  LOG.log(Level.INFO,  'Configuring slack...' )
  
  def slackToken = System.getenv('SLACK_TOKEN')
  def slackRoom = System.getenv('SLACK_ROOM')
  def slackSendAs = ''
  def slackTeamDomain = ''
  def slackTokenCredentialId = System.getenv('SLACK_TOKEN_CREDENTIAL_ID')

  if(slackTokenCredentialId == null) {
    slackTokenCredentialId = ''
  }

  JSONObject formData = ['slack': ['tokenCredentialId': slackTokenCredentialId]] as JSONObject

  def slack = Jenkins.instance.getExtensionList(
    jenkins.plugins.slack.SlackNotifier.DescriptorImpl.class
  )[0]
  def params = [
    slackBaseUrl: slackBaseUrl,
    slackTeamDomain: slackTeamDomain,
    slackToken: slackToken,
    slackRoom: slackRoom,
    slackSendAs: slackSendAs
  ]
  def req = [
    getParameter: { name -> params[name] }
  ] as org.kohsuke.stapler.StaplerRequest
  slack.configure(req, formData)

  LOG.log(Level.INFO,  'Configured slack' )

  slack.save()
}