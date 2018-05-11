# Open Innovation Labs CI/CD

The goal of this repository is to:

 1. Bootstrap Labs residencies will all the tools necessary for a comprehensive, OpenShift native CI/CD pipeline
 2. Serve as a reference implementation of the [openshift-applier](https://github.com/redhat-cop/openshift-applier/tree/master/roles/openshift-applier) model for Infrastructure-as-Code (IaC)

A few additional guiding principles:

* This repository is built to ensure all the individual components are integrated and can be deployed together. 
* It is likely that your residency will need to remove some components in this inventory and then add others. Thus, every residency team is encouraged to create a fork of this repo and modify to their needs. A few things to consider for your fork:
  - If possible, remove local templates and update your inventory to point to the templates in a tag of labs-ci-cd. This encourages reuse, as well as contributions back to the upstream effort.
  - If you build new, reusable features in your fork, contribute them back!
* Generally speaking, there should only be one tool per functional use case e.g. Sonatype Nexus is the artifact repository so we will not support JFrog Artifactory

## How it Works

There are multiple ansible inventory which identify all components to be built and deployed to an OpenShift cluster. These are broken down into three sections:
* bootstrap - `inventory/host_vars/projects-and-policies.yml` contains a collection of objects used to create project namespaces in OpenShift
* tools -  `inventory/host_vars/ci-cd-tooling.yml` contains the collection of Jenkins slaves, Jenkins S2I and other CI/CD tooling deployments such as SonarQube, Nexus and others.
* apps - `inventory/host_vars/app-build-deploy.yml` contains definitions for the Java reference app's build and deploy
The ansible layer is very thin. It simply provides a way to orchestrate the application of [OpenShift templates](https://docs.openshift.com/container-platform/latest/dev_guide/templates.html) across one or more [OpenShift projects](https://docs.openshift.com/container-platform/latest/architecture/core_concepts/projects_and_users.html#projects). All configuration for the applications should be defined by an OpenShift template and the corresponding parameters file.

## Getting Started With Docker

There are two ways to use Labs CI/CD. The preferred approach is to run the playbook using a docker container. This ensures consistency of the execution environment for all users. If you cannot use docker for some reason, read the Getting Started Without Docker section.

### Prerequisites

* [Docker CE](https://www.docker.com/community-edition#/download)
* [OpenShift CLI Tools](https://docs.openshift.com/container-platform/3.7/cli_reference/get_started_cli.html)
* Access to the OpenShift cluster

### Usage

1. Log on to an OpenShift server `oc login -u <user> https://<server>:<port>/`
    - Your user needs permissions to deploy ProjectRequest objects.
2. Clone this repository.
3. If `labs-ci-cd` doesn't yet exist on your OpenShift cluster, just run the default `run.sh` script:
    - `[labs-ci-cd]$ ./run.sh`
4. If `labs-ci-cd` already exists on your OpenShift cluster and you want to create a new instance of `labs-ci-cd` with its own name, run the "unique projects" playbook. Since the command will run in the container, the paths need to be prefixed with `/tmp/src/`:
    - `[labs-ci-cd]$ ./run.sh ansible-playbook /tmp/src/unique-projects-playbook.yaml -i /tmp/src/inventory/ -e "project_name_postfix=<insert unique postfix here>"`
    - This playbook works (in part) by changing the contents of the files in `params`. The playbook is idempotent, so it will only change these files once, but you should expect changes.
    - This playbook is useful if you're developing labs-ci-cd and want to test your changes. With a unique project name, you can safely try out your changes in a test cluster that others are using.
    - Note that only numbers, lowercase letters, and dashes are allowed in project names.

After running the playbook, the pipeline should execute in Jenkins, build the spring boot app, deploy artifacts to nexus, deploy the container to the dev stage and then wait approval to deploy to the demo stage. See Common Issues

## Getting Started Without Docker

It's possible that you cannot run docker on your machine for some reason. No fear, this was the common way of using Labs CI/CD for a long time.

### Prerequisites 

* [Ansible](http://docs.ansible.com/ansible/latest/intro_installation.html) 2.5 or above. Until 2.5 is GA, that likely means you need the following command:
  - `pip install git+https://github.com/ansible/ansible.git@devel`
* [OpenShift CLI Tools](https://docs.openshift.com/container-platform/3.7/cli_reference/get_started_cli.html)
* Access to the OpenShift cluster
* libselinux-python (only needed on Fedora, RHEL, and CentOS)
  - Install by running `yum install libselinux-python`.

### Basic Usage

1. Log on to an OpenShift server `oc login -u <user> https://<server>:<port>/`
    - Your user needs permissions to deploy ProjectRequest objects.
2. Clone this repository.
3. Install the required [openshift-applier](https://github.com/redhat-cop/openshift-applier) dependency:
    - `[labs-ci-cd]$ ansible-galaxy install -r requirements.yml --roles-path=roles`
4. If `labs-*` projects do not yet exist on your OpenShift cluster, run the "bootstrap" inventory. This will apply all the `project-and-policies` content from `host_vars`:
  - `[labs-ci-cd]$ ansible-playbook apply.yml -i inventory/ -e target=bootstrap`
5. If `labs-ci-cd` tooling such as Jenkins or SonarQube do not yet exist on your OpenShift cluster, run the "tools" inventory. This will apply all the `ci-cd-tooling` content from `host_vars`:
  - `[labs-ci-cd]$ ansible-playbook apply.yml -i inventory/ -e target=tools`
6. To deploy the reference Java App, run the "apps" inventory. This will apply all the `app-build-deploy` content from `host_vars`:
  - `[labs-ci-cd]$ ansible-playbook apply.yml -i inventory/ -e target=apps`

#### Cusomised Install
If `labs-ci-cd` already exists on your OpenShift cluster and you want to create a new instance of `labs-ci-cd` with its own name eg `john-ci-cd`, run the "unique projects" playbook:
    - `[labs-ci-cd]$ ansible-playbook unique-projects-playbook.yaml -i inventory/ -e "project_name_postfix=<insert unique postfix here>"`
    - This playbook works (in part) by changing the contents of the files in `params`. The playbook is idempotent, so it will only change these files once, but you should expect changes.
    - This playbook is useful if you're developing labs-ci-cd and want to test your changes. With a unique project name, you can safely try out your changes in a test cluster that others are using.
    - Note that only numbers, lowercase letters, and dashes are allowed in project names.

After running the playbook, the pipeline should execute in Jenkins, build the spring boot app, deploy artifacts to nexus, deploy the container to the dev stage and then wait approval to deploy to the demo stage. See Common Issues

## Running a Subset of the Inventory

1. See [the docs](https://github.com/redhat-cop/openshift-applier/tree/master/roles/openshift-applier#filtering-content-based-on-tags) in the openshift-applier repo.
2. The only required tag to deploy objects within the inventory is **projects**, all other tags are *optional*
3. If using `./run.sh` and docker, here is an example that runs the tags that provision projects, ci, and jenkins objects:
```
./run.sh ansible-playbook /tmp/src/apply.yml -i /tmp/src/inventory/ -e target=tools -e "filter_tags=jenkins,ci,projects"
```

If not using docker/`./run.sh`, here is the same example:

```
ansible-playbook  apply.yml -i inventory/ -e target=tools -e="filter_tags=jenkins,ci,projects"
```

## Layout
- `inventory`: a standard [ansible inventory](http://docs.ansible.com/ansible/latest/intro_inventory.html).
  - the `host_vars` files are written according to [the convention defined by the openshift-applier role](https://github.com/redhat-cop/openshift-applier/tree/master/roles/openshift-applier#sourcing-openshift-object-definitions).
  -  the `hosts` file reflects the fact that the playbook will use the OpenShift CLI on your localhost to interact with the cluster
- `openshift-templates`: a set [OpenShift templates](https://docs.openshift.com/container-platform/3.6/dev_guide/templates.html) to be sourced from the inventory. OpenShift provides a lot of templates out of the box, so these are only to fill in gaps. If possible, reuse or update these templates before writing new ones.
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
