
package org.kie.jenkins.jobdsl.utils

class VersionUtils {

    static String getProjectTargetBranch(String project, String branch, String upstreamProject) {
        if (isOptaplannerQuickstartsProject(upstreamProject) && branch == 'development') {
            return 'main'
        }
        if (isKogitoProject(project)) {
            return getTargetBranch(branch, isDroolsOrOptaPlannerProject(upstreamProject) ? -7 : 0)
        } else if (isDroolsOrOptaPlannerProject(project)) {
            return getTargetBranch(branch, isDroolsOrOptaPlannerProject(upstreamProject) ? 0 : 7)
        } else {
            throw new Exception()
        }
    }

    static String getTargetBranch(String branch, Integer addToMajor) {
        String targetBranch = branch
        List versionSplit = targetBranch.split("\\.") as List
        if (versionSplit[0].isNumber()) {
            targetBranch = "${Integer.parseInt(versionSplit[0]) + addToMajor}.${versionSplit.tail().join('.')}"
        } else {
            println "Cannot parse branch as release branch so going further with current value: ${branch}"
        }
        return targetBranch
    }
    
    static boolean isKogitoProject(String project) {
        return project.startsWith('kogito') || project.startsWith('incubator-kie-kogito')
    }

    static boolean isOptaplannerProject(String project) {
        return project.startsWith('opta') || project.startsWith('incubator-kie-opta')
    }

    static boolean isDroolsProject(String project) {
        return project.startsWith('drools') || project.startsWith('incubator-kie-drools')
    }

    static boolean isDroolsOrOptaPlannerProject(String project) {
        return isOptaplannerProject(project) || isDroolsProject(project)
    }

    static boolean isOptaplannerQuickstartsProject(String project) {
        return project == 'optaplanner-quickstarts' || project == 'incubator-kie-optaplanner-quickstarts'
    }

}
