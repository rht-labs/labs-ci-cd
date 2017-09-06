#!/usr/bin/env bash

ansible-galaxy install --roles-path . -r labs-ci-cd-requirements.yml

ansible-playbook --connection=local -i ci-cd-bootstrap/inventory casl-ansible/playbooks/openshift-cluster-seed.yml
