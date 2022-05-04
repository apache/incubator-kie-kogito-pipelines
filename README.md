# Kogito Pipelines

This repository contains some of the pipelines of Kogito project.

* [Kogito Pipelines](#kogito-pipelines)
* [Kogito Repositories](#kogito-repositories)
* [The different Kogito pipelines](#the-different-kogito-pipelines)
  * [Nightly & Release pipelines](#nightly--release-pipelines)
  * [Tools pipelines](#tools-pipelines)
  * [Repositories' specific pipelines](#repositories-specific-pipelines)
    * [Native checks](#native-checks)
    * [Quarkus check](#quarkus-check)
    * [Mandrel check](#mandrel-check)
    * [PR checks](#pr-checks)
      * [Jenkins artifacts PR checks](#jenkins-artifacts-pr-checks)
      * [GitHub Action checks](#github-action-checks)
    * [Sonar cloud](#sonar-cloud)
* [Configuration of pipelines](#configuration-of-pipelines)
  * [Jenkins](#jenkins)
    * [Jenkins jobs generation](#jenkins-jobs-generation)
  * [Zulip notifications](#zulip-notifications)
    * [Format](#format)

# Kogito Repositories

Apart from this repository, pipelines are also concerning those repositories:

* [drools](https://github.com/kiegroup/drools)
* [kogito-runtimes](https://github.com/kiegroup/kogito-runtimes)
* [optaplanner](https://github.com/kiegroup/optaplanner)
* [kogito-apps](https://github.com/kiegroup/kogito-apps)
* [kogito-examples](https://github.com/kiegroup/kogito-examples)
* [kogito-images](https://github.com/kiegroup/kogito-images)
* [kogito-operator](https://github.com/kiegroup/kogito-operator)
* [optaweb-employee-rostering](https://github.com/kiegroup/optaweb-employee-rostering)
* [optaweb-vehicle-routing](https://github.com/kiegroup/optaweb-vehicle-routing)
* [optaplanner-quickstarts](https://github.com/kiegroup/optaplanner-quickstarts)

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

## Repositories' specific pipelines

### Native checks

In some of the Kogito repositories, you can find native checks. If that is the case, the pipeline can be found in `.ci/jenkins/Jenkinsfile.native`.

### Quarkus check

A nightly check against Quarkus is done every night. Pipeline can be found in https://github.com/kiegroup/kogito-runtimes/blob/main/.ci/jenkins/Jenkinsfile.quarkus.

### Mandrel check

Quarkus and Native checks are also performed against Mandrel builder image (see config `mandrel.builder_image`).

### PR checks

PR checks are using the [build-chain](https://github.com/kiegroup/github-action-build-chain) for artifacts and its configuration can be found in [.ci](./.ci) folder.  
They are run on both Jenkins and GHA with some slight differences.

There is one check per downstream repository. This allows parallelization and more flexibility in restart of a specific downstream repo.

`kogito-images` is run only on Jenkins and is using its own `.ci/jenkins/Jenkinsfile`.

`kogito-operator` is run on another Jenkins and is using its own `.ci/jenkins/Jenkinsfile`.

#### Jenkins artifacts PR checks

The jobs can be found into the `{branch}/pullrequest` folder in Jenkins.  

Each repository contains the needed DSL configuration (can be found in `.ci/jenkins/dsl`) and will most of the time use the [KogitoTemplate](./dsl/seed/src/main/java/../groovy/org/kie/jenkins/jobdsl/templates/KogitoJobTemplate.groovy) method `createMultijobPRJobs`.  
This will generate all the needed PR checks and make use of the [Jenkinsfile.buildchain](./.ci/jenkins/Jenkinsfile.buildchain) file.

Jenkins PR checks are of 3 different types:

* Simple build&test (automatic)  
  Regular testing
* Native build&test (optional, can be launched with comment `jenkins run native`)  
  Test all native parts of the repository
* Mandrel build&test (optional, can be launched with comment `jenkins run mandrel`)  
  Test against Mandrel builder image
* Quarkus main build&test (optional, can be launched with comment `jenkins run quarkus main`)  
  Test against quarkus main version
* Quarkus branch build&test (optional, can be launched with comment `jenkins run quarkus branch`)  
  Test against quarkus branch, corresponding to current used Quarkus released version

#### GitHub Action checks

Each repository has a different yaml files in `.github/workflows` folder to configure the workflow.

We are additionally using [`composite actions`](https://docs.github.com/en/actions/creating-actions/creating-a-composite-action) to centralized most common steps used by the different Kogito repositories' jobs. You can check the different kind of composite actions we have available at [`.ci/actions` folder](https://github.com/kiegroup/kogito-pipelines/tree/main/.ci/actions).

After the build, test results are parsed and logged using the [`action-surefire-report`](https://github.com/ScaCap/action-surefire-report) action.

### Sonar cloud

NOTE: test coverage analysis is executed only by **Jenkins PR simple build&test** and not while using GitHub action.

# Configuration of pipelines

## Jenkins

All pipelines can be found in [kogito Jenkins folder](https://eng-jenkins-csb-business-automation.apps.ocp-c1.prod.psi.redhat.com/job/KIE/job/kogito).

### Jenkins jobs generation

More information can be found [here](./docs/jenkins.md).

## Zulip notifications

Any message / error is sent to [kogito-ci](https://kie.zulipchat.com/#narrow/stream/236603-kogito-ci) stream.

### Format

    [branch] Project
