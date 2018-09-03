# Introduction

A semi automated hack to get updated plugin list. Here's how the helper works:

0. Make sure you have groovy installed, which is available as a package on most linux distros, or via [the download site](http://groovy-lang.org/download.html).
1. Login in the jenkins you want to replicate.
2. Post fix the jenkins url with `/pluginManager/api/json?depth=1&tree=plugins[shortName,version]` in your browser.
3. Copy the output to a new file called `out.json` in the `helpers` dir.
4. Run `./writePluginFileFromExistingJenkins.groovy` to construct a `plugins.txt` file to be used with the [OpenShift Jenkins S2I image](https://github.com/openshift/jenkins/blob/master/README.md#installing-using-s2i-build)
5. Delete `out.json`
6. Check in the new `plugins.txt`