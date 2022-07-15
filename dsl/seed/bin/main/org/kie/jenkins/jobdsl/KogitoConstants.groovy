package org.kie.jenkins.jobdsl

class KogitoConstants {
  static String KOGITO_DEFAULT_PR_TRIGGER_PHRASE = '.*[j|J]enkins,?.*(retest|test) this.*'
  static String KOGITO_LTS_PR_TRIGGER_PHRASE = '.*[j|J]enkins,? run LTS[ tests]?.*'
  static String KOGITO_NATIVE_PR_TRIGGER_PHRASE = '.*[j|J]enkins,? run native[ tests]?.*'

  static String KOGITO_PIPELINES_REPOSITORY = 'kogito-pipelines'

  static String BUILDCHAIN_JENKINSFILE_PATH = '.ci/jenkins/Jenkinsfile.buildchain'
  static String BUILDCHAIN_REPOSITORY = KOGITO_PIPELINES_REPOSITORY

  static String DEFAULT_NATIVE_CONTAINER_PARAMS = '-Dquarkus.native.container-build=true -Dnative'
}