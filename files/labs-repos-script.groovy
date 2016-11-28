import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.repository.maven.LayoutPolicy
import org.sonatype.nexus.repository.maven.VersionPolicy
import org.sonatype.nexus.repository.storage.WritePolicy

// this is the exploded content for labs-repos.json
// to do development on this script, load up the nexus example repo in your IDE for code completion and then copy here and into labs-repo.json. https://github.com/sonatype/nexus-book-examples/tree/nexus-3.x/scripting/nexus-script-example

// not the prettiest code I've written, but it's really the only way to config manage nexus

if ( !repository.repositoryManager.exists( 'labs-snapshots' ) ){
    repository.createMavenHosted( 'labs-snapshots', BlobStoreManager.DEFAULT_BLOBSTORE_NAME, true, VersionPolicy.SNAPSHOT, WritePolicy.ALLOW, LayoutPolicy.STRICT)
}

if ( !repository.repositoryManager.exists( 'labs-releases' ) ){
    repository.createMavenHosted( 'labs-releases', BlobStoreManager.DEFAULT_BLOBSTORE_NAME, true, VersionPolicy.RELEASE, WritePolicy.ALLOW_ONCE, LayoutPolicy.STRICT)
}

if ( !repository.repositoryManager.exists( 'jenkins-public' ) ){
    repository.createMavenProxy('jenkins-public','http://repo.jenkins-ci.org/public')
}

if ( !repository.repositoryManager.exists( 'labs-public' ) ){
    repository.createMavenGroup('labs-public', ['redhat-group','labs-releases','labs-snapshots', 'jenkins-public'])
}