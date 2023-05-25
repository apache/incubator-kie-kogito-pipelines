import org.kie.jenkins.jobdsl.KogitoJobTemplate
import org.kie.jenkins.jobdsl.model.JobType
import org.kie.jenkins.jobdsl.utils.JobParamsUtils
import org.kie.jenkins.jobdsl.utils.VersionUtils
import org.kie.jenkins.jobdsl.KogitoJobUtils
import org.kie.jenkins.jobdsl.Utils

JENKINSFILE_PATH = '.ci/jenkins'

// PRs
setupKogitoRuntimesBDDPrJob()

// Tools
setupUpdateJenkinsDependenciesJob()
setupCreateIssueToolsJob()
setupCleanOldNamespacesToolsJob()
setupCleanOldNightlyImagesToolsJob()
KogitoJobUtils.createQuarkusPlatformUpdateToolsJob(this, 'kogito')
KogitoJobUtils.createMainQuarkusUpdateToolsJob(this,
        [ 'kogito-runtimes', 'kogito-examples', 'kogito-docs', 'kogito-images' ],
        [ 'radtriste', 'cristianonicolai' ]
)
if (Utils.isMainBranch(this)) {
    setupBuildOperatorNode()
}

// Setup branch branch
createSetupBranchJob()

// Nightly
setupNightlyJob()
setupQuarkusPlatformJob(JobType.NIGHTLY)
setupQuarkus3NightlyJob()

KogitoJobUtils.createEnvironmentIntegrationBranchNightlyJob(this, 'quarkus-main')
KogitoJobUtils.createEnvironmentIntegrationBranchNightlyJob(this, 'quarkus-lts')
KogitoJobUtils.createEnvironmentIntegrationBranchNightlyJob(this, 'quarkus-branch')
KogitoJobUtils.createEnvironmentIntegrationBranchNightlyJob(this, 'native-lts')

// Release
setupReleaseArtifactsJob()
setupReleaseCloudJob()

// Drools quarkus 3 integration job
setupDroolsQuarkus3IntegrationJob('quarkus-3') 

/////////////////////////////////////////////////////////////////
// Methods
/////////////////////////////////////////////////////////////////

void setupDroolsQuarkus3IntegrationJob(String envName) {
    KogitoJobUtils.createNightlyBuildChainIntegrationJob(this, envName, Utils.getRepoName(this), true){ script ->
    def jobParams = JobParamsUtils.getDefaultJobParams(script, 'drools')
    jobParams.git.branch = VersionUtils.getProjectTargetBranch('drools', Utils.getGitBranch(this), Utils.getRepoName(this))
    jobParams.env.put('BUILD_ENVIRONMENT_OPTIONS_CURRENT', 'rewrite push_changes')
    jobParams.env.put('INTEGRATION_BRANCH_CURRENT', '9.x')
    JobParamsUtils.setupJobParamsDeployConfiguration(script, jobParams)
    jobParams.triggers = [] // Remove nightly trigger as it will be managed by the main pipeline
    return jobParams
}
}

void setupKogitoRuntimesBDDPrJob() {
    def jobParams = JobParamsUtils.getBasicJobParamsWithEnv(this, '0-runtimes-bdd-testing', JobType.PULL_REQUEST, 'kogito-bdd', "${JENKINSFILE_PATH}/Jenkinsfile.pr.bdd-tests", 'Run on demand BDD tests from runtimes repository')
    jobParams.git.project_url = "https://github.com/${GIT_AUTHOR_NAME}/kogito-runtimes/"
    jobParams.git.repo_url = "https://github.com/${GIT_AUTHOR_NAME}/${jobParams.git.repository}/"
    jobParams.pr = [
        run_only_for_branches: [ jobParams.git.branch ],
        checkout_branch : '${ghprbTargetBranch}',
        trigger_phrase : '.*[j|J]enkins,? run BDD[ tests]?.*',
        trigger_phrase_only: true,
        commitContext: 'BDD'
    ]
    jobParams.disable_concurrent = true
    KogitoJobTemplate.createPRJob(this, jobParams)
}

void setupCleanOldNamespacesToolsJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, 'kogito-clean-old-namespaces', JobType.TOOLS, "${JENKINSFILE_PATH}/Jenkinsfile.tools.clean-old-namespaces")
    jobParams.triggers = [ cron : '@midnight' ]
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void setupCleanOldNightlyImagesToolsJob() {
    jobParams = JobParamsUtils.getBasicJobParams(this, 'kogito-clean-old-nightly-images', JobType.TOOLS, "${JENKINSFILE_PATH}/Jenkinsfile.tools.clean-nightly-images")
    jobParams.triggers = [ cron : 'H 8 * * *' ]
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void setupCreateIssueToolsJob() {
    jobParams = JobParamsUtils.getBasicJobParams(this, 'kogito-create-issue', JobType.TOOLS, "${JENKINSFILE_PATH}/Jenkinsfile.tools.create-issue")
    jobParams.env.putAll([
        GITHUB_CLI_PATH: '/opt/tools/gh-cli/bin/gh',
    ])
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
    jobParams = JobParamsUtils.getBasicJobParams(this, 'jenkins-update-framework-deps', JobType.TOOLS, "${JENKINSFILE_PATH}/Jenkinsfile.tools.update-jenkins-dependencies", 'Nightly check of Jenkins dependencies from framework against current version of Jenkins')
    jobParams.triggers = [cron : '@midnight']
    jobParams.env.putAll([
        REPO_NAME: 'kogito-pipelines',
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        BUILD_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",
        AUTHOR_CREDS_ID: "${GIT_AUTHOR_CREDENTIALS_ID}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void createSetupBranchJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, '0-setup-branch', JobType.SETUP_BRANCH, "${JENKINSFILE_PATH}/Jenkinsfile.setup-branch", 'Kogito Setup Branch')
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",
        GIT_AUTHOR_CREDS_ID: "${GIT_AUTHOR_CREDENTIALS_ID}",

        IMAGE_REGISTRY_CREDENTIALS: "${CLOUD_IMAGE_REGISTRY_CREDENTIALS_NIGHTLY}",
        IMAGE_REGISTRY: "${CLOUD_IMAGE_REGISTRY}",
        IMAGE_NAMESPACE: "${CLOUD_IMAGE_NAMESPACE}",

        IS_MAIN_BRANCH: "${Utils.isMainBranch(this)}"
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            stringParam('KOGITO_VERSION', '', 'Kogito version')
            stringParam('DROOLS_VERSION', '', 'Drools version')
        }
    }
}

void setupNightlyJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, 'kogito-nightly', JobType.NIGHTLY, "${JENKINSFILE_PATH}/Jenkinsfile.nightly", 'Kogito Nightly')
    jobParams.triggers = [cron : '@midnight']
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",
        GIT_AUTHOR_CREDS_ID: "${GIT_AUTHOR_CREDENTIALS_ID}",

        IMAGE_REGISTRY_CREDENTIALS: "${CLOUD_IMAGE_REGISTRY_CREDENTIALS_NIGHTLY}",
        IMAGE_REGISTRY: "${CLOUD_IMAGE_REGISTRY}",
        IMAGE_NAMESPACE: "${CLOUD_IMAGE_NAMESPACE}",
        BRANCH_FOR_LATEST: "${CLOUD_IMAGE_LATEST_GIT_BRANCH}",

        MAVEN_SETTINGS_CONFIG_FILE_ID: "${MAVEN_SETTINGS_FILE_ID}",
        ARTIFACTS_REPOSITORY: "${MAVEN_ARTIFACTS_REPOSITORY}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            booleanParam('SKIP_TESTS', false, 'Skip all tests')

            booleanParam('SKIP_ARTIFACTS', false, 'To skip Artifacts (runtimes, apps, examples) Deployment')
            booleanParam('SKIP_IMAGES', false, 'To skip Images Deployment')
            booleanParam('SKIP_EXAMPLES_IMAGES', false, 'To skip Examples Images Deployment')
            booleanParam('SKIP_OPERATOR', false, 'To skip Operator Deployment')

            booleanParam('USE_TEMP_OPENSHIFT_REGISTRY', false, 'If enabled, use Openshift registry to push temporary images')
        }
    }
}

