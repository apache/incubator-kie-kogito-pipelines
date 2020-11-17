# Kogito Pipelines

This repository contains some of the pipelines of Kogito project.  

* [Kogito Pipelines](#kogito-pipelines)
  * [Kogito Repositories](#kogito-repositories)
  * [The different Kogito pipelines](#the-different-kogito-pipelines)
    * [Nightly & Release pipelines](#nightly--release-pipelines)
    * [Tools pipelines](#tools-pipelines)
    * [Repositories' specific pipelines](#repositories-specific-pipelines)
      * [Kogito Runtimes' other jobs](#kogito-runtimes-other-jobs)
      * [PR checks](#pr-checks)
      * [Sonar cloud](#sonar-cloud)
  * [Configuration of pipelines](#configuration-of-pipelines)
    * [Jenkins](#jenkins)
      * [Jenkins jobs generation](#jenkins-jobs-generation)
    * [Zulip notifications](#zulip-notifications)
      * [Format](#format)

## Kogito Repositories

Apart from this repository, pipelines are also concerning those repositories:

* [kogito-runtimes](https://github.com/kiegroup/kogito-runtimes)
* [kogito-apps](https://github.com/kiegroup/kogito-apps)
* [optaplanner](https://github.com/kiegroup/optaplanner)
* [kogito-examples](https://github.com/kiegroup/kogito-examples)
* [kogito-images](https://github.com/kiegroup/kogito-images)
* [kogito-cloud-operator](https://github.com/kiegroup/kogito-cloud-operator)

## The different Kogito pipelines

### Nightly & Release pipelines

Kogito has 2 main pipelines:

* [Nightly pipeline](./Jenkinsfile.nightly)  
  is a multibranch pipeline which is run daily on each active branch
* [Release pipeline](./Jenkinsfile.release)  
  is a on-demand single pipeline job

More information can be found [here](./docs/nightly_and_release.md).

### Tools pipelines

This is a set of cleanup utils jobs.

### Repositories' specific pipelines

#### Kogito Runtimes' other jobs

Those jobs can be found into the `kogito-runtimes` repository.  
They are daily run jobs:

* [kogito-native](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.native)  
  Perform a daily native build&test of runtimes.
* [kogito-quarkus](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.quarkus)  
  Perform a daily check of runtimes against latest snapshot of Quarkus.
* [kogito-drools-snapshot](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.drools)  
  Perform a daily check of runtimes against latest snapshot of Drools.

#### PR checks

Each repository has a `Jenkinsfile` for the PR check.

The jobs are located into the `pullrequest` folder in Jenkins.  
Only the Operator PR check is not yet in this folder as it is still on another Jenkins

#### Sonar cloud

// TODO

## Configuration of pipelines

### Jenkins

All pipelines can be found in [kogito Jenkins folder](https://rhba-jenkins.rhev-ci-vms.eng.rdu2.redhat.com/job/KIE/job/kogito).

#### Jenkins jobs generation

More information can be found [here](./docs/jenkins.md).

### Zulip notifications

Any message / error is sent to [kogito-ci](https://kie.zulipchat.com/#narrow/stream/236603-kogito-ci) stream.

#### Format

    [branch][d for daily if occurs] Pipeline name
