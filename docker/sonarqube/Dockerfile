FROM registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift

ENV SONAR_VERSION=6.7.1 \
    SONARQUBE_HOME=/opt/sonarqube \
    # Database configuration
    # Defaults to using H2
    SONARQUBE_JDBC_USERNAME=sonar \
    SONARQUBE_JDBC_PASSWORD=sonar \
    SONARQUBE_JDBC_URL=

# Http port
EXPOSE 9000

RUN set -x \

    # pub   2048R/D26468DE 2015-05-25
    #       Key fingerprint = F118 2E81 C792 9289 21DB  CAB4 CFCA 4A29 D264 68DE
    # uid                  sonarsource_deployer (Sonarsource Deployer) <infra@sonarsource.com>
    # sub   2048R/06855C1D 2015-05-25
    && gpg --keyserver ha.pool.sks-keyservers.net --recv-keys F1182E81C792928921DBCAB4CFCA4A29D26468DE \
    && yum install -y unzip java-1.8.0-openjdk-devel \
    && cd /opt \
    && curl -s -o sonarqube.zip -fSL https://sonarsource.bintray.com/Distribution/sonarqube/sonarqube-$SONAR_VERSION.zip \
    && curl -s -o sonarqube.zip.asc -fSL https://sonarsource.bintray.com/Distribution/sonarqube/sonarqube-$SONAR_VERSION.zip.asc \
    && gpg --batch --verify sonarqube.zip.asc sonarqube.zip \
    && unzip sonarqube.zip \
    && mv sonarqube-$SONAR_VERSION sonarqube \
    && mv sonarqube/extensions sonarqube/extensions-init \
    && mv sonarqube/data sonarqube/data-init \
    && mkdir -p sonarqube/data-init/plugins \
    && mkdir -p sonarqube/data \
    && chown -R root:root sonarqube \
    && chmod 4775 sonarqube -R \
    && rm sonarqube.zip* \
    && rm -rf $SONARQUBE_HOME/bin/*

VOLUME "$SONARQUBE_HOME/data"

WORKDIR $SONARQUBE_HOME

ADD sonar.properties /opt/sonarqube/conf/sonar.properties
ADD run.sh /opt/sonarqube/bin/run.sh
CMD /opt/sonarqube/bin/run.sh
ADD plugins.sh /opt/sonarqube/bin/plugins.sh
RUN /opt/sonarqube/bin/plugins.sh pmd gitlab github ldap
RUN chown root:root /opt/sonarqube -R; \
    chmod 1775 /opt/sonarqube -R
