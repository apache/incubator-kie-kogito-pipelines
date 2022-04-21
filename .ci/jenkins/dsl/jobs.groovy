import org.kie.jenkins.jobdsl.templates.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.FolderUtils
import org.kie.jenkins.jobdsl.Utils

JENKINSFILE_PATH = '.ci/jenkins'

def getDefaultJobParams() {
    return KogitoJobTemplate.getDefaultJobParams(this, KogitoConstants.KOGITO_PIPELINES_REPOSITORY)
}

def getJobParams(String jobName, String jobFolder, String jenkinsfileName, String jobDescription = '') {
    def jobParams = getDefaultJobParams()
    jobParams.job.name = jobName
    jobParams.job.folder = jobFolder
    jobParams.jenkinsfile = jenkinsfileName
    if (jobDescription) {
        jobParams.job.description = jobDescription
    }
    return jobParams
}

if (Utils.isMainBranch(this)) {
    // PRs
    setupKogitoRuntimesBDDPrJob()

    // Tools
    setupCreateIssueToolsJob()
    setupCleanOldNamespacesToolsJob()
    setupCleanOldNightlyImagesToolsJob()
}

// Nightly
setupNightlyJob()

if (Utils.isMainBranch(this)) {
    // Release prepare is not in a specific branch and should be generated only on main branch
    setupPrepareReleaseJob()
} else {
    // No release job directly on main branch
    setupReleaseJob()
}

setupUpdateQuarkusToolsJob()

/////////////////////////////////////////////////////////////////
// Methods
/////////////////////////////////////////////////////////////////

void setupKogitoRuntimesBDDPrJob() {
    def jobParams = getJobParams('0-runtimes-bdd-testing', FolderUtils.getPullRequestRuntimesBDDFolder(this), "${JENKINSFILE_PATH}/Jenkinsfile.pr.bdd-tests", 'Run on demand BDD tests from runtimes repository')
    jobParams.git.project_url = "https://github.com/${GIT_AUTHOR_NAME}/kogito-runtimes/"
    jobParams.git.repo_url = "https://github.com/${GIT_AUTHOR_NAME}/${jobParams.git.repository}/"
    jobParams.pr = [
        run_only_for_branches: jobParams.git.branch,
        checkout_branch : '${ghprbTargetBranch}',
        trigger_phrase : '.*[j|J]enkins,? run BDD[ tests]?.*',
        trigger_phrase_only: true,
        commitContext: 'BDD'
    ]
    jobParams.disable_concurrent = true
    KogitoJobTemplate.createPRJob(this, jobParams)
}

void setupUpdateQuarkusToolsJob() {
    def jobParams = getJobParams('update-quarkus-all', FolderUtils.getToolsFolder(this), "${JENKINSFILE_PATH}/Jenkinsfile.tools.update-quarkus")
    KogitoJobTemplate.createPipelineJob(this, jobParams).with {
        parameters {
            stringParam('NEW_VERSION', '', 'Which version to set ?')
        }
        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")

            env('NOTIFICATION_JOB_NAME', "Kogito Pipelines")
            env('PR_PREFIX_BRANCH', "${GENERATION_BRANCH}")

            env('BUILD_BRANCH_NAME', "${GIT_BRANCH}")
            env('GIT_AUTHOR', "${GIT_AUTHOR_NAME}")
            env('GIT_AUTHOR_CREDS_ID', "${GIT_AUTHOR_CREDENTIALS_ID}")
        }
    }
}

