
package org.kie.jenkins.jobdsl

class FolderUtils {

    static String NIGHTLY_FOLDER = 'nightly'
    static String RELEASE_FOLDER = 'release'
    static String TOOLS_FOLDER = 'tools'
    static String PULLREQUEST_FOLDER = 'pullrequest'
    static String OTHER_FOLDER = 'other'
    static String PULLREQUEST_RUNTIMES_BDD_FOLDER = "${PULLREQUEST_FOLDER}/kogito-runtimes.bdd"

    static List getAllNeededFolders() {
        return [
            NIGHTLY_FOLDER,
            RELEASE_FOLDER,
            TOOLS_FOLDER,
            PULLREQUEST_FOLDER,
            PULLREQUEST_RUNTIMES_BDD_FOLDER,
            OTHER_FOLDER,
        ]
    }

    static String getNightlyFolder(def script) {
        return NIGHTLY_FOLDER
    }

    static String getReleaseFolder(def script) {
        return RELEASE_FOLDER
    }

    static String getToolsFolder(def script) {
        return TOOLS_FOLDER
    }

    static String getPullRequestFolder(def script) {
        return PULLREQUEST_FOLDER
    }

    static String getPullRequestRuntimesBDDFolder(def script) {
        return PULLREQUEST_RUNTIMES_BDD_FOLDER
    }

    static String getOtherFolder(def script) {
        return OTHER_FOLDER
    }

}
