# Kogito Pipelines

This repository contains some of the pipelines of Kogito project.

- [Kogito Pipelines](#kogito-pipelines)
- [Kogito Repositories](#kogito-repositories)
- [The different Kogito pipelines](#the-different-kogito-pipelines)
  - [Nightly \& Release pipelines](#nightly--release-pipelines)
  - [Tools pipelines](#tools-pipelines)
  - [Repositories' specific nightlies (environments)](#repositories-specific-nightlies-environments)
      - [Run against integration branch (POC)](#run-against-integration-branch-poc)
    - [PR checks](#pr-checks)
      - [Jenkins artifacts PR checks](#jenkins-artifacts-pr-checks)
      - [GitHub Action checks](#github-action-checks)
    - [Sonar cloud](#sonar-cloud)
- [Configuration of pipelines](#configuration-of-pipelines)
  - [Source Code](#source-code)
    - [Pipelines](#pipelines)
    - [Shared Pipeline libraries](#shared-pipeline-libraries)
  - [Jenkins](#jenkins)
    - [Jenkins KIE folder](#jenkins-kie-folder)
    - [Jenkins jobs generation](#jenkins-jobs-generation)
  - [Zulip notifications](#zulip-notifications)
    - [Format](#format)
- [Contributing / Opening issues](#contributing--opening-issues)

# Kogito Repositories

Apart from this repository, pipelines are also concerning those repositories:

* [kogito-runtimes](https://github.com/apache/incubator-kie-kogito-runtimes)
* [kogito-apps](https://github.com/apache/incubator-kie-kogito-apps)
* [kogito-examples](https://github.com/apache/incubator-kie-kogito-examples)
* [kogito-images](https://github.com/apache/incubator-kie-kogito-images)
* [kogito-operator](https://github.com/apache/incubator-kie-kogito-operator)
* [kie-tools](https://github.com/apache/incubator-kie-tools)

# The different Kogito pipelines

## Nightly & Release pipelines

Kogito has 2 main pipelines:

* [Nightly pipeline](./.ci/jenkins/Jenkinsfile.nightly)  
  is a multibranch pipeline which is run daily on each active branch
* [Release pipeline](./.ci/jenkins/Jenkinsfile.release)  
  is a on-demand single pipeline job

More information can be found [here](./docs/nightly_and_release.md).

## Tools pipelines

This is a set of cleanup utils jobs.

## Repositories' specific nightlies (environments)

The jenkinsfile run in those environments can be found in each repository, at path `.ci/jenkins/Jenkinsfile.specific_nightly`.

The different environments can also be found into the [dsl branch config](.ci/jenkins/config/branch.yaml) file.  
Here is some explanation:

- `native`  
  Run the checks with native mode enabled and GraalVM
- `mandrel`  
  Run the checks with native mode enabled and [Mandrel](https://github.com/graalvm/mandrel)
- `quarkus branch`  
  Run the checks against current Quarkus released branch
- `quarkus main`  
  Run the checks against Quarkus main branch
- `quarkus lts`  
  Run the checks against Quarkus LTS branch
- `mandrel lts`  
  Run the checks against Quarkus LTS branch, with native mode enabled and [Mandrel](https://github.com/graalvm/mandrel)

#### Run against integration branch (POC)

As an experimentation, due to many conflicts with `quarkus lts` and `quarkus main` branches, it has been decided to try to use an integration branch for the those nightly checks, in order to be able to resolve conflicts that my occur, but which cannot be pushed directly to the tested branch.

The integration branch nightly will:

- Checkout the current integration branch
- Try to merge the last changes from the tested branch (with `-no-ff`)
  - If any conflict, raise an error and developers will have to correct the merge conflict on the integration branch
  - If no conflict, run the tests !


Those new nightly jobs are currently set up as new jobs and won't replace the current ones without the integration branch.  
Once we decide, we may remove the "old" jobs and use only the integration branch.

### PR checks

PR checks are using the [build-chain](https://github.com/kiegroup/github-action-build-chain) for artifacts and its configuration can be found in [.ci](./.ci) folder.  
They are run on both Jenkins and GHA with some slight differences.

There is one check per downstream repository. This allows parallelization and more flexibility in restart of a specific downstream repo.

`kogito-images` is run only on Jenkins and is using its own `.ci/jenkins/Jenkinsfile`.

`kogito-operator` is run on another Jenkins and is using its own `.ci/jenkins/Jenkinsfile`.

**NOTE:** PR checks are also available for the different environments listed above. Please read the PR template to know which one are available for a specific repository.

#### Jenkins artifacts PR checks

The jobs can be found into the `{branch}/pullrequest` folder in Jenkins.  

Each repository contains the needed DSL configuration (can be found in `.ci/jenkins/dsl`) and will most of the time use the [KogitoTemplate](./dsl/seed/src/main/java/../groovy/org/kie/jenkins/jobdsl/templates/KogitoJobTemplate.groovy) method `createMultijobPRJobs`.  
This will generate all the needed PR checks and make use of the [Jenkinsfile.buildchain](./dsl/seed/jenkinsfiles/Jenkinsfile.buildchain) file.

Jenkins PR checks are of 3 different types:

* Simple build&test (automatic)  
  Regular testing
* Native build&test (optional, can be launched with comment `jenkins run native`)  
  Test all native parts of the repository
* Mandrel build&test (optional, can be launched with comment `jenkins run mandrel`)  
  Test against Mandrel builder image
* Quarkus main build&test (optional, can be launched with comment `jenkins run quarkus-main`)  
  Test against quarkus main version
* Quarkus branch build&test (optional, can be launched with comment `jenkins run quarkus-branch`)  
  Test against quarkus branch, corresponding to current used Quarkus released version
* Quarkus lts build&test (optional, can be launched with comment `jenkins run quarkus-lts`)  
  Test against quarkus branch, corresponding to current used Quarkus LTS version

#### GitHub Action checks

Each repository has a different yaml files in `.github/workflows` folder to configure the workflow.

We are additionally using [`composite actions`](https://docs.github.com/en/actions/creating-actions/creating-a-composite-action) to centralized most common steps used by the different Kogito repositories' jobs. You can check the different kind of composite actions we have available at [`.ci/actions` folder](https://github.com/apache/incubator-kie-kogito-pipelines/tree/main/.ci/actions).

After the build, test results are parsed and logged using the [`action-surefire-report`](https://github.com/ScaCap/action-surefire-report) action.

### Sonar cloud

NOTE: test coverage analysis is executed only by **Jenkins PR simple build&test** and not while using GitHub action.

# Configuration of pipelines

## Source Code
*Note: Creating separate readme.md documenting how-tos and best practices for implementing pipelines might be useful*

### Pipelines
In this repository two types of pipelines can be found:
- **Kogito pipelines** (obviously) - located in the [.ci/jenkins](./.ci/jenkins) folder
- **Seed jobs library** - see [Jenkins documentation](./docs/jenkins.md)

### Shared pipeline libraries
Apart from these pipelines, the `jenkins-pipeline-shared-libraries` are also stored in this repository. Functions and classes contained in these libraries can be freely used in all pipelines located under [KIE Jenkins folder](https://ci-builds.apache.org/job/KIE). Just include correct import and annotation in your Jenkinsfile:
```
import org.jenkinsci.plugins.workflow.libs.Library

@Library('jenkins-pipeline-shared-libraries')_
```
For more details please see our [Jenkins pipelines shared libraries documentation](./jenkins-pipeline-shared-libraries/README.md) and [Jenkins.io documentation](https://www.jenkins.io/doc/book/pipeline/shared-libraries/)

## Jenkins

### Jenkins KIE folder

All KIE jobs (pipelines) can be found  in [KIE Jenkins folder](https://ci-builds.apache.org/job/KIE)<br />
For this folder and all its descendants there is several useful things set at this folder level:<br />
- **Pipeline library** - accessible in pipelines under name `jenkins-pipeline-shared-libraries` it gives access to some useful functions used throughout various KIE pipelines. More details can be found in our [Jenkins pipeline shared libraries documentation](./jenkins-pipeline-shared-libraries/README.md) and in the [previous chapter](#source-code)
- **Environment Variables** - Environment variables set here are inherited by all the folders and jobs located in the [KIE folder](https://ci-builds.apache.org/job/KIE) tree in Jenkins. However, they can be overridden or extended. You can modify the variables by clicking `Configure` in the left menu (if you have necessary permissions). Currently present Environment Variables are:
  - **FAULTY_NODES** - Comma separated list of Jenkins execution nodes that are faulty in some way and cause KIE jobs to fail. This variable is expected by the *pipeline-library* function `getLabel(String label)`, which extends desired `label` by expression that ensures avoiding these faulty nodes. This way we can increase durability of KIE automation by the time the Apache CI team fixes the issue with faulty node. 


All pipelines from this repository can be found in [kogito Jenkins folder](https://ci-builds.apache.org/job/KIE/job/kogito/).

### Jenkins jobs generation

More information can be found [here](./docs/jenkins.md).

## Zulip notifications

Any message / error is sent to [kogito-ci](https://kie.zulipchat.com/#narrow/stream/236603-kogito-ci) stream.

### Format

    [branch] Project
    
# Contributing / Opening issues

The work on the pipelines has been separated into different epics:

- [KOGITO-8034](https://issues.redhat.com/browse/KOGITO-8034) Kogito Pipelines maintenance / small changes
- [KOGITO-8037](https://issues.redhat.com/browse/KOGITO-8037) Kogito Pipelines improvements for the release pipeline
- [KOGITO-8038](https://issues.redhat.com/browse/KOGITO-8038) Kogito Pipelines improvements for the PR and nightly checks
- [KOGITO-8036](https://issues.redhat.com/browse/KOGITO-8036) Kogito Pipelines infrastructure tasks
- [KOGITO-8035](https://issues.redhat.com/browse/KOGITO-8035) Kogito Pipelines pipelines ideas / nice to have

Please use one of those epics if you create any issue.
