package org.kie.jenkins.jobdsl

class KogitoConstants {

    static String KOGITO_DEFAULT_PR_TRIGGER_PHRASE = '.*[j|J]enkins,?.*(retest|test) this.*'
    
    static String SEED_JENKINSFILES_PATH = '.ci/jenkins'

    static String LABEL_DSL_TEST = 'dsl-test'
}
