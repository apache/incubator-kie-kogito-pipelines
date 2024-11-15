Building Apache KIE

There are two ways to build the KIE projects:

Option 1: Using the Automated Build Script

Run the provided build script (in case of windows you have to use WSL with bash):
- ./build.sh

Option 2: Manual Step-by-Step Build

If you prefer to build manually, follow these steps:

Prerequisites
- Temurin JDK 17.0.11
- Maven 3.9.6
- GraalVM for JDK 17
- Docker 25+
- Python 3.12
- Make 4.3+
- Node 20
- PNPM 9.3.0
- Go 1.21.9
- Helm 3.15.2

Install Python packages:
pip install cekit==4.11.0 docker==7.0.0 docker-squash==1.2.0 ruamel.yaml==0.18.5

Docker Configuration
If using an alternative container runtime:

For Colima:
export DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock

For Rancher:
export DOCKER_HOST=unix://$HOME/.rd/docker.sock

For Podman:
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock

Build Steps

1. Build Drools:
cd incubator-kie-drools
git init .
mvn clean install -DskipTests -Dfull -Donly.reproducible=true
cd ..

2. Build OptaPlanner:
cd incubator-kie-optaplanner
mvn clean install -DskipTests -Dfull -Donly.reproducible=true
cd ..

3. Build Kogito Runtimes:
cd incubator-kie-kogito-runtimes
mvn clean install -DskipTests -Dfull -Donly.reproducible=true
cd ..

4. Build Kogito Apps:
cd incubator-kie-kogito-apps
mvn clean install -DskipTests -Dfull -Donly.reproducible=true -Pjitexecutor-native
cd ..

5. Build Kogito Images:
cd incubator-kie-kogito-images
cekit --descriptor kogito-base-builder-image.yaml build docker --platform linux/amd64
make build-image KOGITO_APPS_TARGET_BRANCH=10.0.0-rc2 ignore_test=true image_name=kogito-data-index-ephemeral
make build-image KOGITO_APPS_TARGET_BRANCH=10.0.0-rc2 ignore_test=true image_name=kogito-data-index-postgresql
make build-image KOGITO_APPS_TARGET_BRANCH=10.0.0-rc2 ignore_test=true image_name=kogito-jit-runner
make build-image KOGITO_APPS_TARGET_BRANCH=10.0.0-rc2 ignore_test=true image_name=kogito-jobs-service-allinone
make build-image KOGITO_APPS_TARGET_BRANCH=10.0.0-rc2 ignore_test=true image_name=kogito-jobs-service-ephemeral
make build-image KOGITO_APPS_TARGET_BRANCH=10.0.0-rc2 ignore_test=true image_name=kogito-jobs-service-postgresql
cd ..

6. Build KIE Tools:
cd incubator-kie-tools
git init .
git config user.name "Builder"
git config user.email "builder@example.com"
git add . && git commit -m "Initial commit"
pnpm bootstrap
export KIE_TOOLS_BUILD__runTests=false
export KIE_TOOLS_BUILD__buildExamples=true
export KIE_TOOLS_BUILD__buildContainerImages=true
pnpm -r --workspace-concurrency=1 build:prod
cd ..

Note: The KIE Sandbox Quarkus Accelerator does not require building. The source itself is the artifact.