void setupQuarkus3NightlyJob() {
    def jobParams = JobParamsUtils.getBasicJobParamsWithEnv(this, '0-main', JobType.NIGHTLY, 'quarkus-3', "${JENKINSFILE_PATH}/Jenkinsfile.nightly.quarkus-3", 'Kogito Nightly')
    jobParams.triggers = [cron : '@midnight']
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            booleanParam('SKIP_TESTS', false, 'Skip all tests')
        }
    }
}

void setupQuarkusPlatformJob(JobType jobType) {
    def jobParams = JobParamsUtils.getBasicJobParams(this, 'quarkus-platform.deploy', jobType, "${JENKINSFILE_PATH}/Jenkinsfile.nightly.quarkus-platform", 'Kogito Quarkus platform job')
    JobParamsUtils.setupJobParamsDefaultMavenConfiguration(this, jobParams)
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
    def jobParams = JobParamsUtils.getBasicJobParams(this, '0-kogito-release', JobType.RELEASE, "${JENKINSFILE_PATH}/Jenkinsfile.release.artifacts", 'Kogito Artifacts Release')
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

            booleanParam('SKIP_CLOUD_RELEASE', false, 'To skip Cloud release. To use whenever you have specific parameters to set for the Cloud release')
        }
    }
}

void setupReleaseCloudJob() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, '0-kogito-release-cloud', JobType.RELEASE, "${JENKINSFILE_PATH}/Jenkinsfile.release.cloud", 'Kogito Cloud Release')
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",

        IMAGE_REGISTRY_CREDENTIALS: "${CLOUD_IMAGE_REGISTRY_CREDENTIALS_RELEASE}",
        IMAGE_REGISTRY: "${CLOUD_IMAGE_REGISTRY}",
        IMAGE_NAMESPACE: "${CLOUD_IMAGE_NAMESPACE}",
        BRANCH_FOR_LATEST: "${CLOUD_IMAGE_LATEST_GIT_BRANCH}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            stringParam('RESTORE_FROM_PREVIOUS_JOB', '', 'URL to a previous stopped release job which needs to be continued')

            stringParam('KOGITO_VERSION', '', 'Kogito version to release as Major.minor.micro')
            stringParam('KOGITO_IMAGES_VERSION', '', '(optional) To be set if different from KOGITO_VERSION. Should be only a bug fix update from KOGITO_VERSION.')
            stringParam('KOGITO_OPERATOR_VERSION', '', '(optional) To be set if different from KOGITO_VERSION. Should be only a bug fix update from KOGITO_VERSION.')
            stringParam('KOGITO_SERVERLESS_OPERATOR_VERSION', '', '(optional) To be set if different from KOGITO_VERSION. Should be only a bug fix update from KOGITO_VERSION.')
            booleanParam('DEPLOY_AS_LATEST', false, 'Given project version is considered the latest version')

            stringParam('APPS_URI', '', 'Override default. Git uri to the kogito-apps repository to use for building images.')
            stringParam('APPS_REF', '', 'Override default. Git reference (branch/tag) to the kogito-apps repository to use for building images.')

            booleanParam('SKIP_TESTS', false, 'Skip all tests')

            stringParam('EXAMPLES_URI', '', 'Override default. Git uri to the kogito-examples repository to use for tests.')
            stringParam('EXAMPLES_REF', '', 'Override default. Git reference (branch/tag) to the kogito-examples repository to use for tests.')

            booleanParam('SKIP_IMAGES_RELEASE', false, 'To skip Images Test & Deployment.')
            booleanParam('SKIP_EXAMPLES_IMAGES_RELEASE', false, 'To skip Examples Images Deployment')
            booleanParam('SKIP_OPERATOR_RELEASE', false, 'To skip Operator Test & Deployment.')
            booleanParam('SKIP_SERVERLESS_OPERATOR_RELEASE', false, 'To skip Serverless Operator Test & Deployment.')

            booleanParam('USE_TEMP_OPENSHIFT_REGISTRY', false, 'If enabled, use Openshift registry to push temporary images')
        }
    }
}

void setupBuildOperatorNode() {
    def jobParams = JobParamsUtils.getBasicJobParams(this, 'build-operator-node', JobType.TOOLS, "${JENKINSFILE_PATH}/Jenkinsfile.build-operator-node")
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

