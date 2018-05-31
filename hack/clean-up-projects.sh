#!/bin/bash

if [[ -n $1 ]]; then
    echo "param provided to script, deleting projects postfixed with $1"
    oc delete project labs-ci-cd$1 labs-dev$1 labs-demo$1
else
    echo "no param provided to script, deleting default projects"
    oc delete project labs-ci-cd labs-dev labs-demo
fi
