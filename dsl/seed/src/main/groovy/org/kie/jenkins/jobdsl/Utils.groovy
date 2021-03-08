
package org.kie.jenkins.jobdsl

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class Utils {

    static def deepCopyObject(def originalMap) {
        return new JsonSlurper().parseText(JsonOutput.toJson(originalMap))
    }

    static String createRepositoryUrl(String author, String repositoryName) {
        return "https://github.com/${author}/${repositoryName}.git"
    }

    static String createProjectUrl(String author, String repositoryName) {
        return "https://github.com/${author}/${repositoryName}/"
    }

    static def getBindingValue(def script, String key){
        return script.getBinding()[key]
    }


    static String getQuarkusLTSVersion() {
        return getBindingValue(script, 'QUARKUS_LTS_VERSION')
    }

    static boolean areTriggersDisabled() {
        return getBindingValue(script, 'DISABLE_TRIGGERS').toBoolean()
    }
}
