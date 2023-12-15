/**
* This method add a comment to current PR (for either ghprb or Github Branch Source plugin)
*/
void postComment(String commentText, String githubTokenCredsId = "kie-ci3-token") {
    if (!CHANGE_ID && !ghprbPullId) {
        error "Pull Request Id variable (ghprbPullId or CHANGE_ID) is not set. Are you sure you are running with Github Branch Source plugin or ghprb plugin?"
    }
    def changeId = CHANGE_ID ?: ghprbPullId
    def changeRepository = CHANGE_ID ? getAuthorAndRepoForPr() : ghprbGhRepository
    String filename = "${util.generateHash(10)}.build.summary"
    def jsonComment = [
        body : commentText
    ]
    writeJSON(json: jsonComment, file: filename)
    sh "cat ${filename}"
    withCredentials([string(credentialsId: githubTokenCredsId, variable: 'GITHUB_TOKEN')]) {
        sh "curl -s -H \"Authorization: token ${GITHUB_TOKEN}\" -X POST -d '@${filename}' \"https://api.github.com/repos/${changeRepository}/issues/${changeId}/comments\""
    }
    sh "rm ${filename}"
}

String getAuthorAndRepoForPr() {
    if (!env.CHANGE_FORK && !env.CHANGE_URL) {
        error "CHANGE_FORK neither CHANGE_URL variables are set. Are you sure you're running with Github Branch Source plugin?"
    }
    def group = ''
    def repository = ''
    if (env.CHANGE_FORK) {
        def parsedFork = env.CHANGE_FORK.split('/')
        group = parsedFork[0]
        if (parsedFork.length==2) {
            repository = parsedFork[1]
        }
    }
    String fullUrl = env.CHANGE_URL
    String urlWithoutProtocol = fullUrl.split('://')[1]
    String path = urlWithoutProtocol.substring(urlWithoutProtocol.indexOf('/'))
    def parsedUrl = path.substring(1, path.indexOf('/pull/')).split('/')
    return "${group?:parsedUrl[0]}/${repository?:parsedUrl[1]}"
}