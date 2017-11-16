FROM fedora:latest

ENV PACKAGE_LIST git ansible tree vim unzip
ENV OC_CLIENT_MIRROR https://mirror.openshift.com/pub/openshift-v3/clients/3.6.173.0.21/linux/oc.tar.gz
ENV HOVERFLY_DOWNLOAD_URI https://github.com/SpectoLabs/hoverfly/releases/download/v0.14.2/hoverfly_bundle_linux_amd64.zip
ENV HOME /home/tool-box

RUN dnf -y update && \
    dnf -y install $PACKAGE_LIST && \
    dnf clean all && \
    curl -o /tmp/oc.tar.gz $OC_CLIENT_MIRROR && \
    tar zxf /tmp/oc.tar.gz -C /usr/bin/  && \
    rm /tmp/oc.tar.gz && \
    curl -o /tmp/hoverfly.zip -L $HOVERFLY_DOWNLOAD_URI && \
    unzip /tmp/hoverfly.zip -d /tmp/hoverfly && \
    mv /tmp/hoverfly/hoverctl /usr/bin && \
    rm -r /tmp/hoverfly /tmp/hoverfly.zip && \
    mkdir -m 775 $HOME && \
    chmod 775 /etc/passwd
    
WORKDIR $HOME

ADD ./s2i /usr/libexec/s2i

USER 1001

CMD ["/usr/libexec/s2i/run"]