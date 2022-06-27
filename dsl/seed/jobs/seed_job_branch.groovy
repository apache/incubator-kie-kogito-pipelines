// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

import org.kie.jenkins.jobdsl.FolderUtils
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.SeedJobUtils
import org.kie.jenkins.jobdsl.Utils

// Create all folders
folder("${GENERATION_BRANCH}")
if (Utils.isOldFolderStructure(this)) {
    // For old branches
    FolderUtils.getAllNeededFolders().each { folder("${GENERATION_BRANCH}/${it}") }
}

SeedJobUtils.createSeedJobTrigger(
    this,
    "${GENERATION_BRANCH}/z-seed-trigger-job",
    KogitoConstants.KOGITO_PIPELINES_REPOSITORY,
    "${SEED_AUTHOR}",
    "${SEED_BRANCH}",
    [
        'dsl/config/branch.yaml',
        'dsl/seed/gradle',
        'dsl/seed/jenkinsfiles/scripts',
        'dsl/seed/jenkinsfiles/Jenkinsfile.seed.branch',
        'dsl/seed/jobs/seed_job_branch.groovy',
        'dsl/seed/src',
        'dsl/seed/build.gradle',
        'dsl/seed/gradle.properties',
    ],
    "${JOB_NAME}")

// Configuration of the seed and generated jobs is done via `dsl/seed/config.yaml`
pipelineJob("${GENERATION_BRANCH}/${JOB_NAME}") {
    description("This job creates all needed Jenkins jobs on branch ${GENERATION_BRANCH}. DO NOT USE FOR TESTING !!!! See https://github.com/kiegroup/kogito-pipelines/blob/main/docs/jenkins.md#test-specific-jobs")

    logRotator {
        numToKeep(5)
    }

    throttleConcurrentBuilds {
        maxTotal(1)
    }

    parameters {
        booleanParam('DEBUG', false, 'Enable Debug capability')

        booleanParam('SKIP_TESTS', false, 'Skip testing')
    }

    environmentVariables {
        env('GENERATION_BRANCH', "${GENERATION_BRANCH}")
        env('MAIN_BRANCHES', "${MAIN_BRANCHES}")

        env('SEED_CONFIG_FILE_GIT_REPOSITORY', "${SEED_CONFIG_FILE_GIT_REPOSITORY}")
        env('SEED_CONFIG_FILE_GIT_AUTHOR_NAME', "${SEED_CONFIG_FILE_GIT_AUTHOR_NAME}")
        env('SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID', "${SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID}")
        env('SEED_CONFIG_FILE_GIT_BRANCH', "${SEED_CONFIG_FILE_GIT_BRANCH}")
        env('SEED_CONFIG_FILE_PATH', "${SEED_CONFIG_FILE_PATH}")

        env('SEED_AUTHOR', "${SEED_AUTHOR}")
        env('SEED_BRANCH', "${SEED_BRANCH}")
        env('SEED_REPO', 'kogito-pipelines')
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url("https://github.com/${SEED_AUTHOR}/kogito-pipelines.git")
                        credentials(KogitoConstants.DEFAULT_CREDENTIALS_ID)
                    }
                    branch("${SEED_BRANCH}")
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath("${SEED_JENKINSFILE}")
        }
    }

    properties {
        githubProjectUrl("https://github.com/${SEED_AUTHOR}/kogito-pipelines/")
    }
}
