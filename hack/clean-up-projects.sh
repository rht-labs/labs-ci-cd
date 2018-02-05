#!/bin/bash

if [[ -n $1 ]]; then
    echo "param provided to script, deleting pr projects"
    oc delete project labs-ci-cd-pr-$1 labs-dev-pr-$1 labs-demo-pr-$1
else
    echo "no param provided to script, deleting default projects"
    oc delete project labs-ci-cd labs-dev labs-demo
fi
