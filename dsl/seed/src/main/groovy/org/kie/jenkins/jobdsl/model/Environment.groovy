package org.kie.jenkins.jobdsl.model

import org.kie.jenkins.jobdsl.Utils

/*
* Environment represents the environment is which a job should run
*
* Each environment can contain default env variables which will be added automatically to the job.
* For this `getDefaultEnvVarsClosure` should be initialized.
*
* Also an environment can be optional.
* So an environment can be disabled and the method `isActiveClosure` should be initialized else it returns `true` by default.
*
*
*/
class Environment {

    String name
    boolean optional
    boolean cloudEnabled
    Closure isActiveClosure
    Closure getDefaultEnvVarsClosure

    String toId() {
        return this.name.toLowerCase().replaceAll('_', '-')
    }

    String toName() {
        return Utils.getRepoNameCamelCase(this.toId())
    }

    boolean isOptional() {
        return this.optional
    }

    boolean isCloudEnabled() {
        return this.cloudEnabled
    }

    boolean isActive(def script) {
        return !this.isOptional() || (this.isActiveClosure ? this.isActiveClosure(script) : true)
    }

    Map getDefaultEnvVars(def script) {
        Map envVars = this.getDefaultEnvVarsClosure ? this.getDefaultEnvVarsClosure(script) : [:]
        envVars.putAll([
            JOB_ENVIRONMENT: this.toName(),
            JOB_ENVIRONMENT_CLOUD_ENABLED: "${this.isCloudEnabled()}",
        ])
        return envVars
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Environment Management

    public static final Environment DEFAULT = new Environment(
        name: 'DEFAULT',
        optional: false,
        cloudEnabled: true,
    )

    public static final Environment SONARCLOUD = new Environment(
        name: 'SONARCLOUD',
        optional: true,
        isActiveClosure: { script -> Utils.isMainBranch(script) },
        getDefaultEnvVarsClosure: { script ->
            [
                ENABLE_SONARCLOUD: 'true'
            ]
        },
    )

    public static final Environment NATIVE = new Environment(
        name: 'NATIVE',
        optional: true,
        cloudEnabled: true,
        isActiveClosure: { script -> Utils.isEnvironmentNativeEnabled(script) },
        getDefaultEnvVarsClosure: { script ->
            [
                NATIVE: 'true',
                ADDITIONAL_TIMEOUT: 720
            ]
        }
    )

    public static final Environment MANDREL = new Environment(
        name: 'MANDREL',
        optional: true,
        cloudEnabled: true,
        isActiveClosure: { script -> Utils.isEnvironmentMandrelEnabled(script) },
        getDefaultEnvVarsClosure: { script ->
            [
                NATIVE: 'true',
                NATIVE_BUILDER_IMAGE: Utils.getEnvironmentMandrelBuilderImage(script),
                ADDITIONAL_TIMEOUT: 720
            ]
        }
    )

    public static final Environment QUARKUS_MAIN = new Environment(
        name: 'QUARKUS_MAIN',
        optional: true,
        // cloudEnabled: true, // Can be activated once we are able to deploy artifacts to specific repository
        isActiveClosure: { script -> Utils.isEnvironmentQuarkusMainEnabled(script) },
        getDefaultEnvVarsClosure: { script -> 
            [ 
                QUARKUS_BRANCH: 'main' 
            ] 
        }
    )

    public static final Environment QUARKUS_BRANCH = new Environment(
        name: 'QUARKUS_BRANCH',
        optional: true,
        // cloudEnabled: true, // Can be activated once we are able to deploy artifacts to specific repository
        isActiveClosure: { script -> Utils.isEnvironmentQuarkusBranchEnabled(script) },
        getDefaultEnvVarsClosure: { script -> 
            [ 
                QUARKUS_BRANCH: Utils.getEnvironmentQuarkusBranchVersion(script) 
            ] 
        }
    )

    public static final Environment QUARKUS_LTS = new Environment(
        name: 'QUARKUS_LTS',
        optional: true,
        // cloudEnabled: true, // Can be activated once we are able to deploy artifacts to specific repository
        isActiveClosure: { script -> Utils.isEnvironmentQuarkusLTSEnabled(script) },
        getDefaultEnvVarsClosure: { script -> [ 
            QUARKUS_BRANCH: Utils.getEnvironmentQuarkusLTSVersion(script),
            BUILD_MVN_OPTS: '-Dproductized',
            DISABLE_PERSISTENCE: 'true',
        ] }
    )

    public static final Environment KOGITO_BDD = new Environment(
        name: 'KOGITO_BDD',
        optional: true,
        cloudEnabled: true,
        isActiveClosure: { script -> Utils.isEnvironmentRuntimesBDDEnabled(script) },
    )

    // Ecosystem env should only be executed in main branch
    // This makes sure all projects of Kogito ecosystem (aka Drools, Kogito, Optaplanner) are in sync
    public static final Environment ECOSYSTEM = new Environment(
        name: 'ECOSYSTEM',
        optional: true,
        isActiveClosure: { script -> Utils.isMainBranch(script) },
    )

    private static final Set<Environment> ENVIRONMENTS = [
        DEFAULT,
        SONARCLOUD,
        NATIVE,
        MANDREL,
        QUARKUS_MAIN,
        QUARKUS_BRANCH,
        QUARKUS_LTS,
        KOGITO_BDD
    ]

    static void register(Environment environment) {
        ENVIRONMENTS.add(environment)
    }

    static List getAllRegistered() {
        return new ArrayList(ENVIRONMENTS)
    }

    static Environment getByName(String name) {
        return ENVIRONMENTS.find { it.name == name }
    }

    static List<Environment> getActiveEnvironments(def script) {
        return getAllRegistered().findAll { environment -> environment.isActive(script) }
    }

    static List<Environment> getMandatoryActiveEnvironments(def script) {
        return getActiveEnvironments(script).findAll { !it.isOptional() }
    }

    static List<Environment> getOptionalActiveEnvironments(def script) {
        return getActiveEnvironments(script).findAll { it.isOptional() }
    }

}
