#!/usr/bin/env groovy

// resources used:
// http://stackoverflow.com/questions/9815273/how-to-get-a-list-of-installed-jenkins-plugins-with-name-and-version-pair
// http://stackoverflow.com/questions/17236710/jenkins-rest-api-using-tree-to-reference-specific-item-in-json-array

// manually login into Jenkins and postfix the base url with /pluginManager/api/json?depth=1&tree=plugins[shortName,version]
// take the output and save to out.json in this directory, then run the script

def json = new File('out.json').text
def result = new groovy.json.JsonSlurper().parseText( json )
println result
new File('../plugins.txt').write buildPluginsString(result)



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