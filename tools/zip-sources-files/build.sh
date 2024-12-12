#!/bin/bash
# build.sh

# Check required tools
check_requirements() {
    local requirements=(
        "java:17.0.11"
        "mvn:3.9.6"
        "docker:25"
        "python3:3.12"
        "make:4.3"
        "node:20"
        "pnpm:9.3.0"
        "go:1.21.9"
        "helm:3.15.2"
    )

    echo "Checking requirements..."
    for req in "${requirements[@]}"; do
        IFS=':' read -r cmd version <<< "$req"
        if ! command -v "$cmd" &> /dev/null; then
            echo "ERROR: $cmd is not installed"
            exit 1
        fi
    done

    # Check Python packages
    pip3 install cekit==4.11.0 docker==7.0.0 docker-squash==1.2.0 ruamel.yaml==0.18.5
}

# Set Docker host based on container runtime
setup_docker_host() {
    if command -v colima &> /dev/null; then
        export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
    elif command -v rancher &> /dev/null; then
        export DOCKER_HOST="unix://$HOME/.rd/docker.sock"
    elif command -v podman &> /dev/null; then
        export DOCKER_HOST="unix://$XDG_RUNTIME_DIR/podman/podman.sock"
    fi
}

# Build components
build_components() {
    # Build Drools
    cd incubator-kie-drools || exit 1
    git init .
    mvn clean install -DskipTests -Dfull -Donly.reproducible=true
    cd ..

    # Build OptaPlanner
    cd incubator-kie-optaplanner || exit 1
    mvn clean install -DskipTests -Dfull -Donly.reproducible=true
    cd ..

    # Build Kogito Runtimes
    cd incubator-kie-kogito-runtimes || exit 1
    mvn clean install -DskipTests -Dfull -Donly.reproducible=true
    cd ..

    # Build Kogito Apps
    cd incubator-kie-kogito-apps || exit 1
    mvn clean install -DskipTests -Dfull -Donly.reproducible=true -Pjitexecutor-native
    cd ..

    # Build Kogito Images
    cd incubator-kie-kogito-images || exit 1
    cekit --descriptor kogito-base-builder-image.yaml build docker --platform linux/amd64
    
    local images=(
        "kogito-data-index-ephemeral"
        "kogito-data-index-postgresql"
        "kogito-jit-runner"
        "kogito-jobs-service-allinone"
        "kogito-jobs-service-ephemeral"
        "kogito-jobs-service-postgresql"
    )

    for image in "${images[@]}"; do
        make build-image KOGITO_APPS_TARGET_BRANCH=main ignore_test=true image_name="$image"
    done
    cd ..

    # Build KIE Tools
    cd incubator-kie-tools || exit 1
    git init .
    git config user.name "Builder"
    git config user.email "builder@example.com"
    git add . && git commit -m "Initial commit"
    pnpm bootstrap
    export KIE_TOOLS_BUILD__runTests=false
    export KIE_TOOLS_BUILD__buildExamples=true
    export KIE_TOOLS_BUILD__buildContainerImages=true
    pnpm -r --workspace-concurrency=1 build:prod
}

# Main execution
main() {
    check_requirements
    setup_docker_host
    build_components
}

main "$@"
