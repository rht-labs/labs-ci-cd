FROM openshift/jenkins-slave-base-centos7:latest

ARG VERSION=1.5.1
ARG WEB_VERSION=0.5.12

RUN yum install -y wget ca-certificates

WORKDIR /arachni

RUN wget -qO- https://github.com/Arachni/arachni/releases/download/v${VERSION}/arachni-${VERSION}-${WEB_VERSION}-linux-x86_64.tar.gz | tar xvz -C /arachni --strip-components=1

EXPOSE 9292

COPY reporters ./system/gems/gems/arachni-${VERSION}/components/reporters

USER root

# NOTE - THIS IS THE STANDARD THING FOR OCP PERMISSION ISSUES
# ENV FIX_FILES /arachni
# RUN find $FIX_FILES -exec chgrp 0 {} \;  && \
#     find $FIX_FILES -exec chmod g+rw {} \; && \
#     find $FIX_FILES -type d -exec chmod g+x {} +

RUN chown -R root:root /arachni && \
    chmod -R 775 /arachni

USER 1001
