jobProperties = [:]
jobPropertiesFilename = 'job_properties.yaml'

void add(String key, def value) {
    if (value) {
        jobProperties.put(key, value)
    }
}

void readFromJobUrl(String jobUrl) {
    assert jobUrl : 'Given job Url is empty'
    String url = "${jobUrl}${jobUrl.endsWith('/') ? '' : '/'}artifact/${jobPropertiesFilename}"
    tempFile = sh(returnStdout: true, script: 'mktemp').trim()
    sh "wget ${url} -O ${tempFile}"
    jobProperties = readYaml(file: tempFile)
}

void writeToFile(boolean archive = true) {
    writeYaml(file: jobPropertiesFilename, data: jobProperties, overwrite: true)
    if (archive) {
        archiveArtifacts(artifacts: jobPropertiesFilename)
    }
}

boolean contains(String key) {
    return jobProperties[key]
}

String retrieve(String key) {
    return jobProperties[key]
}

return this
