FROM fedora:latest

ENV PACKAGE_LIST git ansible tree vim unzip
ENV OC_CLIENT_MIRROR https://mirror.openshift.com/pub/openshift-v3/clients/3.7.23/linux/oc.tar.gz
ENV HOME /home/tool-box

RUN dnf -y update && \
    dnf -y install $PACKAGE_LIST && \
    dnf clean all && \
    curl $OC_CLIENT_MIRROR | tar -C /usr/local/bin/ -xzf - && \
    mkdir -m 775 $HOME && \
    chmod 775 /etc/passwd
    
WORKDIR $HOME

ADD ./s2i /usr/libexec/s2i

USER 1001

CMD ["/usr/libexec/s2i/run"]