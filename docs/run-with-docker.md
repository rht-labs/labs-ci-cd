## Getting Started With Docker

There are two ways to use Labs CI/CD. The preferred approach is to run the playbook using a docker container. This ensures consistency of the execution environment for all users. If you have the prerequisites installed feel free to read the [Getting Started Without Docker section](#getting-started-without-docker).

### Prerequisites

* [Docker CE](https://www.docker.com/community-edition#/download)
* [OpenShift CLI Tools](https://docs.openshift.com/container-platform/latest/cli_reference/get_started_cli.html)
* Access to the OpenShift cluster (Your user needs permissions to deploy ProjectRequest objects)

### Usage

1. Log on to an OpenShift server `oc login -u <user> https://<server>:<port>/`
2. Clone this repository.
3. If `labs-ci-cd` doesn't yet exist on your OpenShift cluster, just run the default `run.sh` script:
```bash
./run.sh
```

## Customised Install

If `labs-ci-cd` already exists on your OpenShift cluster and you want to create a new instance of `labs-ci-cd` with its own name eg `john-ci-cd`, run the "unique projects" playbook. This playbook is useful if you're developing labs-ci-cd and want to test your changes. With a unique project name, you can safely try out your changes in a test cluster that others are using.

```bash
./run.sh ansible-playbook /tmp/src/unique-projects-playbook.yml \
    -i /tmp/src/inventory/ \
    -e ci_cd_namespace=another-ci-cd \
    -e dev_namespace=another-dev \
    -e test_namespace=another-test
```

## Running a Subset of the Inventory

In some cases you might not want to deploy all of the components in this repo; but only a subset such as Jenkins and the customisations to it.

1. See [the docs](https://github.com/redhat-cop/openshift-applier/tree/master/roles/openshift-applier#filtering-content-based-on-tags) in the openshift-applier repo.
2. The only required tag to deploy objects within the inventory is **projects**, all other tags are *optional*
3. If using `./run.sh` and docker, here is an example that runs the tags that provision projects, ci, and jenkins objects:
```bash
./run.sh ansible-playbook /tmp/src/apply.yml \
    -i /tmp/src/inventory/ \
    -e target=tools \
    -e "include_tags=jenkins,ci,projects"
```