void setupCleanOldNamespacesToolsJob() {
    def jobParams = getJobParams('kogito-clean-old-namespaces', FolderUtils.getToolsFolder(this), "${JENKINSFILE_PATH}/Jenkinsfile.tools.clean-old-namespaces")
    jobParams.triggers = [ cron : '@midnight' ]
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void setupCleanOldNightlyImagesToolsJob(String jobFolder) {
    jobParams = getJobParams('kogito-clean-old-nightly-images', FolderUtils.getToolsFolder(this), "${JENKINSFILE_PATH}/Jenkinsfile.tools.clean-nightly-images")
    jobParams.triggers = [ cron : 'H 8 * * *' ]
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void setupCreateIssueToolsJob(String jobFolder) {
    jobParams = getJobParams('kogito-create-issue', FolderUtils.getToolsFolder(this), "${JENKINSFILE_PATH}/Jenkinsfile.tools.create-issue")
    KogitoJobTemplate.createPipelineJob(this, jobParams).with {
        parameters {
            stringParam('AUTHOR', '', 'Git author')
            stringParam('REPOSITORY', '', 'Git repository')
            stringParam('BRANCH', 'main', 'Git branch')
            stringParam('ISSUE_TITLE', '', 'Title of the issue')
            textParam('ISSUE_BODY', '', 'Body of the issue')
        }
        environmentVariables {
            env('GITHUB_CLI_PATH', '/opt/tools/gh-cli/bin/gh')
        }
    }
}

void setupNightlyJob() {
    def jobParams = getJobParams('kogito-nightly', FolderUtils.getNightlyFolder(this), "${JENKINSFILE_PATH}/Jenkinsfile.nightly", 'Kogito Nightly')
    jobParams.triggers = [cron : '@midnight']
    KogitoJobTemplate.createPipelineJob(this, jobParams).with {
        parameters {
            booleanParam('SKIP_TESTS', false, 'Skip all tests')

            booleanParam('SKIP_ARTIFACTS', false, 'To skip Artifacts (runtimes, examples, optaplanner) Deployment')
            booleanParam('SKIP_IMAGES', false, 'To skip Images Deployment')
            booleanParam('SKIP_EXAMPLES_IMAGES', false, 'To skip Examples Images Deployment')
            booleanParam('SKIP_OPERATOR', false, 'To skip Operator Deployment')

            booleanParam('USE_TEMP_OPENSHIFT_REGISTRY', false, 'If enabled, use Openshift registry to push temporary images')
        }

        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")

            env('GIT_BRANCH_NAME', "${GIT_BRANCH}")
            env('GIT_AUTHOR', "${GIT_AUTHOR_NAME}")

            env('IMAGE_REGISTRY_CREDENTIALS', "${CLOUD_IMAGE_REGISTRY_CREDENTIALS_NIGHTLY}")
            env('IMAGE_REGISTRY', "${CLOUD_IMAGE_REGISTRY}")
            env('IMAGE_NAMESPACE', "${CLOUD_IMAGE_NAMESPACE}")
            env('BRANCH_FOR_LATEST', "${CLOUD_IMAGE_LATEST_GIT_BRANCH}")

            env('MAVEN_SETTINGS_CONFIG_FILE_ID', "${MAVEN_SETTINGS_FILE_ID}")
            env('ARTIFACTS_REPOSITORY', "${MAVEN_ARTIFACTS_REPOSITORY}")

            if (Utils.isLTSBranch(this)) {
                env('LTS_NATIVE_BUILDER_IMAGE', Utils.getLTSNativeBuilderImage(this))
            }
        }
    }
}

void setupReleaseJob() {
    KogitoJobTemplate.createPipelineJob(this, getJobParams('kogito-release', FolderUtils.getReleaseFolder(this), "${JENKINSFILE_PATH}/Jenkinsfile.release", 'Kogito Release')).with {
        parameters {
            stringParam('RESTORE_FROM_PREVIOUS_JOB', '', 'URL to a previous stopped release job which needs to be continued')

            stringParam('DROOLS_VERSION', '', 'Drools version to release as Major.minor.micro')
            stringParam('DROOLS_RELEASE_BRANCH', '', '(optional) Use to override the release branch name deduced from the DROOLS_VERSION')
            stringParam('OPTAPLANNER_VERSION', '', 'Project version of OptaPlanner and its examples to release as Major.minor.micro')
            stringParam('OPTAPLANNER_RELEASE_BRANCH', '', '(optional) Use to override the release branch name deduced from the OPTAPLANNER_VERSION')
            stringParam('KOGITO_VERSION', '', 'Kogito version to release as Major.minor.micro')
            stringParam('KOGITO_IMAGES_VERSION', '', '(optional) To be set if different from KOGITO_VERSION. Should be only a bug fix update from KOGITO_VERSION.')
            stringParam('KOGITO_OPERATOR_VERSION', '', '(optional) To be set if different from KOGITO_VERSION. Should be only a bug fix update from KOGITO_VERSION.')
            booleanParam('DEPLOY_AS_LATEST', false, 'Given project version is considered the latest version')

            booleanParam('SKIP_TESTS', false, 'Skip all tests')

            stringParam('EXAMPLES_URI', '', 'Override default. Git uri to the kogito-examples repository to use for tests.')
            stringParam('EXAMPLES_REF', '', 'Override default. Git reference (branch/tag) to the kogito-examples repository to use for tests.')

            booleanParam('SKIP_ARTIFACTS_DEPLOY', false, 'To skip all artifacts (runtimes, examples) Test & Deployment. If skipped, please provide `ARTIFACTS_REPOSITORY`')
            booleanParam('SKIP_ARTIFACTS_PROMOTE', false, 'To skip Runtimes Promote only. Automatically skipped if SKIP_ARTIFACTS_DEPLOY is true.')
            booleanParam('SKIP_IMAGES_DEPLOY', false, 'To skip Images Test & Deployment.')
            booleanParam('SKIP_IMAGES_PROMOTE', false, 'To skip Images Promote only. Automatically skipped if SKIP_IMAGES_DEPLOY is true')
            booleanParam('SKIP_EXAMPLES_IMAGES_DEPLOY', false, 'To skip Examples Images Deployment')
            booleanParam('SKIP_EXAMPLES_IMAGES_PROMOTE', false, 'To skip Examples Images Promote. Automatically skipped if SKIP_EXAMPLES_IMAGES_DEPLOY is true.')
            booleanParam('SKIP_OPERATOR_DEPLOY', false, 'To skip Operator Test & Deployment.')
            booleanParam('SKIP_OPERATOR_PROMOTE', false, 'To skip Operator Promote only. Automatically skipped if SKIP_OPERATOR_DEPLOY is true.')

            booleanParam('USE_TEMP_OPENSHIFT_REGISTRY', false, 'If enabled, use Openshift registry to push temporary images')
        }

        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")

            env('GIT_BRANCH_NAME', "${GIT_BRANCH}")
            env('GIT_AUTHOR', "${GIT_AUTHOR_NAME}")

            env('IMAGE_REGISTRY_CREDENTIALS', "${CLOUD_IMAGE_REGISTRY_CREDENTIALS_RELEASE}")
            env('IMAGE_REGISTRY', "${CLOUD_IMAGE_REGISTRY}")
            env('IMAGE_NAMESPACE', "${CLOUD_IMAGE_NAMESPACE}")
            env('BRANCH_FOR_LATEST', "${CLOUD_IMAGE_LATEST_GIT_BRANCH}")

            env('DEFAULT_STAGING_REPOSITORY', "${MAVEN_NEXUS_STAGING_PROFILE_URL}")
            env('ARTIFACTS_REPOSITORY', "${MAVEN_ARTIFACTS_REPOSITORY}")
        }
    }
}

void setupPrepareReleaseJob() {
    KogitoJobTemplate.createPipelineJob(this, getJobParams('prepare-release-branch', FolderUtils.getReleaseFolder(this), "${JENKINSFILE_PATH}/Jenkinsfile.release.prepare", 'Prepare env for a release')).with {
        parameters {
            stringParam('DROOLS_VERSION', '', 'Drools version to release as Major.minor.micro')
            stringParam('OPTAPLANNER_VERSION', '', 'OptaPlanner version of OptaPlanner and its examples to release as Major.minor.micro')
            stringParam('KOGITO_VERSION', '', 'Kogito version to release as Major.minor.micro')
        }

        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")

            env('PIPELINE_MAIN_BRANCH', "${GIT_MAIN_BRANCH}")
            env('DEFAULT_BASE_BRANCH', "${GIT_MAIN_BRANCH}")

            env('GIT_AUTHOR', "${GIT_AUTHOR_NAME}")
            env('GIT_AUTHOR_CREDS_ID', "${GIT_AUTHOR_CREDENTIALS_ID}")
            env('GIT_BOT_AUTHOR_CREDS_ID', "${GIT_BOT_AUTHOR_CREDENTIALS_ID}")
        }
    }
}
