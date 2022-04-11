// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

import org.kie.jenkins.jobdsl.KogitoConstants
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

        stringParam('SEED_AUTHOR', getSeedAuthor(), 'If different from the default')
        stringParam('SEED_BRANCH', getSeedBranch(), 'If different from the default')
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
