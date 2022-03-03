/**
 * This job is a workaround for the nightly-meta-pipeline (wich was disabled) and will be removed once the UMB messages are sent again<br>
 * Instead UMB message a simple CRON job is applied.
 */

import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_TOOL
def AGENT_LABEL="kie-rhel && kie-mem512m"

def NEXT_PRODUCT_VERSION=Constants.NEXT_PROD_VERSION
def NEXT_PRODUCT_BRANCH='main'
def NEXT_RHBA_VERSION_PREFIX=Constants.RHBA_VERSION_PREFIX

def CURRENT_PRODUCT_VERSION=Constants.CURRENT_PROD_VERSION
def CURRENT_PRODUCT_BRANCH='7.59.x'
def CURRENT_RHBA_VERSION_PREFIX='7.59.1.redhat-'

def KOGITO_NEXT_PRODUCT_VERSION=NEXT_PRODUCT_VERSION
def KOGITO_NEXT_PRODUCT_BRANCH=NEXT_PRODUCT_BRANCH

def KOGITO_CURRENT_PRODUCT_VERSION='1.11.0'
def KOGITO_CURRENT_PRODUCT_BRANCH='1.11.x'

def KOGITO_1_13_PRODUCT_VERSION='1.13.0'
def KOGITO_1_13_PRODUCT_BRANCH='1.13.x'

def OPTAPLANNER_NEXT_PRODUCT_VERSION=NEXT_PRODUCT_VERSION
def OPTAPLANNER_CURRENT_PRODUCT_VERSION='8.11.0'
def OPTAPLANNER_1_13_PRODUCT_VERSION='8.13.0'
def DROOLS_1_13_PRODUCT_VERSION='8.13.0'
// def DROOLS_CURRENT_PRODUCT_VERSION=OPTAPLANNER_CURRENT_PRODUCT_VERSION // Should be uncommented once Current is set for RHPAM 7.13.0


