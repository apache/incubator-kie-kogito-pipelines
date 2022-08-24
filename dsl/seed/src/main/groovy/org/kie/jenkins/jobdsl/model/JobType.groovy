package org.kie.jenkins.jobdsl.model

import org.kie.jenkins.jobdsl.Utils

/*
* JobType corresponds to a type of job.
*
* So a job type can be disabled and the closture `isActiveClosure` should be initialized else it returns `true` by default.
*/
class JobType {

    /**
     * @deprecated Please use SETUP_BRANCH instead
     */
    @Deprecated
    public static final JobType INIT_BRANCH = new JobType(
        name: 'init-branch',
        isActiveClosure: { script -> !Utils.isMainBranch(script) },
    )
    public static final JobType SETUP_BRANCH = new JobType(
        name: 'setup-branch',
    )
    public static final JobType NIGHTLY = new JobType(
        name: 'nightly',
    )
    public static final JobType OTHER = new JobType(
        name: 'other'
    )
    public static final JobType PULLREQUEST = new JobType(
        name: 'pullrequest',
    )
    public static final JobType RELEASE = new JobType(
        name: 'release',
        isActiveClosure: { script -> !Utils.isMainBranch(script) },
    )
    public static final JobType TOOLS = new JobType(
        name: 'tools'
    )

    String name
    Closure isActiveClosure

    String toName() {
        return this.name
    }

    boolean isActive(def script) {
        return this.isActiveByConfig(script) && (this.isActiveClosure ? this.isActiveClosure(script) : true)
    }

    private boolean isActiveByConfig(def script) {
        return !Utils.isJobTypeDisabled(script, this.name)
    }

    private static Set<JobType> JOB_TYPES = [
        INIT_BRANCH,
        SETUP_BRANCH,
        NIGHTLY,
        OTHER,
        PULLREQUEST,
        RELEASE,
        TOOLS,
    ]

    static void register(JobType jobType) {
        JOB_TYPES.add(jobType)
    }

    static List getAllRegistered() {
        return new ArrayList(JOB_TYPES)
    }

    static JobType getByName(String name) {
        return JOB_TYPES.find { it.name == name }
    }

}
