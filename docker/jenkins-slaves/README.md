# Jenkins Slaves for Labs CI/CD

These images have been move to the [Red Hat CoP Containers Quickstarts](https://github.com/redhat-cop/containers-quickstarts/tree/master/jenkins-slaves) repo.

## How to build the slaves 
1. When using a slave from Red Hat CoP Containers Quickstarts, create a params file for it in `params/jenkins-slaves/` with some basic information needed by the template and **pin** it to a version you want to use. For example, to create the `ansible` slave:
```bash
SOURCE_REPOSITORY_URL=https://github.com/redhat-cop/containers-quickstarts
SOURCE_CONTEXT_DIR=jenkins-slaves/jenkins-slave-ansible
BUILDER_IMAGE_NAME=registry.access.redhat.com/openshift3/jenkins-slave-base-rhel7:v3.10
NAME=jenkins-slave-ansible
SOURCE_REPOSITORY_REF=v1.3
```

2. Create a new Ansible object in the `inventory/host_vars/ci-cd-tooling.yml` file as follows, pointing to the `params/jenkins-slaves/` file  created in step 1. Continuing the `ansible`example:
```yaml
- object: ci-cd-builds
  content:
  - name: jenkins-slave-ansible
    template: "https://raw.githubusercontent.com/redhat-cop/containers-quickstarts/v1.1/jenkins-slaves/templates/jenkins-slave-generic-template.yml"
    params: "{{ playbook_dir }}/params/jenkins-slave-ansible/build"
    namespace: "{{ ci_cd_namespace }}"
    tags:
      - jenkins-slaves
      - ansible-slave
```