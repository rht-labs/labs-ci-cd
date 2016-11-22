// this is the exploded content for redhat-repos.json
// to do development on this script, load up the nexus example repo in your IDE for code completion and then copy here and into redhat-repo.json. https://github.com/sonatype/nexus-book-examples/tree/nexus-3.x/scripting/nexus-script-example 

// not the prettiest code I've written, but it's really the only way to config manage nexus

if ( !repository.repositoryManager.exists( 'jboss-public' ) ){
    repository.createMavenProxy('jboss-public','https://repository.jboss.org/nexus/content/groups/public/')
}

if ( !repository.repositoryManager.exists( 'redhat-ga' ) ){
    repository.createMavenProxy('redhat-ga','https://maven.repository.redhat.com/ga/')
}

if ( !repository.repositoryManager.exists( 'redhat-techpreview-all' ) ){
    repository.createMavenProxy('redhat-techpreview-all','https://maven.repository.redhat.com/techpreview/all/')
}

if ( !repository.repositoryManager.exists( 'redhat-group' ) ){
    repository.createMavenGroup('redhat-group', ['maven-central','jboss-public','redhat-techpreview-all','redhat-ga'])
}