#!/usr/bin/env bash
set -eo pipefail

# ###################################################
# # If you need to run locally this script, please uncomment the needed lines
# # and run the script of only the value option (-v)

# Drools
# MAVEN_MODULES=('drools-build-parent')
# MAVEN_PROPERTIES=('version.io.quarkus')
# GRADLE_REGEX=
# REMOTE_POMS=('io.quarkus:quarkus-bom')

# # Kogito Runtimes
# MAVEN_MODULES=('kogito-dependencies-bom' 'kogito-build-parent' 'kogito-quarkus-bom' 'kogito-build-no-bom-parent')
# MAVEN_PROPERTIES=('version.io.quarkus' 'version.io.quarkus.quarkus-test-maven')
# GRADLE_REGEX=
# REMOTE_POMS=('io.quarkus:quarkus-bom')

# # Optaplanner
# MAVEN_MODULES=('optaplanner-build-parent')
# MAVEN_PROPERTIES=('version.io.quarkus')
# GRADLE_REGEX=
# REMOTE_POMS=('io.quarkus:quarkus-bom')

# # Kogito Examples
# MAVEN_MODULES=
# MAVEN_PROPERTIES=('quarkus-plugin.version' 'quarkus.platform.version')
# GRADLE_REGEX=
# REMOTE_POMS=

# # OptaPlanner Quickstarts
# MAVEN_MODULES=
# MAVEN_PROPERTIES=('version.io.quarkus')
# GRADLE_REGEX=('id "io.quarkus" version' 'def quarkusVersion =')
# REMOTE_POMS=
# ###################################################

script_dir_path=$(cd `dirname "${BASH_SOURCE[0]}"`; pwd -P)

DEFAULT_PROJECT=kogito-runtimes

usage() {
    echo 'Usage: update-project-dependencies.sh -v $NEW_VALUE [-m MAVEN_MODULE]* [-p MAVEN_PROPERTY]* [-g GRADLE_REGEX]* [-c COMPARE_REMOTE_POMS]'
    echo
    echo 'Options:'
    echo '  -v $NEW_VALUE             New value to set.'
    echo '  -m $MAVEN_MODULE          Maven module to update.'
    echo '  -p $MAVEN_PROPERTY        Maven property to set to the given value.'
    echo '  -g $GRADLE_REGEX          Gradle line to update as a regex. Anything behing the given regex will be replaced by the given value.'
    echo '  -c $COMPARE_REMOTE_POMS   Remote poms to use to compare and update dependencies.'
    echo
    echo 'Examples:'
    echo '  #  - Update Maven dependecy `version.io.quarkus` to Quarkus 2.0.0.Final on all modules '
    echo '  sh update-project-dependencies.sh -q 2.0.0.Final -p version.io.quarkus'
    echo 
    echo '  #  - Update Maven dependecy `version.io.quarkus` on module `kogito-quarkus-bom` to Quarkus 2.0.0.Final '
    echo '  sh update-project-dependencies.sh -q 2.0.0.Final -p version.io.quarkus -m kogito-quarkus-bom'
    echo 
    echo '  #  - Update Gradle line `quarkusVersion =` to Quarkus 2.0.0.Final '
    echo '  sh update-project-dependencies.sh -q 2.0.0.Final -g "quarkusVersion ="'
    echo
}

parseArgsAsEnv() {
    local OPTIND new_value
    local maven_modules=()
    local maven_props=()
    local gradle_regex=()
    local remote_poms=()

    while getopts "v:m:p:g:c:h" i
    do
        case "$i"
        in
            v)  new_value=${OPTARG} ;;
            m)  maven_modules+=("${OPTARG}") ;;
            p)  maven_props+=("${OPTARG}") ;;
            g)  gradle_regex+=("${OPTARG}") ;;
            c)  remote_poms+=("${OPTARG}") ;;
            h)  usage; exit 0 ;;
            \?) usage; exit 1 ;;
        esac
    done

    if [ "${new_value}" = "" ]; then 
        >&2 echo ERROR: no value specified.
        usage

        exit 2
    fi

    echo "export NEW_VALUE=${new_value};"
    if  [ ! -z ${maven_modules} ]; then echo "export MAVEN_MODULES='${maven_modules[*]}';"; fi
    if  [ ! -z ${maven_props} ]; then echo "export MAVEN_PROPERTIES='${maven_props[*]}';"; fi
    if  [ ! -z ${gradle_regex} ]; then echo "export GRADLE_REGEX='${gradle_regex[*]}';"; fi
    if  [ ! -z ${remote_poms} ]; then echo "export REMOTE_POMS='${remote_poms[*]}';"; fi
}

retrieveGitDefaultValuesAsEnv() {
    local default_pr_branch_name="${1}"
    if [ "${PROJECT}" = "optaplanner-quickstarts" ]; then
        if [ "${default_pr_branch_name}" = "development" ]; then
            default_pr_branch_name='main'
        fi
    fi

    echo "export DEFAULT_GIT_REPOSITORY=${PROJECT};"
    echo "export DEFAULT_GIT_ORIGIN=kiegroup;"
    echo "export DEFAULT_GIT_PR_BRANCH=${default_pr_branch_name}-bump-${NEW_VALUE};"
    echo "export DEFAULT_GIT_COMMIT_MESSAGE='Bump ${NEW_VALUE}';" 
}

function compare_with_remote_poms() {
    for pom in ${REMOTE_POMS[@]}
    do
        ${script_dir_path}/update-maven-compare-dependencies.sh ${pom} ${NEW_VALUE} $1
    done
}

function update_maven_properties() {
    for prop in ${MAVEN_PROPERTIES[@]}
    do
       ${script_dir_path}/update-maven-module-property.sh ${prop} ${NEW_VALUE} $1
    done
}

function update_gradle_regexps() {
    for re in ${GRADLE_REGEX[@]}
    do
        ${script_dir_path}/update-build-gradle-regex-line.sh "${re}" ${NEW_VALUE}
   done
}

showEnv() {
    echo VALUE..................${NEW_VALUE}
    echo MAVEN_MODULES..........${MAVEN_MODULES[@]}
    echo MAVEN_PROPERTIES.......${MAVEN_PROPERTIES[@]}
    echo GRADLE_REGEX...........${GRADLE_REGEX[@]}
    echo REMOTE_POMS ...........${REMOTE_POMS[@]}
    echo
}

execute() {
    set -x
    if [ -z "${MAVEN_MODULES}" ]; then
      compare_with_remote_poms

      update_maven_properties
    else
      for module in ${MAVEN_MODULES[@]}
      do
        compare_with_remote_poms ${module}
  
        update_maven_properties ${module}
      done
    fi

    if [ ! -z "${GRADLE_REGEX}" ]; then
      update_gradle_regexps
    fi
    set +x
}

if [ "$1" != "" ]; then
    parseArgsAsEnv $@
    args=$(parseArgsAsEnv $@)
    status=$?
    if [ "$status" != "0" ]; then
        exit status
    fi
    eval $args
    showEnv
    execute
fi