Building Apache KIE

Complete Environment Requirements
- Temurin JDK 17
- Maven 3.9.11
- GraalVM CE 17
- Docker 26.1.4+
- Python 3
- Make
- Node 24.13.0+
- PNPM 10.29.2
- Go 1.24.13
- Helm 3.15.2

Docker Configuration
If using an alternative container runtime, set DOCKER_HOST accordingly:

For Colima:
export DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock

For Rancher:
export DOCKER_HOST=unix://$HOME/.rd/docker.sock

For Podman:
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock

It is recommended to clean the local ~/.m2 repository, because its content will be incorporated in some container images during the build.

Build Steps

1. Build Drools (requires: JDK, Maven):
cd incubator-kie-drools
git init .
mvn clean install -DskipTests -Dfull -Donly.reproducible=true
cd ..

2. Build OptaPlanner (requires: JDK, Maven):
cd incubator-kie-optaplanner
mvn clean install -DskipTests -Dfull -Donly.reproducible=true
cd ..

3. Build Kogito Runtimes (requires: JDK, Maven):
cd incubator-kie-kogito-runtimes
mvn clean install -DskipTests -Dfull -Donly.reproducible=true
cd ..

4. Build Kogito Apps (requires: JDK, Maven, GraalVM, Docker):
Note: If using Podman, add: -Djib.dockerClient.executable=$(which podman)
cd incubator-kie-kogito-apps
mvn clean install -DskipTests -Dfull -Donly.reproducible=true -Pjitexecutor-native
cd ..

5. Build KIE Tools (requires: JDK, Maven, Node, PNPM, Go, Python, Helm, Docker, Make):
cd incubator-kie-tools
git config user.name "Builder"
git config user.email "builder@example.com"
git init . && git add . && git commit -m "."
pnpm bootstrap && KIE_TOOLS_BUILD__runTests=false KIE_TOOLS_BUILD__buildExamples=true KIE_TOOLS_BUILD__buildContainerImages=true pnpm -r --workspace-concurrency=1 build:prod
cd ..
