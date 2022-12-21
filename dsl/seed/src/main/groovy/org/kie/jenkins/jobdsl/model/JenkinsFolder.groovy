package org.kie.jenkins.jobdsl.model

import org.kie.jenkins.jobdsl.model.JobType
import org.kie.jenkins.jobdsl.utils.EnvUtils

class JenkinsFolder {

    String name
    JobType jobType
    String environmentName
    Map defaultEnv

    JenkinsFolder(JobType jobType, Map defaultEnv, String environmentName) {
        this.jobType = jobType
        this.defaultEnv = defaultEnv
        this.environmentName = environmentName
    }

    JenkinsFolder(JobType jobType, Map defaultEnv) {
        this(jobType, defaultEnv, '')
    }

    JenkinsFolder(JobType jobType) {
        this(jobType, [:], '')
    }

    String getName() {
        String name = this.jobType.getName()
        name += this.environmentName ? ".${this.environmentName}" : ''
        return name
    }

    String getEnvironmentName() {
        return this.environmentName
    }

    /**
    * *DEPRECATED*
    * Should be deleted once https://issues.redhat.com/browse/PLANNER-2870 is implemented
    *
    * Compatibility method
    */
    Map getDefaultEnvVars(def script) {
        return getDefaultEnvVars()
    }
    Map getDefaultEnvVars() {
        Map env = [
            JOB_TYPE: this.jobType.getName(),
            JOB_ENVIRONMENT: this.environmentName,
        ]
        env.putAll(this.jobType.getDefaultEnvVars() ?: [:])
        env.putAll(this.defaultEnv ?: [:])
        return new HashMap(env)
    }

    // A JenkinsFolder is active if jobType AND environment are active
    boolean isActive(def script) {
        return this.jobType.isActive(script) &&
            (this.environmentName ? EnvUtils.isEnvironmentEnabled(script, this.environmentName) : true)
    }

}
