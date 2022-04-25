// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

import org.kie.jenkins.jobdsl.FolderUtils
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.SeedJobUtils

// Create all folders
folder("${GENERATION_BRANCH}")
FolderUtils.getAllNeededFolders().each { folder("${GENERATION_BRANCH}/${it}") }

SeedJobUtils.createSeedJobTrigger(
    this,
    "${GENERATION_BRANCH}/${JOB_NAME}-trigger",
    KogitoConstants.KOGITO_PIPELINES_REPOSITORY,
    "${SEED_AUTHOR}",
    "${SEED_BRANCH}",
    [
        'dsl/seed/config/branch.yaml',
        'dsl/seed/gradle',
        'dsl/seed/jobs/seed_job_branch.groovy',
        'dsl/seed/jobs/Jenkinsfile.seed.branch',
        'dsl/seed/jobs/seed_job_repo.groovy',
        'dsl/seed/jobs/scripts',
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

        stringParam('CUSTOM_REPOSITORIES', "${CUSTOM_REPOSITORIES}", 'To generate only some custom repos... Comma list of `repo[:branch]`. Example: `kogito-pipelines:any_change`. If no branch is given, then `main` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')
        stringParam('CUSTOM_AUTHOR', "${CUSTOM_AUTHOR}", 'To generate only some custom repos... Define from from which author the custom repositories are checked out. If none given, then `SEED_AUTHOR` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')

        stringParam('SEED_AUTHOR', "${SEED_AUTHOR}", 'If different from the default')
        stringParam('SEED_BRANCH', "${SEED_BRANCH}", 'If different from the default')
    }

    environmentVariables {
        env('GENERATION_BRANCH', "${GENERATION_BRANCH}")
        env('MAIN_BRANCHES', "${MAIN_BRANCHES}")
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url("https://github.com/${SEED_AUTHOR}/kogito-pipelines.git")
                        credentials('kie-ci')
                    }
                    branch("${SEED_BRANCH}")
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath('dsl/seed/jobs/Jenkinsfile.seed.branch')
        }
    }

    properties {
        githubProjectUrl("https://github.com/${SEED_AUTHOR}/kogito-pipelines/")
    }
}
