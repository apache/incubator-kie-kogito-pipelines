package org.kie.jenkins.jobdsl

class KogitoConstants {
  static String KOGITO_DSL_NIGHTLY_FOLDER = 'nightly'
  static String KOGITO_DSL_RELEASE_FOLDER = 'release'
  static String KOGITO_DSL_TOOLS_FOLDER = 'tools'
  static String KOGITO_DSL_PULLREQUEST_FOLDER = 'pullrequest'
  static String KOGITO_DSL_OTHER_FOLDER = 'other'
  static String KOGITO_DSL_RUNTIMES_BDD_FOLDER = 'kogito-runtimes.bdd'

  static String KOGITO_DEFAULT_PR_TRIGGER_PHRASE = '.*[j|J]enkins,?.*(retest|test) this.*'
  static String KOGITO_LTS_PR_TRIGGER_PHRASE = '.*[j|J]enkins,? run LTS[ tests]?.*'
  static String KOGITO_NATIVE_PR_TRIGGER_PHRASE = '.*[j|J]enkins,? run native[ tests]?.*'

  static String BUILDCHAIN_JENKINSFILE_PATH = '.ci/jenkins/Jenkinsfile.buildchain'
  static String BUILDCHAIN_REPOSITORY = 'kogito-pipelines'
}