// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.SeedJobUtils

String getSeedAuthor() {
    return SEED_AUTHOR ?: 'kiegroup'
}

String getSeedBranch() {
    return SEED_BRANCH ?: 'main'
}

String getSeedConfigFileGitRepository() {
    return SEED_CONFIG_FILE_GIT_REPOSITORY ?: 'kogito-pipelines'
}

String getSeedConfigFileGitAuthorName() {
    return SEED_CONFIG_FILE_GIT_AUTHOR_NAME ?: 'kiegroup'
}

String getSeedConfigFileGitAuthorCredsId() {
    return SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID ?: KogitoConstants.DEFAULT_CREDENTIALS_ID
}

String getSeedConfigFileGitBranch() {
    return SEED_CONFIG_FILE_GIT_BRANCH ?: 'main'
}

String getSeedConfigFilePath() {
    return SEED_CONFIG_FILE_PATH ?: 'dsl/config/main.yaml'
}

SeedJobUtils.createSeedJobTrigger(
    this,
    'z-seed-trigger-job',
    KogitoConstants.KOGITO_PIPELINES_REPOSITORY,
    getSeedAuthor(),
    getSeedBranch(),
    [
        'dsl/config/main.yaml',
        'dsl/seed/jenkinsfiles/scripts',
        'dsl/seed/jenkinsfiles/Jenkinsfile.seed.main',
        'dsl/seed/jobs/seed_job_main.groovy',
        'dsl/seed/jobs/root_jobs.groovy',
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
        stringParam('SEED_CONFIG_FILE_GIT_REPOSITORY', getSeedConfigFileGitRepository(), 'Repository containing the seed main config file')
        stringParam('SEED_CONFIG_FILE_GIT_AUTHOR_NAME', getSeedConfigFileGitAuthorName(), 'Author name of repository containing the seed main config file')
        stringParam('SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID', getSeedConfigFileGitAuthorCredsId(), 'Credentials Id for the author of repository containing the seed main config file')
        stringParam('SEED_CONFIG_FILE_GIT_BRANCH', getSeedConfigFileGitBranch(), 'Branch of repository containing the seed main config file')
        stringParam('SEED_CONFIG_FILE_PATH', getSeedConfigFilePath(), 'Path on repository containing the seed main config file')

        booleanParam('DEBUG', false, 'Enable Debug capability')

        booleanParam('SKIP_TESTS', false, 'Skip testing')

        stringParam('SEED_AUTHOR', getSeedAuthor(), 'Author to the kogito-pipelines seed repo')
        stringParam('SEED_BRANCH', getSeedBranch(), 'Branch to the kogito-pipelines seed repo')
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url("https://github.com/${getSeedAuthor()}/kogito-pipelines.git")
                        credentials(KogitoConstants.DEFAULT_CREDENTIALS_ID)
                    }
                    branch(getSeedBranch())
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath('dsl/seed/jenkinsfiles/Jenkinsfile.seed.main')
        }
    }

    properties {
        githubProjectUrl("https://github.com/${getSeedAuthor()}/kogito-pipelines/")
    }
}
