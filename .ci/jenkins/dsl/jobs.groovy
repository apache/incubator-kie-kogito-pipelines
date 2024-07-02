/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.kie.jenkins.jobdsl.KogitoJobTemplate
import org.kie.jenkins.jobdsl.model.JobType
import org.kie.jenkins.jobdsl.utils.JobParamsUtils
import org.kie.jenkins.jobdsl.utils.VersionUtils
import org.kie.jenkins.jobdsl.KogitoJobUtils
import org.kie.jenkins.jobdsl.Utils

jenkins_path = '.ci/jenkins'

boolean isMainStream() {
    return Utils.getStream(this) == 'main'
}

// Tools
setupUpdateJenkinsDependenciesJob()
if (isMainStream()) {
    setupCreateIssueToolsJob()
    setupCleanOldNamespacesToolsJob()
    setupCleanOldNightlyImagesToolsJob()

    KogitoJobUtils.createQuarkusPlatformUpdateToolsJob(this, 'kogito')

    KogitoJobUtils.createMainQuarkusUpdateToolsJob(this,
        [ 'kogito-runtimes', 'kogito-examples', 'kogito-docs', 'kogito-images' ],
        [ 'radtriste', 'cristianonicolai' ]
    )
}

// Setup branch branch
createSetupBranchJob()
if (isMainStream()) {
    createSetupBranchCloudJob()
}

// Nightly
setupNightlyJob()
setupQuarkusPlatformJob(JobType.NIGHTLY)
if (isMainStream()) {
    setupNightlyCloudJob()
}

// Weekly
setupWeeklyJob()
if (isMainStream()) {
    setupWeeklyCloudJob()
}

// Release
setupReleaseArtifactsJob()
setupReleaseCloudJob()
if (isMainStream()) {
    setupZipSourcesJob()
}

Utils.isMainBranch(this) && KogitoJobTemplate.createBranchMultibranchPipelineJob(this, 'kogito-ci-build-image', "${jenkins_path}/Jenkinsfile.build-kogito-ci-image")

/////////////////////////////////////////////////////////////////
// Methods
/////////////////////////////////////////////////////////////////

void setupCleanOldNamespacesToolsJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, 'kogito-clean-old-namespaces', JobType.TOOLS, "${jenkins_path}/Jenkinsfile.tools.clean-old-namespaces")
    jobParams.triggers = [ cron : '@midnight' ]
    JobParamsUtils.setupJobParamsAgentDockerBuilderImageConfiguration(this, jobParams)
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void setupCleanOldNightlyImagesToolsJob() {
    jobParams = JobParamsUtils.getBasicJobParams(this, 'kogito-clean-old-nightly-images', JobType.TOOLS, "${jenkins_path}/Jenkinsfile.tools.clean-nightly-images")
    jobParams.triggers = [ cron : 'H 8 * * *' ]
    JobParamsUtils.setupJobParamsAgentDockerBuilderImageConfiguration(this, jobParams)
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void setupCreateIssueToolsJob() {
    jobParams = JobParamsUtils.getBasicJobParams(this, 'kogito-create-issue', JobType.TOOLS, "${jenkins_path}/Jenkinsfile.tools.create-issue")
    jobParams.env.putAll([
        GITHUB_CLI_PATH: '/opt/tools/gh-cli/bin/gh',
    ])
    JobParamsUtils.setupJobParamsAgentDockerBuilderImageConfiguration(this, jobParams)
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            stringParam('AUTHOR', '', 'Git author')
            stringParam('REPOSITORY', '', 'Git repository')
            stringParam('BRANCH', 'main', 'Git branch')
            stringParam('ISSUE_TITLE', '', 'Title of the issue')
            textParam('ISSUE_BODY', '', 'Body of the issue')
        }
    }
}

