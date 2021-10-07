package org.kie.jenkins.jobdsl.model

enum JobId {

    BUILD_AND_TEST,
    DEPLOY_ARTIFACTS,
    UPDATE_VERSION,
    POST_RELEASE

    String toId() {
        return this.name().toLowerCase().replaceAll('_', '-')
    }
}
