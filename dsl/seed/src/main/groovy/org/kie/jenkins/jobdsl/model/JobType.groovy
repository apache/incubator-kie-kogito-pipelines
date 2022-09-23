package org.kie.jenkins.jobdsl.model

import org.kie.jenkins.jobdsl.Utils

/*
* JobType corresponds to a type of job.
*
* So a job type can be disabled and the closture `isActiveClosure` should be initialized else it returns `true` by default.
*/
class JobType {

    public static final JobType NIGHTLY = new JobType(
        name: 'nightly',
        environmentDependent: true
    )
    public static final JobType OTHER = new JobType(
        name: 'other'
    )

    /**
    * *DEPRECATED* section
    * Should be deleted once https://issues.redhat.com/browse/PLANNER-2870 is implemented
    **/
    @Deprecated
    public static final JobType PULLREQUEST = new JobType(
        name: 'pullrequest',
    )
    
    public static final JobType PULL_REQUEST = new JobType(
        name: 'pullrequest',
        environmentDependent: true
    )
    public static final JobType RELEASE = new JobType(
        name: 'release',
        isActiveClosure: { script -> !Utils.isMainBranch(script) },
        defaultEnv: [ RELEASE: 'true' ],
    )
    public static final JobType SETUP_BRANCH = new JobType(
        name: 'setup-branch',
    )
    public static final JobType TOOLS = new JobType(
        name: 'tools'
    )

    String name
    boolean environmentDependent
    Closure isActiveClosure
    Map defaultEnv = [:]

    String getName() {
        return this.name
    }

    boolean isEnvironmentDependent() {
        return environmentDependent
    }

    boolean isActive(def script) {
        return this.isActiveByConfig(script) && (this.isActiveClosure ? this.isActiveClosure(script) : true)
    }

    Map getDefaultEnvVars() {
        return this.defaultEnv
    }

    private boolean isActiveByConfig(def script) {
        return !Utils.isJobTypeDisabled(script, this.name)
    }
}
