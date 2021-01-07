import org.kie.jenkins.jobdsl.model.Folder
import org.kie.jenkins.jobdsl.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoJobUtils
import org.kie.jenkins.jobdsl.Utils

JENKINSFILE_PATH = '.ci/jenkins'

////////////////////////////
// Test purpose
// TODO to remove
// Map getMultijobPRConfig(Folder jobFolder) {
//     return [
//         parallel: true,
//         buildchain: true,
//         jobs : [
//             [
//                 id: 'kogito-runtimes',
//                 primary: true,
//                 env : [
//                     // Sonarcloud analysis only on main branch
//                     // As we have only Community edition
//                     ENABLE_SONARCLOUD: Utils.isMainBranch(this),
//                 ]
//             ], [
//                 id: 'kogito-apps',
//                 dependsOn: 'kogito-runtimes',
//                 repository: 'kogito-apps',
//                 env : [
//                     ADDITIONAL_TIMEOUT: jobFolder.isNative() || jobFolder.isMandrel() ? '360' : '210',
//                 ]
//             ], [
//                 id: 'kogito-examples',
//                 dependsOn: 'kogito-runtimes',
//                 repository: 'kogito-examples'
//             ]
//         ],
//     ]
// }

// // PR checks
// KogitoJobUtils.createAllEnvsPerRepoPRJobs(this) { jobFolder -> getMultijobPRConfig(jobFolder) }

// // Nightly jobs
// KogitoJobUtils.createAllJobsForMavenArtifactsRepository(this)

// KogitoJobUtils.applyInAllCloudNightlyFolders(this) { jobFolder ->
//     setupBuildAndTestJob(jobFolder)
// }
// KogitoJobUtils.createAllEnvsCloudPromoteImagesJobs(this)

// void setupBuildAndTestJob(Folder jobFolder) {
//     println 'hello'
// }
//////////////////////////


// PRs
setupKogitoRuntimesBDDPrJob()

// Tools
setupCreateIssueToolsJob()
setupCleanOldNamespacesToolsJob()
setupCleanOldNightlyImagesToolsJob()
KogitoJobUtils.createMainQuarkusUpdateToolsJob(this, 'Kogito Pipelines', [ 'drools', 'kogito-runtimes', 'kogito-examples', 'kogito-docs' ])

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
        def jobParams = KogitoJobUtils.getBasicJobParams(this, 'kogito-nightly.artifacts', jobFolder, "${JENKINSFILE_PATH}/Jenkinsfile.nightly.artifacts", 'Kogito Nightly Artifacts')
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
    KogitoJobUtils.applyInAllCloudNightlyFolders(this) { jobFolder ->
        def jobParams = KogitoJobUtils.getBasicJobParams(this, 'kogito-nightly.cloud', jobFolder, "${JENKINSFILE_PATH}/Jenkinsfile.nightly.cloud", 'Kogito Nightly Cloud')
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
    def jobParams = KogitoJobUtils.getBasicJobParams(this, 'kogito-release', Folder.RELEASE, "${JENKINSFILE_PATH}/Jenkinsfile.release.run", 'Kogito Release')
    jobParams.env.putAll([
        JENKINS_EMAIL_CREDS_ID: "${JENKINS_EMAIL_CREDS_ID}",

        GIT_BRANCH_NAME: "${GIT_BRANCH}",
        GIT_AUTHOR: "${GIT_AUTHOR_NAME}",

        IMAGE_REGISTRY_CREDENTIALS: "${CLOUD_IMAGE_REGISTRY_CREDENTIALS_RELEASE}",
        IMAGE_REGISTRY: "${CLOUD_IMAGE_REGISTRY}",
        IMAGE_NAMESPACE: "${CLOUD_IMAGE_NAMESPACE}",
        BRANCH_FOR_LATEST: "${CLOUD_IMAGE_LATEST_GIT_BRANCH}",

        DEFAULT_STAGING_REPOSITORY: "${MAVEN_NEXUS_STAGING_PROFILE_URL}",
        ARTIFACTS_REPOSITORY: "${MAVEN_ARTIFACTS_REPOSITORY}",
    ])
    KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
        parameters {
            stringParam('RESTORE_FROM_PREVIOUS_JOB', '', 'URL to a previous stopped release job which needs to be continued')

            stringParam('DROOLS_VERSION', '', 'Drools version to release as Major.minor.micro')
            stringParam('DROOLS_RELEASE_BRANCH', '', '(optional) Use to override the release branch name deduced from the DROOLS_VERSION')
            stringParam('KOGITO_VERSION', '', 'Kogito version to release as Major.minor.micro')
            stringParam('KOGITO_IMAGES_VERSION', '', '(optional) To be set if different from KOGITO_VERSION. Should be only a bug fix update from KOGITO_VERSION.')
            stringParam('KOGITO_OPERATOR_VERSION', '', '(optional) To be set if different from KOGITO_VERSION. Should be only a bug fix update from KOGITO_VERSION.')
            booleanParam('DEPLOY_AS_LATEST', false, 'Given project version is considered the latest version')

            stringParam('APPS_URI', '', 'Override default. Git uri to the kogito-apps repository to use for building images.')
            stringParam('APPS_REF', '', 'Override default. Git reference (branch/tag) to the kogito-apps repository to use for building images.')

            booleanParam('SKIP_TESTS', false, 'Skip all tests')

            stringParam('EXAMPLES_URI', '', 'Override default. Git uri to the kogito-examples repository to use for tests.')
            stringParam('EXAMPLES_REF', '', 'Override default. Git reference (branch/tag) to the kogito-examples repository to use for tests.')

            booleanParam('SKIP_ARTIFACTS_DEPLOY', false, 'To skip all artifacts (drools, runtimes, apps, examples) Test & Deployment. If skipped, please provide `ARTIFACTS_REPOSITORY`')
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
