#invoke npm in jenkinsfile: sh "scl enable rh-nodejs6 'npm run build'"
FROM openshift/jenkins-slave-nodejs-rhel7:latest
USER root

ADD https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm google-chrome-stable_current_x86_64.rpm
RUN yum -y install redhat-lsb libXScrnSaver xdg-utils wget
RUN yum -y localinstall google-chrome-stable_current_x86_64.rpm
ENV CHROME_BIN /bin/google-chrome

RUN yum remove -y rh-nodejs4; \
    yum repolist > /dev/null && \
    INSTALL_PKGS="rh-nodejs6 rh-nodejs6-npm rh-nodejs6-nodejs-nodemon nss_wrapper" && \
    yum install -y --setopt=tsflags=nodocs \
      --enablerepo=rhel-server-rhscl-7-rpms \
      --enablerepo=rhel-7-server-optional-rpms \
      --enablerepo=rhel-7-server-optional-rpms \
      $INSTALL_PKGS && \
    rpm -V $INSTALL_PKGS && \
    yum clean all -y && \
    rm -rf /var/cache/yum

USER 1001
