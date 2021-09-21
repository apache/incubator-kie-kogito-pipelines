package org.kie.jenkins.jobdsl

class KogitoConstants {
  static String KOGITO_DEFAULT_PR_TRIGGER_PHRASE = '.*[j|J]enkins,?.*(retest|test) this.*'
  static String KOGITO_LTS_PR_TRIGGER_PHRASE = '.*[j|J]enkins,? run LTS[ tests]?.*'
  static String KOGITO_NATIVE_PR_TRIGGER_PHRASE = '.*[j|J]enkins,? run native[ tests]?.*'
}