# Open Innovation Labs CI/CD

The goal of this repository is to:

 1. provide all the tools necessary for a comprehensive CI/CD pipeline, built for and deployed to OpenShift
 2. serve as a reference implementation of the [openshift-applier](https://github.com/redhat-cop/casl-ansible/tree/master/roles/openshift-applier) model for Infrastructure-as-Code (IaC)

A few additional guiding principles:

* This repository is built as a monolith i.e. all the individual components are designed to be deployed together. When adding a new tool, care should be taken to integrate that tool with the other tools.
* To deploy a subset of components, you can:
  * Use the tags provided in the inventory to filter certain components. See [the docs](https://github.com/redhat-cop/casl-ansible/tree/master/roles/openshift-applier#filtering-content-based-on-tags) in openshift-applier
  * _coming soon_ - Use the provided tooling to generate a new inventory with a user defined subset of components
* Generally speaking, there should only be one tool per functional use case e.g. Sonatype Nexus is the artifact repository so we will not support JFrog Artifactory

## How it Works

There is ansible inventory which identifies all components to be deployed to an OpenShift cluster. The ansible layer is very thin. It simply provides a way to orchestrate the application of [OpenShift templates](https://docs.openshift.com/container-platform/3.6/dev_guide/templates.html) across one or more [OpenShift projects](https://docs.openshift.com/container-platform/3.6/architecture/core_concepts/projects_and_users.html#projects). All configuration for the applications should be defined by an OpenShift template and the corresponding parameters file.

Currently, the following components are included in this inventory:

* Long lived CI/CD, Development and Demo OpenShift projects
* Jenkins, including
  * S2I build for Jenkins plugins and configuration
  * maven and npm slaves for Jenkins
* Sonatype Nexus
* SonarQube
* Example Java application and pipeline

Currently, the following components have templates but are not yet integrated into the inventory:

* Gitlab
* Gogs

## Prerequisites

* [Ansible](http://docs.ansible.com/ansible/latest/intro_installation.html)
* [OpenShift CLI Tools](https://docs.openshift.com/container-platform/3.6/cli_reference/get_started_cli.html)
* Access to the OpenShift cluster
* libselinux-python (only needed on Fedora, RHEL, and CentOS)
  - Install by running `yum install libselinux-python`.

## Usage

1. Log on to an OpenShift server `oc login -u <user> https://<server>:<port>/`
    - Your user needs permissions to deploy ProjectRequest objects.
2. Clone this repository.
3. Install the required [casl-ansible](https://github.com/redhat-cop/casl-ansible) dependency:
    - `[labs-ci-cd]$ ansible-galaxy install -r requirements.yml --roles-path=roles`
4. If `labs-ci-cd` doesn't yet exist on your OpenShift cluster, run the "cluster seed" playbook:
  - `[labs-ci-cd]$ ansible-playbook roles/casl-ansible/playbooks/openshift-cluster-seed.yml -i inventory/`
5. If `labs-ci-cd` already exists on your OpenShift cluster and you want to create a new instance of `labs-ci-cd` with its own name, run the "unique projects" playbook:
    - `[labs-ci-cd]$ ansible-playbook unique-projects-playbook.yaml -i inventory/ -e "project_name_postfix=<insert unique postfix here>"`
    - This playbook is useful if you're developing labs-ci-cd and want to test your changes. With a unique project name, you can safely try out your changes in a test cluster that others are using.
    - Note that only numbers, lowercase letters, and dashes are allowed in project names.

After running the playbook, the pipeline should execute in Jenkins, build the spring boot app, deploy artifacts to nexus, deploy the container to the dev stage and then wait approval to deploy to the demo stage. See Common Issues


## Running a Subset of the Inventory

1. See [the docs](https://github.com/redhat-cop/casl-ansible/tree/master/roles/openshift-applier#filtering-content-based-on-tags) in casl-ansible
2. The only required tag to deploy objects within the inventory is **projects**, all other tags are *optional*
3. An example that provisions projects, ci, and jenkins objects:
```
ansible-playbook -i inventory/ roles/casl-ansible/playbooks/openshift-cluster-seed.yml --extra-vars="filter_tags=jenkins,ci,projects"
```

## Layout
- `inventory`: a standard [ansible inventory](http://docs.ansible.com/ansible/latest/intro_inventory.html).
  - the `group_vars` are written according to [the convention defined by the openshift-applier role](https://github.com/redhat-cop/casl-ansible/tree/master/roles/openshift-applier#sourcing-openshift-object-definitions).
  -  the `hosts` file reflects the fact that the playbook will use the OpenShift CLI on your localhost to interact with the cluster
- `openshift-templates`: a set [OpenShift templates](https://docs.openshift.com/container-platform/3.6/dev_guide/templates.html) to be sourced from the inventory. OpenShift provides a lot of templates out of the box, and [the Labs team curates a repository](https://github.com/rht-labs/labs-ci-cd/tree/master/templates) as well. These should be favored before writing custom/new templates to be kept here.
- `params`: a set of [parameter files](https://docs.openshift.com/container-platform/3.6/dev_guide/templates.html#templates-parameters) to be processed along with their respective OpenShift template. the convention here is to group files by their application.

## Common Issues

- S2I Build fails to push image to registry with `error: build error: Failed to push image: unauthorized: authentication required`
  - See [this issue](https://github.com/openshift/origin/issues/4518)

## Contributing

1) Fork the repo and open PR's
2) Add all new components to the inventory with appropriate namespaces and tags
3) Extended the `Jenkinsfile` with steps to verify that your components built/deployed correctly
4) For now, it is your responsibility to run the CI job. Please contact an admin for the details to set the CI job up.

## License
[ASL 2.0](LICENSE)
