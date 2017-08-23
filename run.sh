#!/usr/bin/env bash

ansible-galaxy install --role-path . -r labs-ci-cd-requirements.yml

ansible-playbook --connection=local -i ci-cd-bootstrap/inventory labs-ci-cd.yml
