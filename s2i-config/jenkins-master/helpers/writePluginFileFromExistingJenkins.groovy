#!/usr/bin/env groovy

// resources used:
// http://stackoverflow.com/questions/9815273/how-to-get-a-list-of-installed-jenkins-plugins-with-name-and-version-pair
// http://stackoverflow.com/questions/17236710/jenkins-rest-api-using-tree-to-reference-specific-item-in-json-array

String user = System.console().readLine 'Enter Jenkins user:  '
String password = System.console().readLine 'Enter Jenkins password:  '
String apiHost = System.console().readLine 'Enter Jenkins hostname (https assumed, only include hostname here):  '


def process = "curl -k -g https://${user}:${password}@${apiHost}/pluginManager/api/json?depth=1&tree=plugins[shortName,version]".execute()
def out = new StringBuffer()
def err = new StringBuffer()
process.consumeProcessOutput( out, err )
process.waitFor()
def json = out.toString()
if ( json == null || json == ""){
	throw new GroovyRuntimeException( "something went wrong, please verify your inputs" )
}
def result = new groovy.json.JsonSlurper().parseText( json )
println result
new File('plugins.txt').write buildPluginsString(result)



/*********************
 * Everything below here is a helper function
 *********************/

def buildPluginsString(def result){
	def sb = new StringBuilder()
	result.plugins.each{ plugin ->
		// check if the dependency was pulled from a private .m2
		// if true, this dependnecy should be added as a binary in the plugins directory
		if (!plugin.version.contains('private')){
			sb.append "${plugin.shortName}:${plugin.version}\n"
		}
	}
	return sb.toString()
}