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

    KogitoJobUtils.createQuarkusPlatformUpdateToolsJob(this, 'kogito')

    KogitoJobUtils.createMainQuarkusUpdateToolsJob(this,
        [ 'kogito-runtimes', 'kogito-examples', 'kogito-docs', 'kogito-images' ],
        [ 'radtriste', 'cristianonicolai' ]
    )
}

// Setup branch branch
createSetupBranchJob()

// Nightly
if (Utils.getStream(this) != '2.2.x') {
setupNightlyJob()
}

// Weekly
setupWeeklyJob()

// Release
setupReleaseArtifactsJob()
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
        }
    }
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

            stringParam('RELEASE_VERSION', '', 'Version to release as Major.minor.micro')
            stringParam('GIT_TAG_NAME', '', 'Git tag to create. i.e.: 10.0.0-rc1')

            booleanParam('SKIP_TESTS', false, 'Skip all tests')
        }
    }
}

void setupZipSourcesJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, 'zip-and-upload-sources', JobType.TOOLS, "${jenkins_path}/Jenkinsfile.zip.sources", 'Zip sources and upload them into artifactory')
    jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

            GIT_BRANCH_NAME: "${GIT_BRANCH}",
            GIT_AUTHOR: "${GIT_AUTHOR_NAME}",

            RELEASE_GPG_SIGN_KEY_CREDS_ID: Utils.getReleaseGpgSignKeyCredentialsId(this),
            RELEASE_GPG_SIGN_PASSPHRASE_CREDS_ID: Utils.getReleaseGpgSignPassphraseCredentialsId(this),
            RELEASE_SVN_REPOSITORY: Utils.getReleaseSvnStagingRepository(this),
            RELEASE_SVN_CREDS_ID: Utils.getReleaseSvnCredentialsId(this)
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
            stringParam('SOURCES_FILE_NAME_TEMPLATE', 'incubator-kie-${TARGET_VERSION}-sources', 'Filename pattern for the resulting sources archive. Can be parameterized by job parameters or env variables.')
        }
    }

}