def metaJob="""
pipeline{
    agent {
        label "$AGENT_LABEL"
    } 
    tools {
        jdk "$javadk"        
    }   
    // IMPORTANT: In case you trigger a new branch here, please create the same branch on build-configuration project
    
    stages {
        stage('trigger RHBA nightly job ${NEXT_PRODUCT_BRANCH}') {
            steps {
                build job: 'rhba.nightly/${NEXT_PRODUCT_BRANCH}', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'KIE_GROUP_DEPLOYMENT_REPO_URL', value: 'https://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8443/nexus/service/local/repositories/scratch-release-rhba-${NEXT_PRODUCT_BRANCH}/content-compressed'],
                        [\$class: 'StringParameterValue', name: 'UMB_VERSION', value: '${NEXT_PRODUCT_BRANCH}'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: "${NEXT_PRODUCT_VERSION}"],
                        [\$class: 'StringParameterValue', name: 'CONFIG_BRANCH', value: "\${env.DEFAULT_CONFIG_BRANCH}"],
                        [\$class: 'BooleanParameterValue', name: 'SKIP_TESTS', value: true]
                ]
            }
        }

        // Kogito prod nightlies
        stage('trigger KOGITO nightly job ${KOGITO_NEXT_PRODUCT_BRANCH}') {
            steps {
                build job: 'kogito.nightly/${KOGITO_NEXT_PRODUCT_BRANCH}', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'RHBA_MAVEN_REPO_URL', value: 'http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/rhba-${NEXT_PRODUCT_BRANCH}-nightly-with-upstream'],
                        [\$class: 'StringParameterValue', name: 'RHBA_VERSION_PREFIX', value: '${NEXT_RHBA_VERSION_PREFIX}'],
                        [\$class: 'StringParameterValue', name: 'KOGITO_DEPLOYMENT_REPO_URL', value: 'https://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8443/nexus/service/local/repositories/scratch-release-kogito-${KOGITO_NEXT_PRODUCT_BRANCH}/content-compressed'],
                        [\$class: 'StringParameterValue', name: 'UMB_VERSION', value: '${KOGITO_NEXT_PRODUCT_BRANCH}'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: '${KOGITO_NEXT_PRODUCT_VERSION}'],
                        [\$class: 'StringParameterValue', name: 'DROOLS_PRODUCT_VERSION', value: '${NEXT_PRODUCT_VERSION}'],
                        [\$class: 'StringParameterValue', name: 'OPTAPLANNER_PRODUCT_VERSION', value: '${NEXT_PRODUCT_VERSION}'],
                        [\$class: 'StringParameterValue', name: 'CONFIG_BRANCH', value: "\${env.DEFAULT_CONFIG_BRANCH}"],
                        [\$class: 'BooleanParameterValue', name: 'SKIP_TESTS', value: true]
                ]
            }
        }
        
        stage('trigger RHBA nightly job ${CURRENT_PRODUCT_BRANCH}') {
            steps {
                build job: 'rhba.nightly/${CURRENT_PRODUCT_BRANCH}', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'KIE_GROUP_DEPLOYMENT_REPO_URL', value: 'https://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8443/nexus/service/local/repositories/scratch-release-rhba-${getNexusFromVersion(CURRENT_PRODUCT_VERSION)}/content-compressed'],
                        [\$class: 'StringParameterValue', name: 'UMB_VERSION', value: '${getUMBFromVersion(CURRENT_PRODUCT_VERSION)}'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: '${CURRENT_PRODUCT_VERSION}'],
                        [\$class: 'StringParameterValue', name: 'CONFIG_BRANCH', value: '${CURRENT_PRODUCT_BRANCH}'],
                        [\$class: 'BooleanParameterValue', name: 'SKIP_TESTS', value: true]
                ]
            }
        }

        // Kogito prod nightlies
        stage('trigger KOGITO nightly job ${KOGITO_CURRENT_PRODUCT_VERSION}') {
            steps {
                build job: 'kogito.nightly/${KOGITO_CURRENT_PRODUCT_BRANCH}', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'RHBA_MAVEN_REPO_URL', value: 'http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/rhba-${getNexusFromVersion(CURRENT_PRODUCT_VERSION)}-nightly-with-upstream'],
                        [\$class: 'StringParameterValue', name: 'RHBA_VERSION_PREFIX', value: '${CURRENT_RHBA_VERSION_PREFIX}'],
                        [\$class: 'StringParameterValue', name: 'KOGITO_DEPLOYMENT_REPO_URL', value: 'https://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8443/nexus/service/local/repositories/scratch-release-kogito-${getNexusFromVersion(KOGITO_CURRENT_PRODUCT_VERSION)}/content-compressed'],
                        [\$class: 'StringParameterValue', name: 'UMB_VERSION', value: '${getUMBFromVersion(KOGITO_CURRENT_PRODUCT_VERSION)}'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: '${KOGITO_CURRENT_PRODUCT_VERSION}'],
                        [\$class: 'StringParameterValue', name: 'OPTAPLANNER_PRODUCT_VERSION', value: '${OPTAPLANNER_CURRENT_PRODUCT_VERSION}'],
                        [\$class: 'StringParameterValue', name: 'CONFIG_BRANCH', value: '${CURRENT_PRODUCT_BRANCH}'],
                        [\$class: 'StringParameterValue', name: 'RHBA_RELEASE_VERSION', value: '${getNexusFromVersion(CURRENT_PRODUCT_VERSION)}'],
                        [\$class: 'BooleanParameterValue', name: 'SKIP_TESTS', value: true]
                ]
            }
        }

        // Kogito prod nightlies for 1.13
        stage('trigger KOGITO nightly job ${KOGITO_1_13_PRODUCT_VERSION}') {
            steps {
                build job: 'kogito.nightly/${KOGITO_1_13_PRODUCT_BRANCH}', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'RHBA_MAVEN_REPO_URL', value: 'http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/rhba-${NEXT_PRODUCT_BRANCH}-nightly-with-upstream'],
                        [\$class: 'StringParameterValue', name: 'RHBA_VERSION_PREFIX', value: '${NEXT_RHBA_VERSION_PREFIX}'],
                        [\$class: 'StringParameterValue', name: 'KOGITO_DEPLOYMENT_REPO_URL', value: 'https://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8443/nexus/service/local/repositories/scratch-release-kogito-${getNexusFromVersion(KOGITO_1_13_PRODUCT_BRANCH)}/content-compressed'],
                        [\$class: 'StringParameterValue', name: 'UMB_VERSION', value: '${getUMBFromVersion(KOGITO_1_13_PRODUCT_VERSION)}'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: '${KOGITO_1_13_PRODUCT_VERSION}'],
                        [\$class: 'StringParameterValue', name: 'OPTAPLANNER_PRODUCT_VERSION', value: '${OPTAPLANNER_1_13_PRODUCT_VERSION}'],
                        [\$class: 'StringParameterValue', name: 'CONFIG_BRANCH', value: '${CURRENT_PRODUCT_BRANCH}'],
                        [\$class: 'BooleanParameterValue', name: 'SKIP_TESTS', value: true]
                ]
            }
        }

        // Kogito-tooling prod nightlies
        /* stage('trigger KOGITO-TOOLING nightly job ${NEXT_PRODUCT_BRANCH}') {
            steps {
                build job: 'kogito-tooling.nightly/main', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'DEPLOYMENT_REPO_URL', value: 'https://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8443/nexus/service/local/repositories/scratch-release-rhba-${NEXT_PRODUCT_BRANCH}/content-compressed'],
                        [\$class: 'StringParameterValue', name: 'UMB_VERSION', value: '${NEXT_PRODUCT_BRANCH}'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: '${NEXT_PRODUCT_VERSION}'],
                        [\$class: 'BooleanParameterValue', name: 'SKIP_TESTS', value: true]
                ]
            }
        } */

        // Kogito-tooling prod nightlies
        /* stage('trigger KOGITO-TOOLING nightly job 0.13.0-prerelease') {
            steps {
                build job: 'kogito-tooling.nightly/0.13.0-prerelease', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'DEPLOYMENT_REPO_URL', value: 'https://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8443/nexus/service/local/repositories/scratch-release-rhba-${getNexusFromVersion(NEXT_PRODUCT_VERSION)}/content-compressed'],
                        [\$class: 'StringParameterValue', name: 'UMB_VERSION', value: '${getUMBFromVersion(NEXT_PRODUCT_VERSION)}'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: '${NEXT_PRODUCT_VERSION}'],
                        [\$class: 'BooleanParameterValue', name: 'SKIP_TESTS', value: true]
                ]
            }
        }
        */
    }
}
"""
// creates folder if is not existing
def folderPath="PROD"
folder(folderPath)

pipelineJob("${folderPath}/cron-meta-nightly-pipeline") {

    description("This job is a workaround for the nightly-meta-pipeline (wich was disabled) and will be removed once the UMB messages are sent again<br>\n" +
            "Instead UMB message a simple CRON job is applied.")

    parameters {
        wHideParameterDefinition {
            name('AGENT_LABEL')
            defaultValue("${AGENT_LABEL}")
            description('name of machine where to run this job')
        }
        wHideParameterDefinition {
            name('javadk')
            defaultValue("${javadk}")
            description('version of jdk')
        }
    }

    logRotator {
        numToKeep(5)
    }

    properties {
        pipelineTriggers {
            triggers {
                cron{
                    spec("H 15 * * *")
                }
            }
        }
    }

    definition {
        cps {
            script("${metaJob}")
            sandbox()
        }
    }
}

String getUMBFromVersion(def version) {
    def matcher = version =~ /(\d*)\.(\d*)\.?/
    return "${matcher[0][1]}${matcher[0][2]}"
}

String getNexusFromVersion(def version) {
    def matcher = version =~ /(\d*)\.(\d*)\.?/
    return "${matcher[0][1]}.${matcher[0][2]}"
}
