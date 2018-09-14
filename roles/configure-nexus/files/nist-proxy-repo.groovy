import org.sonatype.nexus.blobstore.api.BlobStoreManager;

if ( !repository.repositoryManager.exists( 'nist-cve-proxy' ) ) {
    repository.createRawProxy( 'nist-cve-proxy', 'https://nvd.nist.gov/feeds/', BlobStoreManager.DEFAULT_BLOBSTORE_NAME);
}