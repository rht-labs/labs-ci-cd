# jenkins-slave-npm
Provides a docker image of the nodejs v6 runtime with npm for use as a Jenkins slave. Also included is Chrome to enable headless testing as part of a CI build.

## Build
`docker build -t jenkins-slave-npm .`

## Run
For local running and experimentation run `docker run -i -t --rm jenkins-slave-npm /bin/bash` and have a play once inside the container.

## Jenkins Running
Add a new Kubernetes Container template called `jenkins-slave-npm` and specify this as the node when running builds. 
```
scl enable rh-nodejs6 'npm install'
scl enable rh-nodejs6 'npm run build'
```
