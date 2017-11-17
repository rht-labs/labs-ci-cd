#!/usr/bin/env groovy

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