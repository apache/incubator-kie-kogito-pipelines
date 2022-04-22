// import org.kie.jenkins.jobdsl.templates.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoConstants
// import org.kie.jenkins.jobdsl.FolderUtils
// import org.kie.jenkins.jobdsl.Utils
import org.kie.jenkins.jobdsl.SeedJobUtils

SeedJobUtils.createSeedJobTrigger(
    this,
    'test-shared-lib-trigger-job',
    KogitoConstants.KOGITO_PIPELINES_REPOSITORY,
    'kiegroup',
    'test_seed_trigger',
    [],
    'test-shared-lib-trigger-job')

pipelineJob('test-raw-trigger-job') {
    description('This job listens to pipelines repo and launch the seed job if needed. DO NOT USE FOR TESTING !!!! See https://github.com/kiegroup/kogito-pipelines/blob/main/docs/jenkins.md#test-specific-jobs')

    logRotator {
        numToKeep(5)
    }

    throttleConcurrentBuilds {
        maxTotal(1)
    }

    parameters {
        booleanParam('FORCE_REBUILD', false, 'Default, the job will scan for modified files and do the update in case some files are modified. In case you want to force the DSL generation')
    }

    environmentVariables {
        env('JOB_RELATIVE_PATH_TO_TRIGGER', 'test-raw-lib-trigger-job')
        env('LISTEN_TO_MODIFIED_PATHS', new groovy.json.JsonBuilder([]).toString())
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/kiegroup/kogito-pipelines.git')
                        credentials('kie-ci')
                    }
                    branch('test_seed_trigger')
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath('dsl/seed/jobs/Jenkinsfile.seed.trigger')
        }
    }

    properties {
        githubProjectUrl('https://github.com/kiegroup/kogito-pipelines/')

        pipelineTriggers {
            triggers {
                gitHubPushTrigger()
            }
        }
    }
}
