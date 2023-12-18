# Drools 8 / Kogito / OptaPlanner 8 pipelines

Here is a small overview of current pipelines existing around Kogito project.

## Which projects ?

- Drools
  - https://github.com/apache/incubator-kie-drools
  - https://github.com/kiegroup/drools-website
  - https://github.com/apache/incubator-kie-benchmarks
  - https://github.com/kiegroup/kie-pmml-integration
- Kogito
  - https://github.com/apache/incubator-kie-kogito-pipelines
  - https://github.com/apache/incubator-kie-kogito-runtimes
  - https://github.com/apache/incubator-kie-kogito-apps
  - https://github.com/apache/incubator-kie-kogito-examples
  - https://github.com/apache/incubator-kie-kogito-images
  - https://github.com/apache/incubator-kie-kogito-operator
  - https://github.com/apache/incubator-kie-kogito-docs
  - https://github.com/apache/incubator-kie-kogito-serverless-operator
  - https://github.com/apache/incubator-kie-docs
  - https://github.com/kiegroup/kie-tools
- OptaPlanner
  - https://github.com/apache/incubator-kie-optaplanner
  - https://github.com/apache/incubator-kie-optaplanner-quickstarts
  - https://github.com/kiegroup/optaplanner-website

### Projects' dependencies

- Kogito snapshot depends on Drools snapshot
  They remain extremely coupled and some pipelines will need first to run Drools before Kogito, like the `Prepare release branch` and `Release` pipelines
- OptaPlanner depends a Final version of Drools.

All 3 projects can still be released

### Kogito specificities

Kogito contains both Artifacts and Cloud repositories. Those are "decoupled", which means there are:

- 2 main release jobs, one for Artifacts and one for Cloud (but artifacts can call cloud)
- 2 main default nightly jobs, one for Artifacts and one for Cloud (but artifacts can call cloud)
- 2 main setup branch jobs, one for Artifacts and one for Cloud (but artifacts can call cloud)

## Jenkins 

All current Jenkins pipelines can be found in https://ci-builds.apache.org/job/KIE

Each project has there its own folder (`drools`, `kogito`, `optaplanner`) and in there its own main seed job (see after).
### Seed generation

For more information on the seed generation, please look at the [jenkins](./jenkins.md) documentation.

Main information to remember:

- One main seed job will start the whole process and generate all jobs, for all defined branches.  
  In that process, it will create global root jobs as well as one folder per branch (defined into the `main.yaml` config file).  
  In each branch, it is created a branch seed job.  
  Logic of the seed job can be found in [here](../dsl/seed/jenkinsfiles/Jenkinsfile.seed.main)  
  Definition of the main seed job can found [here](../dsl/seed/jobs/seed_job_main.groovy)
- Each branch seed job will generate the jobs, based on the `.ci/jenkins/dsl/jobs.groovy` file, for each of the defined repository/branch.  
  For each branch, the information needed is defined into the `branch.yaml` file, being environments or repositories or credentials ...  
  Logic of the seed job can be found in [here](../dsl/seed/jenkinsfiles/Jenkinsfile.seed.branch)  
  Definition of a branch seed job can found [here](../dsl/seed/jobs/seed_job_branch.groovy)
- When branching (`prepare-release-branch` job), it should process 3 things:
  - Create release branches for all concerned repositories
  - Create config branch for the config files repository
  - Create seed branch for the seed library (`kogito-pipelines` repository)

### Jenkins jobs

### Job environments

Environments can be found in the branch seed configuration.

Usually you find those:

- default
  or the "empty" environment
- native
  Execute build with native maven commands
- quarkus-branch
  Check the project against the latest updates from the current quarkus branch used
- quarkus-main
  Check the project against the latest updates from quarkus main branch (forward compatiblity)
- quarkus-3
  Migrate repositories to Quarkus 3
- quarkus-lts
  Check the project against the latest updates from quarkus LTS branch (backward compatiblity)
- native-lts
  Check the project against the latest updates from quarkus LTS branch (backward compatiblity) in native mode

#### Nightlies

**Main nightly**

or default nightly.

This one building, testing and deploying artifacts/images.

**Environment nightlies**

Those jobs are only for artifacts' repositories.

Environment jobs are defined in each concerned repositories. They use the environment definition and make a simple build.  
Those jobs are making use of the build-chain via the [build-chain jenkinsfile](../dsl/seed/jenkinsfiles/Jenkinsfile.buildchain).

The run of those builds use the `.ci/environments` folder of each repository.  
This allows to have some specific updates for the build.
Updates are usually in a patch folder. But some `before.sh` and `after.sh` scripts can allow more changes.
For example, Quarkus 3 flows uses OpenRewrite + patches to do the migration, as the changes are quite big.

