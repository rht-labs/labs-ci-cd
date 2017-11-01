FROM registry.access.redhat.com/openshift3/jenkins-slave-base-rhel7:latest

USER root

#RUN rpm -Uvh https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
#RUN yum -y install ansible

RUN yum repolist > /dev/null && \
    INSTALL_PKGS="ansible" && \
    yum install -y --setopt=tsflags=nodocs \
      --enablerepo=rhel-7-server-extras-rpms \
      $INSTALL_PKGS && \
    rpm -V $INSTALL_PKGS && \
    yum clean all -y && \
    rm -rf /var/cache/yum
