// +++++++++++++++++++++++++++++++++++++++++++ root jobs ++++++++++++++++++++++++++++++++++++++++++++++++++++

import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.KogitoJobTemplate
import org.kie.jenkins.jobdsl.Utils

def jobParams = [
    job: [
        name: '0-prepare-release-branch',
        description: 'Prepare env for a release',
    ],
    git: [
        repository: Utils.getSeedRepo(this),
        author: Utils.getSeedAuthor(this),
        credentials: Utils.getSeedAuthorCredsId(this),
        branch: Utils.getSeedBranch(this),
    ],
    env: [:],
    jenkinsfile: 'dsl/seed/jenkinsfiles/Jenkinsfile.release.prepare',
]

KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
    parameters {
        RELEASE_PROJECTS.split(',').each { projectName ->
            stringParam("${projectName}_VERSION".toUpperCase(), '', "${Utils.getRepoNameCamelCase(projectName)} version to release as Major.minor.micro")
        }

        booleanParam('PRODUCTIZED_BRANCH', false, 'Is the created branch a productized one ?')
    }

    environmentVariables {
        env('JENKINS_EMAIL_CREDS_ID', Utils.getJenkinsEmailCredsId(this))

        env('SEED_CONFIG_FILE_GIT_REPOSITORY', "${SEED_CONFIG_FILE_GIT_REPOSITORY}")
        env('SEED_CONFIG_FILE_GIT_AUTHOR_NAME', "${SEED_CONFIG_FILE_GIT_AUTHOR_NAME}")
        env('SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID', "${SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID}")
        env('SEED_CONFIG_FILE_GIT_BRANCH', "${SEED_CONFIG_FILE_GIT_BRANCH}")
        env('SEED_CONFIG_FILE_PATH', "${SEED_CONFIG_FILE_PATH}")

        env('SEED_REPO', Utils.getSeedRepo(this))
        env('SEED_AUTHOR', Utils.getSeedAuthor(this))
        env('SEED_BRANCH', Utils.getSeedBranch(this))
        env('SEED_CREDENTIALS_ID', Utils.getSeedAuthorCredsId(this))
    }
}

def jobParamsRemove = [
    job: [
        name: '0-remove-branch',
        description: 'Remove branches',
    ],
    git: [
        repository: Utils.getSeedRepo(this),
        author: Utils.getSeedAuthor(this),
        credentials: Utils.getSeedAuthorCredsId(this),
        branch: Utils.getSeedBranch(this),
    ],
    env: [:],
    jenkinsfile: 'dsl/seed/jenkinsfiles/Jenkinsfile.remove.branches',
]

KogitoJobTemplate.createPipelineJob(this, jobParamsRemove)?.with {
    parameters {
        choiceParam('BRANCH_TO_REMOVE', ALL_BRANCHES.split(',').findAll { it != MAIN_BRANCH_NAME }, 'Which release branch to remove ?') 
    }

    environmentVariables {
        env('JENKINS_EMAIL_CREDS_ID', Utils.getJenkinsEmailCredsId(this))

        env('GIT_REPOSITORY', "${SEED_CONFIG_FILE_GIT_REPOSITORY}")
        env('GIT_AUTHOR', "${SEED_CONFIG_FILE_GIT_AUTHOR_NAME}")
        env('GIT_AUTHOR_CREDENTIALS_ID', "${SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID}")
        env('GIT_BRANCH_TO_BUILD', "${SEED_CONFIG_FILE_GIT_BRANCH}")
    }
}