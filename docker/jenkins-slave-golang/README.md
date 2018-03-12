# jenkins-slave-golang
Provides a docker image of the golang runtime for use as a Jenkins slave.

## Build
`docker build -t jenkins-slave-golang .`

## Run
For local running and experimentation run `docker run -i -t jenkins-slave-golang /bin/bash` and have a play once inside the container.

## Jenkins Running
Add a new Kubernetes Container template called `jenkins-slave-golang` and specify this as the node when running builds. There are path issues with Jenkins permissions and Go when trying to run a build so easiest way to fix this is to setup the GOLANG path to be same as the WORKSPACE
```
export GOPATH=${WORKSPACE}
go get -v -t ./...
go build -v
# if there are Ginkgo tests then also run this!
$GOPATH/bin/ginkgo -r --cover -keepGoing
```
