FROM registry.access.redhat.com/openshift3/jenkins-slave-base-rhel7:latest

USER root

RUN yum repolist > /dev/null && \
    INSTALL_PKGS="ansible" && \
    yum install -y --setopt=tsflags=nodocs \
      --enablerepo=rhel-7-server-extras-rpms \
      $INSTALL_PKGS && \
    rpm -V $INSTALL_PKGS && \
    yum clean all -y && \
    rm -rf /var/cache/yum

ADD bin/start.sh /root/

# Set /root as starting directory
WORKDIR /root

# Helper script
ENTRYPOINT ["/root/start.sh"]
