package org.kie.jenkins.jobdsl

class KogitoConstants {

    static String KOGITO_DEFAULT_PR_TRIGGER_PHRASE = '.*[j|J]enkins,?.*(retest|test) this.*'
    
    static String SEED_JENKINSFILES_PATH = '.ci/jenkins'

    static String BUILD_CHAIN_JENKINSFILE = 'Jenkinsfile.buildchain'

    static String CLOUD_IMAGE_DEFAULT_PARAMS_PREFIX = ''
    static String CLOUD_IMAGE_BASE_PARAMS_PREFIX = 'BASE'
    static String CLOUD_IMAGE_PROMOTE_PARAMS_PREFIX = 'PROMOTE'
}
