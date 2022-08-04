package org.kie.jenkins.jobdsl.model

import org.kie.jenkins.jobdsl.Utils

/*
* JobType corresponds to a type of job.
*
* Also a job type can be optional.
* So a job type can be disabled and the closture `isActiveClosure` should be initialized else it returns `true` by default.
*/
class JobType {

    public static final JobType INIT = new JobType(
        name: 'INIT',
    )
    public static final JobType PULLREQUEST = new JobType(
        name: 'PULLREQUEST',
    )
    public static final JobType NIGHTLY = new JobType(
        name: 'NIGHTLY',
        optional: true,
    )
    public static final JobType RELEASE = new JobType(
        name: 'RELEASE',
        optional: true,
        isActiveClosure: { script -> !Utils.isMainBranch(script) }
    )
    public static final JobType TOOLS = new JobType(
        name: 'TOOLS'
    )
    public static final JobType OTHER = new JobType(
        name: 'OTHER'
    )

    String name
    boolean optional
    Closure isActiveClosure

    String toName() {
        return this.name.toLowerCase().replaceAll('_', '-')
    }

    boolean isOptional() {
        return this.optional
    }

    boolean isActive(def script) {
        return !this.isOptional() || (this.isActiveClosure ? this.isActiveClosure(script) : true)
    }

    private static Set<JobType> JOB_TYPES = [
        INIT,
        PULLREQUEST,
        NIGHTLY,
        RELEASE,
        TOOLS,
        OTHER
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
