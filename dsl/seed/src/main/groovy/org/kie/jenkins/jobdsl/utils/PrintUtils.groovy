package org.kie.jenkins.jobdsl.utils

import org.kie.jenkins.jobdsl.Utils

class PrintUtils {

    static void warn(def script, String output) {
        script.println("[WARN] ${output}")
    }

    static void info(def script, String output) {
        script.println("[INFO] ${output}")
    }

    static void debug(def script, String output) {
        if (Utils.getBindingValue(script, 'DEBUG')?.toBoolean()) {
            script.println("[DEBUG] ${output}")
        }
    }

    static void deprecated(def script, String output, String alternative = '') {
        script.println("[DEPRECATED] ${output}${alternative ? ". Please use ${alternative} instead" : ''}")
    }

}