Integration jobs are creating specific branches before perfoming the build. Branch name pattern is `{BASE_BRANCH}-integration-{ENVIRONMENT_NAME}`.

#### PRs

PR jobs are making use of the build-chain via the [build-chain jenkinsfile](../dsl/seed/jenkinsfiles/Jenkinsfile.buildchain).
The environment variables are determining what and how it should be launched for each environments.

Like the nightlies, the PR check use the environments scripts if needed and existing.

#### Setup branch

Those jobs are used:

- when a release branch is created to update the release branch version and the main branch version
- after a release is done, so we can update the release branch to the next snapshot

Like the default nightly, there is a main pipeline which is calling the other jobs from the folder.

#### Release

Contains all release jobs. 

Like the default nightly and setup branch processes, there is a main release job which is calling the different deploy/promote jobs.

For more information, you can also have a look to the [release page](./release%20pipeline.md).

There is also a [release checklist](./release%20checklist.md).

#### Tools

Useful tools for the pipelines, like updating an external dependency version, cleaning images ...

### Quarkus Platform nightly

As we need to deploy a specific Quarkus Platform with our snapshot artifacts, there is a nightly existing, which is launched by the main nightly.

### Quarkus 3 jobs

The Quarkus 3 flow is a bit specific as we are preparing for releasing versions in parallel of Quarkus 2.x version.
We end up with 2 streams:

- Drools8/Kogito/OptaPlanner8
- Drools9/Kogito2/OptaPlanner9

#### Quarkus 3 nightly integration jobs

This is done in the `quarkus-3` environment nightly job (that can be found in `nightly.quarkus-3` folder).

This environment has always a `0001_before_sh.patch` which is the result of the `before.sh` script, containing OpenRewrite automation. All other patches are manually maintained.

When the nightly job is in failure, it usually means that the patches could not be correctly applied. Check the logs.
And `UNSTABLE` build, means only test failures.

The nightlies are creating specific branches:

- Drools 9.x
- Kogito 2.x
- OptaPlanner 9.x

**NOTE: The Quarkus 3 integration jobs are not running any testing. They are only performing the code migration. Tests are performed in the nightly jobs of the created branches**

#### Quarkus 3 branches

**DSL config files**

Main Config file for Quarkus 3 are can be found on separate from code:
- https://github.com/apache/incubator-kie-drools/tree/9.x-dsl-config
- https://github.com/apache/incubator-kie-kogito-pipelines/tree/2.x-dsl-config
- https://github.com/apache/incubator-kie-optaplanner/tree/9.x-dsl-config

And jobs would be found in different Jenkins folders:
- https://ci-builds.apache.org/job/KIE/job/drools-9.x/
- https://ci-builds.apache.org/job/KIE/job/kogito-2.x/
- https://ci-builds.apache.org/job/KIE/job/optaplanner-9.x/

NOTE: We keep separate configurations between the 2 streams because it is easier to manage. Jobs configuration can then be read. Compare to "Stream 8/1", some environments are disabled for now.

## GitHub Actions

Historically, GHA was used as a backup of Jenkins, at the time when Jenkins was very unstable.  
Since then, we tried to keep both sync.  
Also GHA is used to provide external users (outside Red Hat) some feedbacks in case anything fails.

Unlike Jenkins, there is no DSL for GHA but it means job run is much more dynamic. Any change can be applied automatically.

And we only have PR checks with GHA.

### PR checks

See the different workflows. They mostly use the build-chain for artifacts's repositories. For cloud, it is to run simple and quick jobs.

## How-tos ?

### Update Quarkus version

1) Create an issue on [kie-issues](https://github.com/apache/incubator-kie-issues)
2) Launch `main/tools/update-quarkus-all` job with 
   * [Drools](https://ci-builds.apache.org/job/KIE/job/drools/job/main/job/tools/job/update-quarkus-all/)
   * [Kogito](https://ci-builds.apache.org/job/KIE/job/kogito/job/main/job/tools/job/update-quarkus-all/)
   * [OptaPlanner](https://ci-builds.apache.org/job/KIE/job/optaplanner/job/main/job/tools/job/update-quarkus-all/)
3) Wait for the PRs to arrive on [kogito-ci](https://kie.zulipchat.com/#narrow/stream/236603-kogito-ci) and [optaplanner-ci](https://kie.zulipchat.com/#narrow/stream/354864-optaplanner-ci) streams
4) Review the changes.
   **Make sure there is no downgrade of dependency version !!!!**

