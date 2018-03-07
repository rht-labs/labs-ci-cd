# jenkins-slave-gradle
Provides a docker image of the gradle runtime for use as a Jenkins slave.

## Build
`docker build -t jenkins-slave-gradle .`

## Run
For local running and experimentation run `docker run -i -t --rm jenkins-slave-gradle /bin/bash` and have a play once inside the container.

## Jenkins Running
Add a new Kubernetes Container template called `jenkins-slave-gradle` and specify this as the node when running builds. Set the version of Java you want to use and away you go!
```bash
export JAVA_HOME=/path/to/java/version
gradle clean build
```
