# Introduction

`writePluginFileFromExistingJenkins.groovy` is a simple Groovy script to help replicate the plugins of an existing Jenkins instance via OpenShift S2I. Execute the groovy file like any other shell script, although you'll need groovy installed, which is available as a package on most linux distros, or via [the download site](http://groovy-lang.org/download.html).

Here's how this helper works:

1. prompt the user to provide the Jenkins username, password, and hostname
2. `curl` the Jenkins REST API using info from step 1
3. construct a `plugins.txt` file to be used with the [OpenShift Jenkins S2I image](https://github.com/openshift/jenkins/blob/master/README.md#installing-using-s2i-build)