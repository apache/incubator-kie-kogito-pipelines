// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoJobUtils
import org.kie.jenkins.jobdsl.SeedJobUtils

String getSeedAuthor() {
    return SEED_AUTHOR ?: 'kiegroup'
}

String getSeedBranch() {
    return SEED_BRANCH ?: 'main'
}

SeedJobUtils.createSeedJobTrigger(
    this,
    '0-seed-job-trigger',
    KogitoConstants.KOGITO_PIPELINES_REPOSITORY,
    getSeedAuthor(),
    getSeedBranch(),
    [
        'dsl/seed/config/main.yaml',
        'dsl/seed/jobs/seed_job_main.groovy',
        'dsl/seed/jobs/Jenkinsfile.seed.main',
    ],
    './0-seed-job')

// Configuration of the seed and generated jobs is done via `dsl/seed/config.yaml`
pipelineJob('0-seed-job') {
    description('This job creates all needed Jenkins jobs. DO NOT USE FOR TESTING !!!! See https://github.com/kiegroup/kogito-pipelines/blob/main/docs/jenkins.md#test-specific-jobs')

    logRotator {
        numToKeep(5)
    }

    throttleConcurrentBuilds {
        maxTotal(1)
    }

    parameters {
        booleanParam('DEBUG', false, 'Enable Debug capability')

        booleanParam('SKIP_TESTS', false, 'Skip testing')

        stringParam('CUSTOM_BRANCH_KEY', '', 'To generate only some custom repos... Branch key to use for job generation. This is useful if you use')
        stringParam('CUSTOM_REPOSITORIES', '', 'To generate only some custom repos... Comma list of `repo[:branch]`. Example: `kogito-pipelines:any_change`. If no branch is given, then `main` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')
        stringParam('CUSTOM_AUTHOR', '', 'To generate only some custom repos... Define from from which author the custom repositories are checked out. If none given, then `SEED_AUTHOR` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')
        stringParam('CUSTOM_MAIN_BRANCH', '', 'To generate only some custom repos... If no main_branch is given, then DSL config `git.main_branch` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')

        stringParam('SEED_AUTHOR', 'radtriste', 'If different from the default')
        stringParam('SEED_BRANCH', 'kogito-7110', 'If different from the default')
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url("https://github.com/${getSeedAuthor()}/kogito-pipelines.git")
                        credentials('kie-ci')
                    }
                    branch(getSeedBranch())
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath('dsl/seed/jobs/Jenkinsfile.seed.main')
        }
    }

    properties {
        githubProjectUrl("https://github.com/${getSeedAuthor()}/kogito-pipelines/")
    }
}

def jobParams = KogitoJobUtils.getDefaultJobParams(this, KogitoConstants.KOGITO_PIPELINES_REPOSITORY)
jobParams.job.name = '0-prepare-release-branch'
jobParams.job.folder = ''
jobParams.jenkinsfile = '.ci/jenkins//Jenkinsfile.release.prepare'
jobParams.job.description = 'Prepare env for a release'
KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
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