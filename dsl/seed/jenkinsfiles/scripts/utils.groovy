import groovy.json.JsonOutput

String getDslSeedFolderAbsolutePath(String seedRepoPath = '') {
    String path = "${WORKSPACE}"
    if (seedRepoPath) {
        path += "/${seedRepoPath}"
    }
    return "${path}/dsl/seed"
}

boolean isDebug() {
    return params.DEBUG ?: false
}

def deepCopyObject(def originalMap) {
    return readJSON(text: JsonOutput.toJson(originalMap))
}

boolean getMainBranch(Map mainBranches, String repository) {
    return mainBranches.get(mainBranches.containsKey(repository) ? repository : 'default')
}

return this
