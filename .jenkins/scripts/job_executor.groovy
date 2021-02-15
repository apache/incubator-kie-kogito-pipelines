

def executeJobWithRetry(String jobPath, List buildParams, Closure notifier) {
    echo "Build ${jobPath} with params ${buildParams}"

    def job = build(job: jobPath, wait: true, parameters: buildParams, propagate: false)
    def jobResult = job.getResult()
    def jobUrl = job.getAbsoluteUrl()
    if (jobResult != 'SUCCESS') {
        echo "Sending a notification about an unsuccessful job build ${jobName}."
        notifier("`${jobName}` finished with status `${jobResult}`.\nSee: ${jobUrl}\n\nPlease provide which action should be done (retry ? continue ? skip ? abort ?): ${env.BUILD_URL}input")

        // abort is handled automatically by the pipeline in the input
        def result = input message: "Job `${jobName}` is in status ${jobResult}. What do you want to do ?\nBeware that skipping a deploy job will not launch the promote part.", parameters: [choice(name: 'ACTION', choices: ['retry', 'continue', 'skip'].join('\n')), string(name: 'MESSAGE', description: 'If you want to add information to your action...')]
        String resultStr = "`${jobName}` failure => Decision was made to ${result['ACTION']}."
        if (result['MESSAGE'] != '') {
            resultStr += "Additional Information: `${result['MESSAGE']}`"
        }
        echo resultStr
        notifier(resultStr)

        // If skip, do not do anything (no registration)
        if (result['ACTION'] == 'retry') {
            return executeJobWithRetry(jobName, buildParams)
        } else if (result['ACTION'] == 'continue') {
            registerJob(jobName, job)
        }
    } else {
        // Succeeded
        registerJob(jobName, job)
    }

    return job
}

def executeJob(String jobPath, List buildParams, boolean propagate=true) {
    echo "Build ${jobName} with params ${buildParams} and propagate = ${propagate}"

    def job = build(job: jobPath, wait: true, parameters: buildParams, propagate: propagate)
    registerJob(jobName, job)

    // Set Unstable if we don't propagate and job failed
    if (!propagate && !isJobSucceeded(jobName)) {
        addUnstableStage(jobName)
        unstable("Job ${jobName} finished with result ${job.getResult()}")
    }
    return job
}

def registerJob(String jobName, def job) {
    JOBS[jobName] = job
}

def getJob(String jobName) {
    return JOBS[jobName]
}

String getJobUrl(String jobName) {
    echo "getJobUrl for ${jobName}"
    def job = getJob(jobName)
    return job ? job.getAbsoluteUrl() : ''
}

boolean isJobSucceeded(String jobName) {
    def job = getJob(jobName)
    return job ? job.getResult() == 'SUCCESS' : false
}

void addFailedStage(String jobName = '') {
    FAILED_STAGES.put("${STAGE_NAME}", jobName)
}

void addUnstableStage(String jobName = '') {
    UNSTABLE_STAGES.put("${STAGE_NAME}", jobName)
}

// Do not remove this below line as fundamental to reuse func in this script
return this