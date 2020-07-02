#!/bin/bash

mkdir -p ~/rpmbuild/{RPMS,SRPMS,BUILD,SOURCES,SPECS,tmp}

cat <<EOF >~/.rpmmacros
%_topdir   %(echo $HOME)/rpmbuild
%_tmppath  %{_topdir}/tmp
EOF

cd ~/rpmbuild
wget ${KOGIT0_CLI_BINARY_URL}
tar -xf kogito-cli-${KOGITO_CLI_VERSION}-linux-amd64.tar.gz
mkdir kogito-cli-${KOGITO_CLI_VERSION}
mkdir -p kogito-cli-${KOGITO_CLI_VERSION}/usr/bin
install -m 755 kogito kogito-cli-${KOGITO_CLI_VERSION}/usr/bin
tar -zcvf kogito-cli-${KOGITO_CLI_VERSION}-tar.gz kogito-cli-${KOGITO_CLI_VERSION}
cp kogito-cli-${KOGITO_CLI_VERSION}-tar.gz SOURCES/

cat <<EOF > SPECS/kogito.spec
%define        __spec_install_post %{nil}
%define          debug_package %{nil}
%define        __os_install_post %{_dbpath}/brp-compress

Summary: Kogito Operator is a Kubernetes based operator for deployment of Kogito Runtimes from source. Additionally, to facilitate interactions with the operator we also offer a CLI (Command Line Interface) that can deploy Kogito applications for you.
Name: kogito-cli
Version: ${KOGITO_CLI_VERSION}
Release: 1
License: Apache License 2.0
Group: Development/Tools
SOURCE0 : kogito-cli-${KOGITO_CLI_VERSION}-tar.gz
URL: https://github.com/kiegroup/kogito-cloud-operator

BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root

%description
%{summary}
%prep
%setup -q
%build
%install
rm -rf %{buildroot}
mkdir -p  %{buildroot}
cp -a * %{buildroot}
%clean
rm -rf %{buildroot}
%files
%defattr(-,root,root,-)
%{_bindir}/*
%changelog
* ${KOGITO_CLI_RPM_CHANGELOG}
- ${KOGITO_CLI_RPM_CHANGELOG_DESCRIPTION}
EOF

rpmbuild -ba SPECS/kogito.spec
