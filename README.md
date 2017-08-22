# Open Innovation Labs CI/CD Bootstrapping
A collection of Red Hat Open Innovation Labs CI/CD components

## Overview
This is an inventory, some templates, some build projects, and associated configuration which can be used
to bootstrap a CI/CD environment in OpenShift Container Platform. Currently, this will deploy:

* CI/CD Project
* Dev/Test/UAT Environments
* Jenkins (With extra plugins and Docker build slaves)
* SonarQube (And associated PostgreSQL database)
* SonaType Nexus
* Maven Build Pod
* NPM Build Pod
* Example Java application and pipeline

## Usage

1. Log on to an OpenShift server `oc login -u <user> https://<server>:<port>/`
    1. No special permissions should be required
2. Clone this repository to `<workdir>/labs-ci-cd`
3. Modify the inventory under `<workdir>/labs-ci-cd/ci-cd-bootstrap/inventory/group_vars/all.yml` as required for site/customer specific needs
    1. Additional customization can be done in `<workdir>/labs-ci-cd/ci-cd-bootstrap/namespaces`
    2. Additional customization can be done in `<workdir>/labs-ci-cd/ci-cd-bootstrap/params`
4. Clone the `github.com/redhat-cop/casl-ansible` repository to `<workdir>/casl-ansible`
5. Change directory to `<workdir>/casl-ansible`
6. Change directory to `<workdir>/casl-ansible/roles/oc-apply/tests`
7. Run the playbook using `ansible-playbook -i <workdir>/labs-ci-cd/ci-cd-bootstrap/inventory test.yml`
