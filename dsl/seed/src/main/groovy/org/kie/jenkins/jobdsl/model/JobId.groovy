package org.kie.jenkins.jobdsl.model

enum JobId {

    BUILD_AND_TEST,
    DEPLOY_ARTIFACTS,
    PROMOTE_IMAGES

    String toId() {
        return this.name().toLowerCase().replaceAll('_', '-')
    }

}
