package org.kie.jenkins.jobdsl.model

import org.kie.jenkins.jobdsl.Utils

/*
* JobType corresponds to a type of job.
*
* Also a job type can be optional.
* So a job type can be disabled and the closture `isActiveClosure` should be initialized else it returns `true` by default.
*/
enum JobType {

    PULLREQUEST,
    NIGHTLY(
        optional: true,
    ),
    // UPDATE_VERSION,
    RELEASE(
        optional: true,
        isActiveClosure: { script -> !Utils.isMainBranch(script) }
    ),
    TOOLS,
    OTHER

    boolean optional
    Closure isActiveClosure

    String toFolderName() {
        return this.name().toLowerCase().replaceAll('_', '-')
    }

    boolean isOptional() {
        return this.optional
    }

    boolean isActive(def script) {
        return !this.isOptional() || (this.isActiveClosure ? this.isActiveClosure(script) : true)
    }
}
