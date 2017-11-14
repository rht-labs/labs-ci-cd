# Open Innovation Labs CI/CD Bootstrapping
This branch has a reduced subset of Red Hat Open Innovation Labs CI/CD components. We're in the process of refactoring this approach - pardon the mess and lack of documentation.

## Overview - Current State
This is an ansible inventory which identifies a list of applications, including CI / CD components, to be deployed to an OpenShift cluster. The ansible layer is very thin. It simply provides a way to orchestrate the application of [OpenShift templates](https://docs.openshift.com/container-platform/3.6/dev_guide/templates.html) across one or more [OpenShift projects](https://docs.openshift.com/container-platform/3.6/architecture/core_concepts/projects_and_users.html#projects). All configuration for the applications should be defined by an OpenShift template and the corresponding parameters file. 

Currently, the following components are included in this inventory:

* Long lived CI/CD, Development and Demo projects 
* Jenkins, including
  * S2I build for Jenkins plugins and configuration
  * maven and npm slaves for Jenkins
* Sonatype Nexus
* Example Java application and pipeline

## Prerequisites
* [Ansible](http://docs.ansible.com/ansible/latest/intro_installation.html)
* [OpenShift CLI Tools](https://docs.openshift.com/container-platform/3.6/cli_reference/get_started_cli.html)
* Access to the OpenShift cluster 

## Usage 
1. Log on to an OpenShift server `oc login -u <user> https://<server>:<port>/`
    1. Your user needs permissions to deploy ProjectRequest objects
2. Clone this repository
3. Install the required [casl-ansible](https://github.com/redhat-cop/casl-ansible) dependency
    1. `[labs-ci-cd]$ ansible-galaxy install -r requirements.yml --roles-path=roles`
4. Run the ansible playbook provided by the casl-ansible
    1. `[labs-ci-cd]$ ansible-playbook roles/casl-ansible/playbooks/openshift-cluster-seed.yml -i inventory/  --connection local`

After running the playbook, the pipeline should execute in Jenkins, build the spring boot app, deploy artifacts to nexus, deploy the container to the dev stage and then wait approval to deploy to the demo stage. See Common Issues


## Running a Subset of the Inventory

See [the docs](https://github.com/redhat-cop/casl-ansible/tree/master/roles/openshift-applier#filtering-content-based-on-tags) in casl-ansible

## Layout
- `inventory`: a standard [ansible inventory](http://docs.ansible.com/ansible/latest/intro_inventory.html). 
  - the `group_vars` are written according to [the convention defined by the openshift-applier role](https://github.com/redhat-cop/casl-ansible/tree/master/roles/openshift-applier#sourcing-openshift-object-definitions).
  -  the `hosts` file reflects the fact that the playbook will use the OpenShift CLI on your localhost to interact with the cluster
- `templates`: a set [OpenShift templates](https://docs.openshift.com/container-platform/3.6/dev_guide/templates.html) to be sourced from the inventory. OpenShift provides a lot of templates out of the box, and [the Labs team curates a repository](https://github.com/rht-labs/labs-ci-cd/tree/master/templates) as well. These should be favored before writing custom/new templates to be kept here.
- `params`: a set of [parameter files](https://docs.openshift.com/container-platform/3.6/dev_guide/templates.html#templates-parameters) to be processed along with their respective OpenShift template. the convention here is to group files by their application.
- `projectrequests`: processing templates for `ProjectRequest` objects in OpenShift requires elevated privileges, so we process `ProjectRequests` without templates because we want normal users to be able to run these playbooks. this directory contains the object definitions.

## Common Issues

- S2I Build fails to push image to registry with `error: build error: Failed to push image: unauthorized: authentication required`
  - This appears to be an eventual consistency error. Wait a minute or two and then kick the build off again, it should work