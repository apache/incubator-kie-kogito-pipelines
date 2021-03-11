import org.kie.jenkins.jobdsl.templates.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoConstants

boolean isMainBranch() {
    return "${GIT_BRANCH}" == "${GIT_MAIN_BRANCH}"
}

def getDefaultJobParams() {
    return [
        job: [
            name: 'kogito-pipelines'
        ],
        git: [
            author: "${GIT_AUTHOR_NAME}",
            branch: "${GIT_BRANCH}",
            repository: 'kogito-pipelines',
            credentials: "${GIT_AUTHOR_CREDENTIALS_ID}",
            token_credentials: "${GIT_AUTHOR_TOKEN_CREDENTIALS_ID}"
        ]
    ]
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

def bddRuntimesPrFolder = "${KogitoConstants.KOGITO_DSL_PULLREQUEST_FOLDER}/${KogitoConstants.KOGITO_DSL_RUNTIMES_BDD_FOLDER}"
def nightlyBranchFolder = "${KogitoConstants.KOGITO_DSL_NIGHTLY_FOLDER}/${JOB_BRANCH_FOLDER}"
def releaseBranchFolder = "${KogitoConstants.KOGITO_DSL_RELEASE_FOLDER}/${JOB_BRANCH_FOLDER}"

if ("${GIT_BRANCH}" == "${GIT_MAIN_BRANCH}") {
    // PRs
    folder(KogitoConstants.KOGITO_DSL_PULLREQUEST_FOLDER)
    folder(bddRuntimesPrFolder)

    setupKogitoRuntimesBDDPrJob(bddRuntimesPrFolder)

    // Tools
    folder(KogitoConstants.KOGITO_DSL_TOOLS_FOLDER)

    setupCreateIssueToolsJob(KogitoConstants.KOGITO_DSL_TOOLS_FOLDER)
    setupCleanOldNamespacesToolsJob(KogitoConstants.KOGITO_DSL_TOOLS_FOLDER)
    setupCleanOldNightlyImagesToolsJob(KogitoConstants.KOGITO_DSL_TOOLS_FOLDER)
}

// Nightly
folder(KogitoConstants.KOGITO_DSL_NIGHTLY_FOLDER)
folder(nightlyBranchFolder)

setupNightlyJob(nightlyBranchFolder)

folder(KogitoConstants.KOGITO_DSL_RELEASE_FOLDER)
if (isMainBranch()) {
    // Release prepare and create branches jobs are not in a specific branch and should be generated only on main branch
    setupCreateReleaseBranchJob(KogitoConstants.KOGITO_DSL_RELEASE_FOLDER)
    setupPrepareReleaseJob(KogitoConstants.KOGITO_DSL_RELEASE_FOLDER)
} else {
    // No release job directly on main branch
    folder(releaseBranchFolder)

    setupReleaseJob(releaseBranchFolder)
}

/////////////////////////////////////////////////////////////////
// Methods
/////////////////////////////////////////////////////////////////

void setupKogitoRuntimesBDDPrJob(String jobFolder) {
    def jobParams = getJobParams('0-runtimes-bdd-testing', jobFolder, 'Jenkinsfile.pr.bdd-tests', 'Run on demand BDD tests from runtimes repository')
    jobParams.git.project_url = "https://github.com/${GIT_AUTHOR_NAME}/kogito-runtimes/"
    jobParams.git.repo_url = "https://github.com/${GIT_AUTHOR_NAME}/${jobParams.git.repository}/"
    jobParams.pr = [
        checkout_branch : '${ghprbTargetBranch}',
        trigger_phrase : '.*[j|J]enkins,? run BDD[ tests]?.*',
        trigger_phrase_only: true,
        commitContext: 'BDD'
    ]
    jobParams.disable_concurrent = true
    KogitoJobTemplate.createPRJob(this, jobParams)
}

void setupCleanOldNamespacesToolsJob(String jobFolder) {
    def jobParams = getJobParams('kogito-clean-old-namespaces', jobFolder, 'Jenkinsfile.tools.clean-old-namespaces')
    jobParams.triggers = [ cron : '@midnight' ]
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void setupCleanOldNightlyImagesToolsJob(String jobFolder) {
    jobParams = getJobParams('kogito-clean-old-nightly-images', jobFolder, 'Jenkinsfile.tools.clean-nightly-images')
    jobParams.triggers = [ cron : 'H 8 * * *' ]
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void setupCreateIssueToolsJob(String jobFolder) {
    jobParams = getJobParams('kogito-create-issue', jobFolder, 'Jenkinsfile.tools.create-issue')
    KogitoJobTemplate.createPipelineJob(this, jobParams).with {
        parameters {
            stringParam('AUTHOR', '', 'Git author')
            stringParam('REPOSITORY', '', 'Git repository')
            stringParam('BRANCH', 'master', 'Git branch')
            stringParam('ISSUE_TITLE', '', 'Title of the issue')
            textParam('ISSUE_BODY', '', 'Body of the issue')
        }
        environmentVariables {
            env('GITHUB_CLI_PATH', '/opt/tools/gh-cli/bin/gh')
        }
    }
}

void setupNightlyJob(String jobFolder) {
    def jobParams = getJobParams('kogito-nightly', jobFolder, 'Jenkinsfile.nightly', 'Kogito Nightly')
    jobParams.triggers = [cron : '@midnight']
    KogitoJobTemplate.createPipelineJob(this, jobParams).with {
        parameters {
            booleanParam('SKIP_TESTS', false, 'Skip all tests')

            booleanParam('SKIP_ARTIFACTS', false, 'To skip Artifacts (runtimes, examples, optaplanner) Deployment')
            booleanParam('SKIP_IMAGES', false, 'To skip Images Deployment')
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
        }
    }
}

void setupReleaseJob(String jobFolder) {
    KogitoJobTemplate.createPipelineJob(this, getJobParams('kogito-release', jobFolder, 'Jenkinsfile.release', 'Kogito Release')).with {
        parameters {
            stringParam('PROJECT_VERSION', '', 'Project version to release as Major.minor.micro')
            stringParam('KOGITO_IMAGES_VERSION', '', 'To be set if different from PROJECT_VERSION. Should be only a bug fix update from PROJECT_VERSION.')
            stringParam('KOGITO_OPERATOR_VERSION', '', 'To be set if different from PROJECT_VERSION. Should be only a bug fix update from PROJECT_VERSION.')
            stringParam('OPTAPLANNER_VERSION', '', 'Project version of OptaPlanner and its examples to release as Major.minor.micro')
            stringParam('OPTAPLANNER_RELEASE_BRANCH', '', 'Use to override the release branch name deduced from the OPTAPLANNER_VERSION')
            booleanParam('DEPLOY_AS_LATEST', false, 'Given project version is considered the latest version')

            booleanParam('SKIP_TESTS', false, 'Skip all tests')

            stringParam('EXAMPLES_URI', '', 'Override default. Git uri to the kogito-examples repository to use for tests.')
            stringParam('EXAMPLES_REF', '', 'Override default. Git reference (branch/tag) to the kogito-examples repository to use for tests.')

            booleanParam('SKIP_ARTIFACTS_DEPLOY', false, 'To skip all artifacts (runtimes, examples) Test & Deployment. If skipped, please provide `ARTIFACTS_REPOSITORY`')
            booleanParam('SKIP_ARTIFACTS_PROMOTE', false, 'To skip Runtimes Promote only. Automatically skipped if SKIP_ARTIFACTS_DEPLOY is true.')
            booleanParam('SKIP_IMAGES_DEPLOY', false, 'To skip Images Test & Deployment.')
            booleanParam('SKIP_IMAGES_PROMOTE', false, 'To skip Images Promote only. Automatically skipped if SKIP_IMAGES_DEPLOY is true')
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

void setupCreateReleaseBranchJob(String jobFolder) {
    KogitoJobTemplate.createPipelineJob(this, getJobParams('create-release-branches', jobFolder, 'Jenkinsfile.release.create-branches', 'Create release branches')).with {
        parameters {
            stringParam('REPOSITORIES', '', 'Comma-separated list of repository[:base branch] to update. The default base branch for every repository is the \'master\' branch.')
            stringParam('RELEASE_BRANCH', '', 'Release branch to create')
        }

        environmentVariables {
            env('GIT_AUTHOR', "${GIT_AUTHOR_NAME}")
            env('GIT_AUTHOR_CREDS_ID', "${GIT_AUTHOR_CREDENTIALS_ID}")
            env('DEFAULT_BASE_BRANCH', "${GIT_MAIN_BRANCH}")
        }
    }
}

void setupPrepareReleaseJob(String jobFolder) {
    KogitoJobTemplate.createPipelineJob(this, getJobParams('prepare-release-branch', jobFolder, 'Jenkinsfile.release.prepare', 'Prepare env for a release')).with {
        parameters {
            stringParam('KOGITO_VERSION', '', 'Project version to release as Major.minor.micro')
            stringParam('OPTAPLANNER_VERSION', '', 'Project version of OptaPlanner and its examples to release as Major.minor.micro')
            stringParam('OPTAPLANNER_RELEASE_BRANCH', '', 'Use to override the release branch name deduced from the OPTAPLANNER_VERSION')
        }

        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")

            env('PIPELINE_MAIN_BRANCH', "${GIT_MAIN_BRANCH}")

            env('GIT_AUTHOR', "${GIT_AUTHOR_NAME}")
            env('GIT_AUTHOR_CREDS_ID', "${GIT_AUTHOR_CREDENTIALS_ID}")
            env('GIT_BOT_AUTHOR_CREDS_ID', "${GIT_BOT_AUTHOR_CREDENTIALS_ID}")
        }
    }
}
