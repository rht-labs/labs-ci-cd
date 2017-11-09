# Tool Box

This container exists to help folks that can't install ansible, git or other necessary tools. It is not to be used in any time of production setting and is not suppportable under an OpenShift subscription. It includes `oc` version 3.6.173.0.21

## Usage

`$ oc new-app https://github.com/rht-labs/labs-ci-cd --name=tool-box --context-dir=docker/tool-box`

wait for the build to finish and the container to deploy. you can now log into the terminal via the pod web console or

`$ oc get pods -l app=tool-box`

copy the NAME of the pod

`$ oc rsh <NAME>`

