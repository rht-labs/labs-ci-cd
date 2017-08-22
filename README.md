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

1. Clone this repository to `<workdir>/labs-ci-cd`
2. Modify the inventory under `<workdir>/labs-ci-cd/ci-cd-bootstrap/inventory/group_vars/all.yml` as required for site/customer specific needs
    1. Additional customization can be done in `<workdir>/labs-ci-cd/ci-cd-bootstrap/namespaces`
    2. Additional customization can be done in `<workdir>/labs-ci-cd/ci-cd-bootstrap/params`
2. Clone the `casl-ansible` repository to `<workdir>/casl-ansible`
3. Change directory to `<workdir>/casl-ansible`
4. Check out the `oc-create` branch
5. Change directory to `<workdir>/casl-ansible/roles/oc-apply/tests`
6. Run the deck using `ansible-playbook -i <workdir>/labs-ci-cd/ci-cd-bootstrap/inventory test.yml`