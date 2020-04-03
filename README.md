# Open Innovation Labs CI/CD

## 🏁 No Longer Being Maintained  🌇
This project is being depricated and will no longer receive updates or contributions. OpenShift has moved on to version 4.x and this project was a great enabler for kick starting development of applications on OpenShift 3.x. The tools of DevOps have evolved and grown towards a GitOps approach and so the evolution of Labs CI/CD has moved that way too.

For this teams new approach to tooling and automation checkout these two repositories:
 - 🐙 [The new home for labs ci cd and other tooling](https://github.com/rht-labs/labs-ci-cd)  🐙
 - ⚓️ [Our custom Helm Charts](https://github.com/rht-labs/helm-charts)  ⚓️

## What's in the box?

This project is an Ansible inventory for loading an OpenShift cluster with some frequently used projects, apps and tools on a Red Hat Open Innovation Lab residencies. Using the `openshift-applier`, cluster content is loaded from templates and param files in a repeatable, config-as-code way.

 Running this Ansible inventory will first create three project namespaces: `labs-ci-cd`, `labs-dev` and `labs-test`. Subsequently it will create a bunch of commonly used `ci-cd-tools` such as Jenkins, Nexus and Sonar. It will also create a collection of `jenkins-slaves` that can be used in builds such as `npm`, `maven` and `golang` to name a few. Apps can be added also by applying their `Jenkinsfile` as a buildConfig with an example (java-app) is included as a reference.

 The diagaram below shows the components that get created by running the playbook's `bootstrap` and `tools` inventories.
![whats-in-the-box.png](whats-in-the-box.png)

## How it Works
The layout of the project is like most standard `ansible-playbooks` with a simplified view of the key parts shown below:
```bash
.
├── site.yml
├── requirements.yml
├── inventory
│   ├── group_vars
│   │   └── all.yml
│   ├── host_vars
│   |   └── ...
│   └── hosts
├── params
│   └── jenkins-slaves
│       └── **
├── secrets
│   └── ...
```
 * `site.yml` is a playbook that sets up some variables and drives the `openshift-applier` role.
 * `requirements.yml` is a manifest which contains the Ansible modules needed to run the playbook 
 * `inventory/host_vars/*.yml` is the collection of objects we want to insert into the cluster written according to [the convention defined by the openshift-applier role](https://github.com/redhat-cop/openshift-applier/tree/master/roles/openshift-applier#sourcing-openshift-object-definitions).
 * `inventory/hosts` is where the `targets` are defined for grouping of the various inventories to be run eg `bootsrap` for creating projects and roles bindings
 * `params` is a set of [parameter files](https://docs.openshift.com/container-platform/;latest/dev_guide/templates.html#templates-parameters) to be processed along with their respective OpenShift template. The convention here is to group files by their application.

### Multiple inventories
The Ansible layer is very thin; it simply provides a way to orchestrate the application of [OpenShift templates](https://docs.openshift.com/container-platform/latest/dev_guide/templates.html) across one or more [OpenShift projects](https://docs.openshift.com/container-platform/latest/architecture/core_concepts/projects_and_users.html#projects). All configuration for the applications should be defined by an OpenShift template and the corresponding parameters file.

There are multiple Ansible inventories which divide the type of components to be built and deployed to an OpenShift cluster. These are broken down into three sections:
* `bootstrap` - Located in `inventory/host_vars/projects-and-policies.yml` contains a collection of objects used to create project namespaces and bind roles to groups for those namespace in OpenShift
* `tools` -  Located in `inventory/host_vars/ci-cd-tooling.yml` contains the collection of Jenkins slaves, Jenkins S2I and other CI/CD tooling deployments such as SonarQube, Nexus and others.
* `apps` - Located in `inventory/host_vars/app-build-deploy.yml` contains definitions for the Java reference app's build and deploy


## Getting Started

### Prerequisites 

* [Ansible](http://docs.ansible.com/ansible/latest/intro_installation.html) 2.5 or above.
* [OpenShift CLI Tools](https://docs.openshift.com/container-platform/latest/cli_reference/get_started_cli.html)
* Access to the OpenShift cluster (Your user needs permissions to deploy ProjectRequest objects)
* libselinux-python (only needed on Fedora, RHEL, and CentOS)
  - Install by running `yum install libselinux-python`.

### Inventory Usage
It should be noted that non-docker executions will utilize the inventory directory included in this repo by default. If you would like to specify a custom inventory for any of the below tasks, you can do so by adding `-i /path/to/my/inventory` to the command

### Basic Usage

1. Log on to an OpenShift server `oc login -u <user> https://<server>:<port>/`
2. Clone this repository.
3. Install the required [openshift-applier](https://github.com/redhat-cop/openshift-applier) dependency:
```bash
ansible-galaxy install -r requirements.yml --roles-path=roles
```
4. To deploy everything please run:
```bash
ansible-playbook site.yml
```

## Customised Install

If `labs-ci-cd` already exists on your OpenShift cluster and you want to create a new instance of `labs-ci-cd` with its own name eg `john-ci-cd`, run the "unique projects" playbook. This playbook is useful if you're developing labs-ci-cd and want to test your changes. With a unique project name, you can safely try out your changes in a test cluster that others are using.

```bash
ansible-playbook site.yml -e ci_cd_namespace=another-ci-cd -e dev_namespace=another-dev -e test_namespace=another-test
```

Or please look [here](inventory/group_vars/all.yml) for other variables you can change.

Note:
  - Only numbers, lowercase letters, and dashes are allowed in project names.

After running the playbook, the pipeline should execute in Jenkins, build the spring boot app, deploy artifacts to nexus, deploy the container to the dev stage and then wait approval to deploy to the demo stage. See Common Issues

## Persistent vs Ephemeral Jenkins

`labs-ci-cd` will default to deploying a persistent Jenkins, if you do not wish to use persistent jenkins please add on the extra variable `jenkins_persistence_type` and set it to `ephemeral` For Example:

```bash
ansible-playbook site.yml -e jenkins_persistence_type=ephemeral
```

## Running a Subset of the Inventory

In some cases you might not want to deploy all of the components in this repo; but only a subset such as Jenkins and the customisations to it.

1. See [the docs](https://github.com/redhat-cop/openshift-applier/tree/master/roles/openshift-applier#filtering-content-based-on-tags) in the openshift-applier repo.
2. The only required tag to deploy objects within the inventory is **projects**, all other tags are *optional*
3. Here is an example that runs the tags that provision projects, ci, and jenkins objects:
```bash
ansible-playbook site.yml \
    -e "include_tags=jenkins,ci,projects"
```

## Scope and Direction

The goal of this repository is to:

 1. Bootstrap Labs residencies will all the tools necessary for a comprehensive, OpenShift native CI/CD pipeline
 2. Serve as a reference implementation of the [openshift-applier](https://github.com/redhat-cop/openshift-applier/tree/master/roles/openshift-applier) model for Infrastructure-as-Code (IaC)

A few additional guiding principles:

* This repository is built to ensure all the individual components are integrated and can be deployed together.
* It is likely that your residency will need to remove some components in this inventory and then add others. Thus, every residency team is encouraged to create a fork of this repo and modify to their needs. A few things to consider for your fork:
  - Avoid using local Templates in this repo, Templates should always point to a *tag* of [redhat-cop/containers-quickstarts](https://github.com/redhat-cop/containers-quickstarts) or [rht-labs/openshift-templates](https://github.com/rht-labs/openshift-templates). This encourages reuse, as well as contributions back to the upstream effort.
  - If you build new, reusable features in your fork, contribute them back!
* Generally speaking, there should only be one tool per functional use case e.g. Sonatype Nexus is the artifact repository so we will not support JFrog Artifactory

## Contributing

1) Fork the repo and open PR's
2) Add all new components to the inventory with appropriate namespaces and tags
3) Extended the `Jenkinsfile` with steps to verify that your components built/deployed correctly
4) For now, it is your responsibility to run the CI job. Please contact an admin for the details to set the CI job up.
5) The `tests/slaves/Jenkinsfile` gets run as part of CI and will spin up a new Jenkins instance from this repositories code and validate each of the provided slaves can be accessed and contain their expected binary on the path.

## Common Issues

* Issues with valid nexus certs like seen [here](https://github.com/redhat-cop/infra-ansible/issues/342). You can set the ansible variable `nexus_validate_certs: false` as a work around.
* S2I Build fails to push image to registry with `error: build error: Failed to push image: unauthorized: authentication required`. See [this issue](https://github.com/openshift/origin/issues/4518)


## License
[ASL 2.0](LICENSE)