void setupUpdateJenkinsDependenciesJob() {
    jobParams = JobParamsUtils.getBasicJobParams(this, 'jenkins-update-framework-deps', JobType.TOOLS, "${jenkins_path}/Jenkinsfile.tools.update-jenkins-dependencies", 'Nightly check of Jenkins dependencies from framework against current version of Jenkins')
    jobParams.triggers = [cron : '@midnight']
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        BUILD_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",
        GIT_AUTHOR_CREDS_ID: "${GIT_AUTHOR_CREDENTIALS_ID}",
        GIT_AUTHOR_PUSH_CREDS_ID: "${GIT_AUTHOR_PUSH_CREDENTIALS_ID}",
    ])
    JobParamsUtils.setupJobParamsAgentDockerBuilderImageConfiguration(this, jobParams)
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void createSetupBranchJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, '0-setup-branch', JobType.SETUP_BRANCH, "${jenkins_path}/Jenkinsfile.setup-branch", 'Kogito Setup Branch for Artifacts')
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",
        GIT_AUTHOR_CREDS_ID: "${GIT_AUTHOR_CREDENTIALS_ID}",
        GIT_AUTHOR_PUSH_CREDS_ID: "${GIT_AUTHOR_PUSH_CREDENTIALS_ID}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            stringParam('KOGITO_VERSION', '', 'Kogito version')
            stringParam('DROOLS_VERSION', '', 'Drools version')
            booleanParam('DEPLOY', true, 'Should be deployed after setup ?')
            booleanParam('SKIP_CLOUD_SETUP_BRANCH', !isMainStream(), 'Skip Cloud setup branch call')
        }
    }
}

void createSetupBranchCloudJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, '0-setup-branch-cloud', JobType.SETUP_BRANCH, "${jenkins_path}/Jenkinsfile.setup-branch.cloud", 'Kogito Setup Branch for Cloud')
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",
        GIT_AUTHOR_CREDS_ID: "${GIT_AUTHOR_CREDENTIALS_ID}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            stringParam('KOGITO_VERSION', '', 'Kogito version')
            booleanParam('DEPLOY', true, 'Should be deployed after setup ?')
        }
    }
}

void setupNightlyJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, '0-kogito-nightly', JobType.NIGHTLY, "${jenkins_path}/Jenkinsfile.nightly", 'Kogito Nightly')
    jobParams.triggers = [cron : isMainStream () ? '@midnight' : 'H 4 * * *']
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",
        GIT_AUTHOR_CREDS_ID: "${GIT_AUTHOR_CREDENTIALS_ID}",
        GIT_AUTHOR_PUSH_CREDS_ID: "${GIT_AUTHOR_PUSH_CREDENTIALS_ID}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            booleanParam('SKIP_TESTS', false, 'Skip all tests')
            booleanParam('SKIP_CLOUD_NIGHTLY', !isMainStream(), 'Skip cloud nightly execution')
        }
    }
}

void setupWeeklyJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, '0-kogito-weekly', JobType.OTHER, "${jenkins_path}/Jenkinsfile.weekly", 'Kogito Weekly')
    jobParams.triggers = [cron : '0 5 * * 0']
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",
        GIT_AUTHOR_CREDS_ID: "${GIT_AUTHOR_CREDENTIALS_ID}",
        GIT_AUTHOR_PUSH_CREDS_ID: "${GIT_AUTHOR_PUSH_CREDENTIALS_ID}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            booleanParam('SKIP_TESTS', false, 'Skip all tests')
            booleanParam('SKIP_CLOUD_WEEKLY', !isMainStream(), 'Skip cloud weekly execution')
        }
    }
}

void setupWeeklyCloudJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, '0-kogito-weekly-cloud', JobType.OTHER, "${jenkins_path}/Jenkinsfile.weekly.cloud", 'Kogito Weekly')
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",
        GIT_AUTHOR_CREDS_ID: "${GIT_AUTHOR_CREDENTIALS_ID}",

        IMAGE_REGISTRY_CREDENTIALS: "${CLOUD_IMAGE_REGISTRY_CREDENTIALS}",
        IMAGE_REGISTRY: "${CLOUD_IMAGE_REGISTRY}",
        IMAGE_NAMESPACE: "${CLOUD_IMAGE_NAMESPACE}",
        BRANCH_FOR_LATEST: "${CLOUD_IMAGE_LATEST_GIT_BRANCH}",

        MAVEN_SETTINGS_CONFIG_FILE_ID: "${MAVEN_SETTINGS_FILE_ID}",
        ARTIFACTS_REPOSITORY: "${MAVEN_ARTIFACTS_REPOSITORY}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            booleanParam('SKIP_TESTS', false, 'Skip all tests')

            booleanParam('SKIP_IMAGES', false, 'To skip Images Deployment')
            booleanParam('SKIP_OPERATOR', false, 'To skip Operator Deployment')

            stringParam('GIT_CHECKOUT_DATETIME', '', 'Git checkout date and time - (Y-m-d H:i)')

            booleanParam('USE_TEMP_OPENSHIFT_REGISTRY', false, 'If enabled, use Openshift registry to push temporary images')
        }
    }
}

void setupNightlyCloudJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, '0-kogito-nightly-cloud', JobType.NIGHTLY, "${jenkins_path}/Jenkinsfile.nightly.cloud", 'Kogito Nightly')
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",
        GIT_AUTHOR_CREDS_ID: "${GIT_AUTHOR_CREDENTIALS_ID}",

        IMAGE_REGISTRY_CREDENTIALS: "${CLOUD_IMAGE_REGISTRY_CREDENTIALS}",
        IMAGE_REGISTRY: "${CLOUD_IMAGE_REGISTRY}",
        IMAGE_NAMESPACE: "${CLOUD_IMAGE_NAMESPACE}",
        BRANCH_FOR_LATEST: "${CLOUD_IMAGE_LATEST_GIT_BRANCH}",

        MAVEN_SETTINGS_CONFIG_FILE_ID: "${MAVEN_SETTINGS_FILE_ID}",
        ARTIFACTS_REPOSITORY: "${MAVEN_ARTIFACTS_REPOSITORY}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            booleanParam('SKIP_TESTS', false, 'Skip all tests')

            booleanParam('SKIP_IMAGES', false, 'To skip Images Deployment')
            booleanParam('SKIP_OPERATOR', false, 'To skip Operator Deployment')

            booleanParam('USE_TEMP_OPENSHIFT_REGISTRY', false, 'If enabled, use Openshift registry to push temporary images')
        }
    }
}

void setupQuarkusPlatformJob(JobType jobType) {
    def jobParams = JobParamsUtils.getBasicJobParams(this, 'quarkus-platform.deploy', jobType, "${jenkins_path}/Jenkinsfile.nightly.quarkus-platform", 'Kogito Quarkus platform job')
    JobParamsUtils.setupJobParamsAgentDockerBuilderImageConfiguration(this, jobParams)
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",

        QUARKUS_PLATFORM_NEXUS_URL: Utils.getMavenQuarkusPlatformRepositoryUrl(this),
        QUARKUS_PLATFORM_NEXUS_CREDS: Utils.getMavenQuarkusPlatformRepositoryCredentialsId(this),
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void setupReleaseArtifactsJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, '0-kogito-release', JobType.RELEASE, "${jenkins_path}/Jenkinsfile.release", 'Kogito Artifacts Release')
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",

        DEFAULT_STAGING_REPOSITORY: "${MAVEN_NEXUS_STAGING_PROFILE_URL}",
        ARTIFACTS_REPOSITORY: "${MAVEN_ARTIFACTS_REPOSITORY}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            stringParam('RESTORE_FROM_PREVIOUS_JOB', '', 'URL to a previous stopped release job which needs to be continued')

            stringParam('KOGITO_VERSION', '', 'Kogito version to release as Major.minor.micro')
            stringParam('DROOLS_VERSION', '', 'Drools version to set for the release')
            booleanParam('DEPLOY_AS_LATEST', false, 'Given project version is considered the latest version')

            booleanParam('SKIP_TESTS', false, 'Skip all tests')

            booleanParam('SKIP_CLOUD_RELEASE', !isMainStream(), 'To skip Cloud release. To use whenever you have specific parameters to set for the Cloud release')
        }
    }
}

void setupReleaseCloudJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, '0-kogito-release-cloud', JobType.RELEASE, "${jenkins_path}/Jenkinsfile.release.cloud", 'Kogito Cloud Release')
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",

        IMAGE_REGISTRY_CREDENTIALS: "${CLOUD_IMAGE_REGISTRY_CREDENTIALS}",
        IMAGE_REGISTRY: "${CLOUD_IMAGE_REGISTRY}",
        IMAGE_NAMESPACE: "${CLOUD_IMAGE_NAMESPACE}",
        BRANCH_FOR_LATEST: "${CLOUD_IMAGE_LATEST_GIT_BRANCH}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            stringParam('RESTORE_FROM_PREVIOUS_JOB', '', 'URL to a previous stopped release job which needs to be continued')

            stringParam('KOGITO_VERSION', '', 'Kogito version to release as Major.minor.micro')
            stringParam('KOGITO_IMAGES_VERSION', '', '(optional) To be set if different from KOGITO_VERSION. Should be only a bug fix update from KOGITO_VERSION.')
            stringParam('KOGITO_SERVERLESS_OPERATOR_VERSION', '', '(optional) To be set if different from KOGITO_VERSION. Should be only a bug fix update from KOGITO_VERSION.')
            booleanParam('DEPLOY_AS_LATEST', false, 'Given project version is considered the latest version')

            stringParam('APPS_URI', '', 'Override default. Git uri to the kogito-apps repository to use for building images.')
            stringParam('APPS_REF', '', 'Override default. Git reference (branch/tag) to the kogito-apps repository to use for building images.')

            booleanParam('SKIP_TESTS', false, 'Skip all tests')

            booleanParam('SKIP_IMAGES_RELEASE', false, 'To skip Images Test & Deployment.')
            booleanParam('SKIP_SERVERLESS_OPERATOR_RELEASE', false, 'To skip Serverless Operator Test & Deployment.')

            booleanParam('USE_TEMP_OPENSHIFT_REGISTRY', false, 'If enabled, use Openshift registry to push temporary images')
        }
    }
}

void setupZipSourcesJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, 'zip-and-upload-sources', JobType.TOOLS, "${jenkins_path}/Jenkinsfile.zip.sources", 'Zip sources and upload them into artifactory')
    jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

            GIT_BRANCH_NAME: "${GIT_BRANCH}",
            GIT_AUTHOR: "${GIT_AUTHOR_NAME}",

            IMAGE_REGISTRY_CREDENTIALS: "${CLOUD_IMAGE_REGISTRY_CREDENTIALS}",

            RELEASE_GPG_SIGN_KEY_CREDS_ID: Utils.getReleaseGpgSignKeyCredentialsId(this),
            RELEASE_GPG_SIGN_PASSPHRASE_CREDS_ID: Utils.getReleaseGpgSignPassphraseCredentialsId(this),
            RELEASE_SVN_REPOSITORY: Utils.getReleaseSvnCredentialsId(this),
            RELEASE_SVN_CREDS_ID: Utils.getReleaseSvnStagingRepository(this)
            ])

    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            textParam('SOURCES_REPOSITORIES',
                    '''incubator-kie-drools
incubator-kie-kogito-runtimes
incubator-kie-kogito-apps
incubator-kie-kogito-images
incubator-kie-optaplanner
incubator-kie-tools
incubator-kie-sandbox-quarkus-accelerator''',
                    'Configuration of sources repositories to pack. Format: "repository_name;branch(if-override-needed)" -eg. we want to override default sources branch for some repositories.')
            stringParam('TARGET_VERSION', '10.0.0', 'Version of the resulting artifact which will be mentioned in the artifact name')
            stringParam('SOURCES_DEFAULT_BRANCH', 'main', 'Branch to check out sources from. Can be overridden in REPOSITORIES definition')
            stringParam('SOURCES_FILE_NAME_TEMPLATE', 'incubator-kie-${TARGET_VERSION}-sources', 'Branch to check ouit sources from. Can be overridden in REPOSITORIES definition')
        }
    }

}