
package org.kie.jenkins.jobdsl

class RegexUtils {

    static String getRegexFirstLetterCase(String id) {
        return id.length() > 1 ? "${getRegexLetterStringCase(id.substring(0, 1))}${id.substring(1).toLowerCase()}" : getRegexLetterStringCase(id)
    }

    static String getRegexLetterStringCase(String str) {
        return "[${str.toUpperCase()}|${str.toLowerCase()}]"
    }

    static String getRegexMultipleCase(String str) {
        return "(${str.toUpperCase()}|${str.toLowerCase()}|${Utils.firstLetterUpperCase(str.toLowerCase())})"
    }
}
