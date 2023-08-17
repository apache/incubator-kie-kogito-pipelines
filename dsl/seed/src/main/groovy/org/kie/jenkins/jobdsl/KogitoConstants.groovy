package org.kie.jenkins.jobdsl

class KogitoConstants {

    static String KOGITO_DEFAULT_PR_TRIGGER_PHRASE = '.*[j|J]enkins,?.*(retest|test) this.*'

    static String SEED_JENKINSFILES_PATH = 'dsl/seed/jenkinsfiles'

    static String LABEL_DSL_TEST = 'dsl-test'

    static String PIPELINE_PROPERTIES_FILENAME = 'pipeline.properties'

    static String BUILD_CHAIN_JENKINSFILE = 'Jenkinsfile.buildchain'

}
