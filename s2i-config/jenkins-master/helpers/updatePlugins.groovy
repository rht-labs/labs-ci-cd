#!/usr/bin/env groovy
/*
A script to update the plugins for Jenkins S2I to the most recent versions compatible with the defined version of
Jenkins core.

Workflow:
  1. Download the entire list of Jenkins plugins from `https://plugins.jenkins.io/api`
  2. Filter the list of plugins based on their required version of Jenkins
  3. Create a list of the most recent plugins and filter out duplicates
  4. Read the `plugins.txt` file and retrieve the latest plugin versions from the list
     a. There is an array of plugin names called `pinned` and these will NOT be updated
  5. Output the updated list of plugins in a format compatible for use as `plugins.txt`
 */


import groovy.json.JsonSlurper

import java.util.stream.Collectors

def jenkinsVersion = (2*1000000)+(46*1000)+3

final def PLUGINS_API_BASE_URL = 'https://plugins.jenkins.io/api/plugins'

def newPlugins = []

def parser = new JsonSlurper()

File inputList

if (args && args[0]) {  // Did the user provide an input plugin list?
    inputList = new File(args[0])
} else {
    println "Error! No input file provided"
}

if (!(inputList.exists() && inputList.canRead())) {
    inputList = null
    println "Specified input file `${inputList.absolutePath}` does not exist or is not readable."
}

def conn = new URL("${PLUGINS_API_BASE_URL}?pages=1&limit=5000").openConnection()
conn.addRequestProperty("Accept", "application/json")

def compatible = [:]

// Plugins listed here will NOT be upgraded.
def pinned = [
    'blueocean',
    'blueocean-pipeline-editor',
    'openshift-sync',
    'openshift-login',
    'openshift-pipeline'
]

try {
    if (conn.getResponseCode() == 200) {
        String jsonText = conn.getInputStream().text

        def pluginList = parser.parseText(jsonText).plugins

        pluginList
            .stream()
            .map({ p ->
                def (major, minor, micro) = p.requiredCore.tokenize( '.' )
                def value = (Integer.parseInt(major)*1000000)+(Integer.parseInt(minor)*1000)+Integer.parseInt(micro?:"0")
                p.put('versionInt', value)
                return p
            })
            .filter({ p -> p.versionInt <= jenkinsVersion })
            .each { p ->
                if (compatible[p.name] && compatible[p.name].versionInt < p.versionInt) {
                    println "${p.name} version ${p.versionInt} is newer than ${compatible[p.name].versionInt}"
                    compatible.replace(p.name, [name: p.name, versionInt: p.versionInt, version: p.version])
                } else if (compatible[p.name] == null) {
                    compatible.put(p.name, [name: p.name, versionInt: p.versionInt, version: p.version])
                }
            }
    } else {
        println "Got non-200 response"
    }
} catch (Exception e) {
    println "Error retrieving plugins list"
    e.printStackTrace()
}

if (inputList) {    // User DID pass an input file as an argument
    def pluginsText = inputList.readLines()
             .stream()
             .map({ line -> line.tokenize( ':' ) })
             .map({ name ->
                if (name[0] in pinned) {
                    return String.format("%s:%s", name[0], name[1])
                } else if (compatible[name[0]]) {
                    return String.format("%s:%s", name[0], compatible[name[0]].version)
                } else {
                    return String.format("%s:%s", name[0], name[1])
                }
             })
             .sorted()
             .collect(Collectors.joining("\n"))

    File output
    if (args.length>1 && args[1]) {  // Did the user provide an output file?
        output = new File(args[1])
        if (!(output.canWrite() || output.createNewFile())) {
            output = null
        }
    }
    if (output) {
        output.write(pluginsText)
    } else {
        println pluginsText
    }
} else {
    println "You MUST provide an absolute or relative path to an input file for this script to work."
    println "EX: ./updatePlugins.groovy /path/to/plugins.txt"
}