import groovy.json.JsonOutput

String getDslSeedFolderAbsolutePath(String seedRepoPath = '') {
    String path = "${WORKSPACE}"
    if (seedRepoPath) {
        path += "/${seedRepoPath}"
    }
    return "${path}/dsl/seed"
}

String getSeedConfigFilePath(String seedRepoPath = '') {
    return "${getDslSeedFolderAbsolutePath(seedRepoPath)}/config/main.yaml"
}

boolean isDebug() {
    return params.DEBUG ?: false
}

Map parseCustomRepositories(String customRepositoriesParam) {
    if (customRepositoriesParam) {
        List repoBranches =  customRepositoriesParam.split(',') as List
        return repoBranches.collectEntries { repoBranch ->
            repoBranchSplit = repoBranch.split(':') as List
            String repo = ''
            String branch = ''
            if (repoBranchSplit.size() > 2) {
                error "Repository should be in the format of `repo[:branch]` and got value ${repoBranch}"
            } else {
                repo = repoBranchSplit[0]
                if (repoBranchSplit.size() == 2) {
                    branch = repoBranchSplit[1]
                }
            }
            return [ (repo), branch ]
        }
    }
    return [:]
}

def deepCopyObject(def originalMap) {
    return readJSON(text: JsonOutput.toJson(originalMap))
}

def getRepoConfig(String repository, String generationBranch, String seedRepoPath = '') {
    def branchConfig = readBranchConfig(seedRepoPath)
    def repoConfig = branchConfig.repositories.find { it.name == repository }

    def cfg = deepCopyObject(branchConfig)
    cfg.remove('repositories')

    cfg.git.branch = repoConfig.branch ?: generationBranch
    cfg.git.jenkins_config_path = repoConfig.jenkins_config_path ?: cfg.git.jenkins_config_path

    if (repoConfig.author) {
        cfg.git.author.name = repoConfig.author.name ?: cfg.git.author.name
        cfg.git.author.credentials_id = repoConfig.author.credentials_id ?: cfg.git.author.credentials_id
        cfg.git.author.token_credentials_id = repoConfig.author.credentials_id ?: cfg.git.author.token_credentials_id
    }
    if (repoConfig.bot_author) {
        cfg.git.bot_author.name = repoConfig.bot_author.name ?: cfg.git.bot_author.name
        cfg.git.bot_author.credentials_id = repoConfig.bot_author.credentials_id ?: cfg.git.bot_author.credentials_id
    }
    if (isDebug()) {
        println "[DEBUG] Repo config:"
        println "[DEBUG] ${cfg}"
    }
    return cfg
}

def readBranchConfig(String seedRepoPath = '') {
    def branchConfig = readYaml(file: getBranchConfigFilePath(seedRepoPath))
    Map customRepos = parseCustomRepositories(params.CUSTOM_REPOSITORIES)
    if (customRepos) {
        branchConfig = deepCopyObject(branchConfig)
        branchConfig.repositories = []
        customRepos.each { repoName, branch ->
            def cfg = [
                name : repoName,
                branch : branch,
            ]
            if (getCustomAuthor()) {
                cfg.author = [ name : getCustomAuthor()]
            }
            branchConfig.repositories.add(cfg)
        }
    }
    if (isDebug()) {
        println '[DEBUG] Branch config:'
        println "[DEBUG] ${branchConfig}"
    }
    return branchConfig
}

String getBranchConfigFilePath(String seedRepoPath = '') {
    return "${getDslSeedFolderAbsolutePath(seedRepoPath)}/config/branch.yaml"
}

String getCustomAuthor() {
    return params.CUSTOM_AUTHOR
}

boolean arePathsModified(List<String> paths) {
    def modified = false
    def changeLogSets = currentBuild.changeSets
    for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = 0; j < entries.length; j++) {
            def entry = entries[j]
            def files = new ArrayList(entry.affectedFiles)
            for (int k = 0; k < files.size(); k++) {
                def file = files[k]

                if (isDebug()) {
                    println "[DEBUG] ${file.path}"
                }

                if (paths.any { file.path.startsWith(it) }) {
                    if (isDebug()) {
                        println "[DEBUG] Modified path ${file.path} is taken into account"
                    }
                    modified = true
                }
            }
        }
    }
    return modified
}

return this
