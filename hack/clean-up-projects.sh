#!/bin/bash

oc get projects|grep labs|awk '{print $1}'|xargs oc delete projects
