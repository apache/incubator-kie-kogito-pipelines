
package org.kie.jenkins.jobdsl

class VersionUtils {

    static String getProjectTargetBranch(String project, String branch, String upstreamProject) {
        if (isOptaplannerQuickstartsProject(upstreamProject) && branch == 'development') {
            return 'main'
        }
        if (isKogitoProject(project)) {
            return getTargetBranch(branch, isOptaPlannerProject(upstreamProject) ? -7 : 0)
        } else if (isOptaPlannerProject(project)) {
            return getTargetBranch(branch, isOptaPlannerProject(upstreamProject) ? 0 : 7)
        } else {
            throw new Exception()
        }
    }

    static String getTargetBranch(String branch, Integer addToMajor) {
        String targetBranch = branch
        String [] versionSplit = targetBranch.split("\\.")
        if (versionSplit.length == 3
            && versionSplit[0].isNumber()
            && versionSplit[1].isNumber()
            && versionSplit[2] == 'x') {
            targetBranch = "${Integer.parseInt(versionSplit[0]) + addToMajor}.${versionSplit[1]}.x"
        } else {
            println "Cannot parse branch as release branch so going further with current value: ${branch}"
        }
        return targetBranch
    }

    static boolean isKogitoProject(String project) {
        return project.startsWith('kogito')
    }

    static boolean isOptaplannerProject(String project) {
        return project.startsWith('opta')
    }

    static boolean isOptaplannerQuickstartsProject(String project) {
        return project == 'optaplanner-quickstarts'
    }

}
