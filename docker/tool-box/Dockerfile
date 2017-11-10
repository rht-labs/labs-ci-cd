FROM fedora:latest

ENV PACKAGE_LIST git ansible tree vim
ENV OC_CLIENT_MIRROR https://mirror.openshift.com/pub/openshift-v3/clients/3.6.173.0.21/linux/oc.tar.gz
ENV HOME /home/tool-box

RUN dnf -y update && \
    dnf -y install $PACKAGE_LIST && \
    dnf clean all && \
    curl -o /tmp/oc.tar.gz $OC_CLIENT_MIRROR && \
    tar zxf /tmp/oc.tar.gz -C /usr/bin/  && \
    rm /tmp/oc.tar.gz && \
    mkdir -m 775 $HOME && \
    chmod 775 /etc/passwd
    
WORKDIR $HOME

ADD ./s2i /usr/libexec/s2i

USER 1001

CMD ["/usr/libexec/s2i/run"]
