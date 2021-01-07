import org.kie.jenkins.jobdsl.model.Folder
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoJobUtils
import org.kie.jenkins.jobdsl.Utils

JENKINSFILE_PATH = '.ci/jenkins'

////////////////////////////
// Test purpose
// TODO to remove
Map getMultijobPRConfig(Folder jobFolder) {
    return [
        parallel: true,
        buildchain: true,
        jobs : [
            [
                id: 'kogito-runtimes',
                primary: true,
                env : [
                    // Sonarcloud analysis only on main branch
                    // As we have only Community edition
                    ENABLE_SONARCLOUD: Utils.isMainBranch(this),
                ]
            ], [
                id: 'kogito-apps',
                dependsOn: 'kogito-runtimes',
                repository: 'kogito-apps',
                env : [
                    ADDITIONAL_TIMEOUT: jobFolder.isNative() ? '360' : '210',
                ]
            ], [
                id: 'kogito-examples',
                dependsOn: 'kogito-runtimes',
                repository: 'kogito-examples'
            ]
        ],
    ]
}
KogitoJobUtils.createAllEnvsPerRepoPRJobs(this, { jobFolder -> getMultijobPRConfig(jobFolder) })

KogitoJobUtils.createAllJobsForArtifactsRepository(this, ['kogito', 'drools'])

KogitoJobUtils.createAllEnvsBuildChainBuildAndTestJobs(this)
KogitoJobUtils.createAllEnvsDeployArtifactsJobs(this)
KogitoJobUtils.createAllEnvsUpdateVersionJobs(this, ['kogito', 'drools'])
KogitoJobUtils.createAllEnvsMavenUpdateVersionJobs(this, ['kogito', 'drools'])
////////////////////////////

// Nightly
setupAllArtifactsNightlyJobs()
setupAllCloudNightlyJobs()

// Release
setupReleaseJob()

/////////////////////////////////////////////////////////////////
// Methods
/////////////////////////////////////////////////////////////////

void setupKogitoRuntimesBDDPrJob() {
    def jobParams = KogitoJobUtils.getBasicJobParams(this, '0-runtimes-bdd-testing', Folder.PULLREQUEST_RUNTIMES_BDD, "${JENKINSFILE_PATH}/Jenkinsfile.pr.bdd-tests", 'Run on demand BDD tests from runtimes repository')
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

void setupUpdateQuarkusToolsJob() {
    def jobParams = KogitoJobUtils.getBasicJobParams(this, 'update-quarkus-all', Folder.TOOLS, "${JENKINSFILE_PATH}/Jenkinsfile.ecosystem.update-quarkus")
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        NOTIFICATION_JOB_NAME: 'Kogito Pipelines',
        PR_PREFIX_BRANCH: "${GENERATION_BRANCH}",

        BUILD_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",
        GIT_AUTHOR_CREDS_ID: "${GIT_AUTHOR_CREDENTIALS_ID}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            stringParam('NEW_VERSION', '', 'Which version to set ?')
        }
    }
}

void setupCleanOldNamespacesToolsJob() {
    def jobParams = KogitoJobUtils.getBasicJobParams(this, 'kogito-clean-old-namespaces', Folder.TOOLS, "${JENKINSFILE_PATH}/Jenkinsfile.tools.clean-old-namespaces")
    jobParams.triggers = [ cron : '@midnight' ]
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void setupCleanOldNightlyImagesToolsJob() {
    jobParams = KogitoJobUtils.getBasicJobParams(this, 'kogito-clean-old-nightly-images', Folder.TOOLS, "${JENKINSFILE_PATH}/Jenkinsfile.tools.clean-nightly-images")
    jobParams.triggers = [ cron : 'H 8 * * *' ]
    KogitoJobTemplate.createPipelineJob(this, jobParams)
}

void setupCreateIssueToolsJob() {
    jobParams = KogitoJobUtils.getBasicJobParams(this, 'kogito-create-issue', Folder.TOOLS, "${JENKINSFILE_PATH}/Jenkinsfile.tools.create-issue")
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

void setupAllArtifactsNightlyJobs() {
    KogitoJobUtils.applyInAllNightlyFolders(this) { jobFolder ->
        def jobParams = KogitoJobUtils.getBasicJobParams(this, 'kogito-nightly.artifacts', jobFolder, "${JENKINSFILE_PATH}/Jenkinsfile.nightly.artifacts", 'Kogito Nightly')
        jobParams.triggers = [cron : '@midnight']
        jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",
            GIT_BRANCH_NAME: "${GIT_BRANCH}",
        ])
        KogitoJobTemplate.createPipelineJob(this, jobParams).with {
            parameters {
                booleanParam('SKIP_TESTS', false, 'Skip all tests')
            }
        }
    }
}

void setupAllCloudNightlyJobs() {
    KogitoJobUtils.applyInAllNightlyFolders(this) { jobFolder ->
        def jobParams = KogitoJobUtils.getBasicJobParams(this, 'kogito-nightly.artifacts', jobFolder, "${JENKINSFILE_PATH}/Jenkinsfile.nightly.artifacts", 'Kogito Nightly')
        // jobParams.triggers = [cron : '@midnight'] // TODO should be triggered by Artifacts pipeline once finished
        jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

            GIT_BRANCH_NAME: "${GIT_BRANCH}",
            GIT_AUTHOR: "${GIT_AUTHOR_NAME}",

            IMAGE_REGISTRY_CREDENTIALS: "${CLOUD_IMAGE_REGISTRY_CREDENTIALS_NIGHTLY}",
            IMAGE_REGISTRY: "${CLOUD_IMAGE_REGISTRY}",
            IMAGE_NAMESPACE: "${CLOUD_IMAGE_NAMESPACE}",
            BRANCH_FOR_LATEST: "${CLOUD_IMAGE_LATEST_GIT_BRANCH}",
        ])
        KogitoJobTemplate.createPipelineJob(this, jobParams).with {
            parameters {
                booleanParam('SKIP_TESTS', false, 'Skip all tests')

                booleanParam('SKIP_IMAGES', false, 'To skip Images Deployment')
                booleanParam('SKIP_EXAMPLES_IMAGES', false, 'To skip Examples Images Deployment')
                booleanParam('SKIP_OPERATOR', false, 'To skip Operator Deployment')

                booleanParam('USE_TEMP_OPENSHIFT_REGISTRY', false, 'If enabled, use Openshift registry to push temporary images')
            }
        }
    }
}

void setupReleaseJob() {
    KogitoJobTemplate.createPipelineJob(this, KogitoJobUtils.getBasicJobParams(this, 'kogito-release', Folder.RELEASE, "${JENKINSFILE_PATH}/Jenkinsfile.release.run", 'Kogito Release'))?.with {
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
    }
}
