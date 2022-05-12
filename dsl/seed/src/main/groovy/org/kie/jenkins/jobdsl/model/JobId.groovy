package org.kie.jenkins.jobdsl.model

enum JobId {

    BUILD_AND_TEST,
    DEPLOY_ARTIFACTS,
    DEPLOY_IMAGES

    String toId() {
        return this.name().toLowerCase().replaceAll('_', '-')
    }

}
