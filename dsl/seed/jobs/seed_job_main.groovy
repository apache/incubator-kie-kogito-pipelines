// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

import org.kie.jenkins.jobdsl.SeedJobUtils
import org.kie.jenkins.jobdsl.Utils

KOGITO_PIPELINES_REPOSITORY = 'kogito-pipelines'
DEFAULT_CREDENTIALS_ID = 'kie-ci'

String getSeedRepo() {
    return Utils.getSeedRepo(this) ?: KOGITO_PIPELINES_REPOSITORY
}

String getSeedAuthor() {
    return Utils.getSeedAuthor(this) ?: 'kiegroup'
}

String getSeedAuthorCredsId() {
    return Utils.getSeedAuthorCredsId(this) ?: DEFAULT_CREDENTIALS_ID
}

String getSeedBranch() {
    return Utils.getSeedBranch(this) ?: 'main'
}

String getSeedConfigFileGitRepository() {
    return SEED_CONFIG_FILE_GIT_REPOSITORY ?: KOGITO_PIPELINES_REPOSITORY
}

String getSeedConfigFileGitAuthorName() {
    return SEED_CONFIG_FILE_GIT_AUTHOR_NAME ?: 'kiegroup'
}

String getSeedConfigFileGitAuthorCredsId() {
    return SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID ?: DEFAULT_CREDENTIALS_ID
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
    getSeedRepo(),
    getSeedAuthor(),
    getSeedAuthorCredsId(),
    getSeedBranch(),
    [
        'dsl/seed/jenkinsfiles/scripts',
        'dsl/seed/jenkinsfiles/Jenkinsfile.seed.main',
        'dsl/seed/jobs/seed_job_main.groovy',
        'dsl/seed/jobs/root_jobs.groovy',
    ],
    './0-seed-job')

SeedJobUtils.createSeedJobTrigger(
    this,
    'z-seed-config-trigger-job',
    "${SEED_CONFIG_FILE_GIT_REPOSITORY}",
    "${SEED_CONFIG_FILE_GIT_AUTHOR_NAME}",
    "${SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID}",
    "${SEED_CONFIG_FILE_GIT_BRANCH}",
    [
        "${SEED_CONFIG_FILE_PATH}",
    ],
    './0-seed-job'
)

// Configuration of the seed and generated jobs is done via `dsl/seed/config.yaml`
pipelineJob('0-seed-job') {
    description('This job creates all needed Jenkins jobs. DO NOT USE FOR TESTING !!!! See https://github.com/kiegroup/kogito-pipelines/blob/main/docs/jenkins.md#test-specific-jobs')

    logRotator {
        numToKeep(5)
    }

    throttleConcurrentBuilds {
        maxTotal(1)
    }

    environmentVariables {
        env('AGENT_LABEL', Utils.isProdEnvironment(this) ? 'kie-rhel8 && !built-in' : 'kie-rhel8-priority')
    }

    parameters {
        stringParam('SEED_CONFIG_FILE_GIT_REPOSITORY', getSeedConfigFileGitRepository(), 'Repository containing the seed main config file')
        stringParam('SEED_CONFIG_FILE_GIT_AUTHOR_NAME', getSeedConfigFileGitAuthorName(), 'Author name of repository containing the seed main config file')
        stringParam('SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID', getSeedConfigFileGitAuthorCredsId(), 'Credentials Id for the author of repository containing the seed main config file')
        stringParam('SEED_CONFIG_FILE_GIT_BRANCH', getSeedConfigFileGitBranch(), 'Branch of repository containing the seed main config file')
        stringParam('SEED_CONFIG_FILE_PATH', getSeedConfigFilePath(), 'Path on repository containing the seed main config file')

        booleanParam('DEBUG', false, 'Enable Debug capability')

        booleanParam('SKIP_TESTS', false, 'Skip testing')

        stringParam('SEED_REPO', getSeedRepo(), 'Seed repository name')
        stringParam('SEED_AUTHOR', getSeedAuthor(), 'Author of the seed repo')
        stringParam('SEED_AUTHOR_CREDS_ID', getSeedAuthorCredsId(), 'Credentials Id for the author of the seed repo')
        stringParam('SEED_BRANCH', getSeedBranch(), 'Branch to the seed repo')
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/${SEED_AUTHOR}/${SEED_REPO}.git')
                        credentials('${SEED_AUTHOR_CREDS_ID}')
                    }
                    branch('${SEED_BRANCH}')
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath('dsl/seed/jenkinsfiles/Jenkinsfile.seed.main')
        }
    }

    properties {
        githubProjectUrl("https://github.com/${getSeedAuthor()}/${getSeedRepo()}/")
    }
}
