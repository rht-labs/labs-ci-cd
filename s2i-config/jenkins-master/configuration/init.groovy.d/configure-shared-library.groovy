import jenkins.model.Jenkins
import jenkins.plugins.git.GitSCMSource;
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;

import java.util.logging.Level
import java.util.logging.Logger

final def LOG = Logger.getLogger("LABS")

def gitRepo = System.getenv('SHARED_LIB_REPO')

if(gitRepo?.trim()) {

  LOG.log(Level.INFO,  'Configuring shared library (implicit)...' )

  def sharedLibrary = Jenkins.getInstance().getDescriptor("org.jenkinsci.plugins.workflow.libs.GlobalLibraries")

  def libraryName = System.getenv('SHARED_LIB_NAME') ?: "labs-shared-library"
  def gitRef = System.getenv('SHARED_LIB_REF') ?: "master"
  def secretId = System.getenv('SHARED_LIB_SECRET') ?: ""

  //append namespace to secret as it appears in Jenkins
  if(secretId) {
    secretId = System.getenv('OPENSHIFT_BUILD_NAMESPACE') + "-" + secretId
  }

  GitSCMSource source = new GitSCMSource( libraryName, gitRepo, secretId, "*", "", false);
  SCMSourceRetriever sourceRetriever = new SCMSourceRetriever(source);

  LibraryConfiguration pipeline = new LibraryConfiguration(libraryName, sourceRetriever)
  pipeline.setDefaultVersion(gitRef)
  pipeline.setImplicit(true)
  sharedLibrary.get().setLibraries([pipeline])

  sharedLibrary.save()

  LOG.log(Level.INFO,  'Configured shared library' )

} else {
  LOG.log(Level.INFO, 'Skipping shared library configuration')
}
