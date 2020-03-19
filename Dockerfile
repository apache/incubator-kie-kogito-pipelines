FROM registry.redhat.io/openshift4/ose-jenkins-agent-base

LABEL MAINTAINER bsig-cloud   <bsig-cloud@redhat.com>
LABEL Build kogito-cloud-operator using podman
ENV GOROOT=/usr/local/go \
    GO_VERSION=1.13  \
    GOPATH=/home/jenkins/go \
    GO111MODULE=on  \
    RELEASE_VERSION=v0.15.1 \
    PATH=/home/jenkins/go/bin:/usr/local/go/bin:/usr/bin/bzr-2.7.0/:$PATH
RUN  wget https://rpmfind.net/linux/fedora/linux/releases/30/Everything/x86_64/os/Packages/k/kernel-headers-5.0.9-300.fc30.x86_64.rpm && \
     yum install kernel-headers-5.0.9-300.fc30.x86_64.rpm -y

RUN   yum install make python-devel -y
RUN  curl -L -o /usr/go${GO_VERSION}.linux-amd64.tar.gz https://dl.google.com/go/go${GO_VERSION}.linux-amd64.tar.gz && \
     tar -C  /usr/local    -xvzf  /usr/go${GO_VERSION}.linux-amd64.tar.gz && \ 
    rm -rf   /usr/go${GO_VERSION}.linux-amd64.tar.gz
RUN  wget  https://launchpad.net/bzr/2.7/2.7.0/+download/bzr-2.7.0.tar.gz && \
     tar -C /usr/bin/  -xvzf bzr-2.7.0.tar.gz && \
     yum install gcc -y && \
     make -C /usr/bin/bzr-2.7.0/ &&  \
     rm -rf bzr-2.7.0.tar.gz 

RUN   wget https://www.mercurial-scm.org/release/centos7/mercurial-5.2.2-1.x86_64.rpm && \
      yum install mercurial-5.2.2-1.x86_64.rpm -y && \
      rm -rf mercurial-5.2.2-1.x86_64.rpm && \
      mkdir -p  /home/jenkins/go/src/github.com/kiegroup/kogito-cloud-operator/ 


RUN   curl -LO https://github.com/operator-framework/operator-sdk/releases/download/${RELEASE_VERSION}/operator-sdk-${RELEASE_VERSION}-x86_64-linux-gnu && \
      chmod +x operator-sdk-${RELEASE_VERSION}-x86_64-linux-gnu && mkdir -p /usr/local/bin/ && cp operator-sdk-${RELEASE_VERSION}-x86_64-linux-gnu /usr/local/bin/operator-sdk && rm operator-sdk-${RELEASE_VERSION}-x86_64-linux-gnu
RUN   echo "[centos_extras]" >> /etc/yum.repos.d/tar.repo && \
      echo "baseurl=http://mirror.centos.org/centos/7/extras/x86_64/">> /etc/yum.repos.d/tar.repo && \
      echo "gpgcheck=0" >> /etc/yum.repos.d/tar.repo && \
      echo "[criu]" >>  /etc/yum.repos.d/tar.repo && \
      echo "baseurl=https://copr-be.cloud.fedoraproject.org/results/adrian/criu-el7/epel-7-x86_64/" >> /etc/yum.repos.d/tar.repo && \
      echo "gpgcheck=0" >> /etc/yum.repos.d/tar.repo && \
      wget https://rpmfind.net/linux/fedora/linux/updates/30/Everything/x86_64/Packages/l/libseccomp-2.4.2-2.fc30.x86_64.rpm && \
      yum install libseccomp-2.4.2-2.fc30.x86_64.rpm  -y && \
      wget https://rpmfind.net/linux/fedora/linux/releases/30/Everything/x86_64/os/Packages/l/libnet-1.1.6-17.fc30.x86_64.rpm && \
       yum install libnet-1.1.6-17.fc30.x86_64.rpm   -y && \
      wget https://rpmfind.net/linux/fedora/linux/releases/30/Everything/x86_64/os/Packages/p/protobuf-c-1.3.1-2.fc30.x86_64.rpm  && \
      yum install protobuf-c-1.3.1-2.fc30.x86_64.rpm -y && \
      yum install  podman -y &&\
      ln -s /usr/bin/podman   /usr/bin/docker
USER 1001

