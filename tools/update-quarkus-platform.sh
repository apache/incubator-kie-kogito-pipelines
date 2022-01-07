#!/usr/bin/env bash
set -e

usage() {
    echo 'Usage: update-quarkus-platform.sh -v $MINOR_VERSION COMMAND'
    echo
    echo 'Options:'
    echo '  -v $MINOR_VERSION    set MINOR version'
    echo '                       e.g. 6.0.Final for Kogito 1.6.0.Final and OptaPlanner 8.6.0.Final'
    echo '  COMMAND              may be `stage` or `finalize`'
    echo
    echo 'Examples:'
    echo '  # Stage the PR'
    echo '  #  - Bump Kogito 1.7.0.Final + OptaPlanner 8.7.0.Final'
    echo '  #  - Add staging repositories'
    echo '  sh update-quarkus-platform.sh -v 7.0.Final stage'
    echo
    echo '  # Finalize the PR:'
    echo '  #  - Remove staging repositories'
    echo '  sh update-quarkus-platform.sh -v 7.0.Final finalize'
}

parseArgsAsEnv() {
    local OPTIND minor_version command

    while getopts "v:h" i
    do
        case "$i"
        in
            v) minor_version=${OPTARG} ;;
            h) usage; exit 0 ;;
            \?) usage; exit 1 ;;
        esac
    done
    shift "$((OPTIND-1))"

    if [ "${minor_version}" = "" ]; then 
        >&2 echo ERROR: no minor version specified.
        usage

        exit 2
    fi

    command=$1
    case "${command}"
    in
        stage)
            command='stage'
            ;;
        finalize)
            command='finalize'
            ;;
        *)
            >&2 echo ERROR: invalid command $command.

            exit 2
    esac

    echo "export KOGITO_VERSION=1.${minor_version};"
    echo "export OPTAPLANNER_VERSION=8.${minor_version};"
    echo "export COMMAND=${command};"
}

retrieveGitDefaultValuesAsEnv() {
    echo "export DEFAULT_GIT_REPOSITORY=quarkus-platform;"
    echo "export DEFAULT_GIT_PR_BRANCH=bump-kogito-${KOGITO_VERSION}+optaplanner-${OPTAPLANNER_VERSION};"
    if [ "${COMMAND}" = "stage" ]; then
        echo "export DEFAULT_GIT_ORIGIN=quarkusio;"
        echo "export DEFAULT_GIT_COMMIT_MESSAGE='Kogito ${KOGITO_VERSION} + OptaPlanner ${OPTAPLANNER_VERSION}';" 
    elif [ "${COMMAND}" = "finalize" ]; then
        echo "export DEFAULT_GIT_CHECKOUT_BRANCH=${DEFAULT_GIT_PR_BRANCH};"
        echo "export DEFAULT_GIT_COMMIT_MESSAGE='Updated maven config';" 
    fi
}

stage() {
    set -x

    # add custom repositories
    echo "$DIFF_FILE" | patch .github/mvn-settings.xml

    # process versions
    ./mvnw \
        -s .github/mvn-settings.xml \
        versions:set-property \
        -Dproperty=kogito-quarkus.version \
        -DnewVersion=$KOGITO_VERSION \
        -DgenerateBackupPoms=false
    ./mvnw \
        -s .github/mvn-settings.xml \
        versions:set-property \
        -Dproperty=optaplanner-quarkus.version \
        -DnewVersion=$OPTAPLANNER_VERSION \
        -DgenerateBackupPoms=false
    
    # update pom metadata
    ./mvnw -s .github/mvn-settings.xml validate -Pregen-kogito -N

    ./mvnw -s .github/mvn-settings.xml -Dsync

    set +x
}

finalize() {
    set -x

    # undo patch to add repos
    echo "$DIFF_FILE" | patch -R .github/mvn-settings.xml

    ./mvnw -Dsync

    set +x
}

showEnv() {
    echo "KOGITO_VERSION...........${KOGITO_VERSION}"
    echo "OPTAPLANNER_VERSION......${OPTAPLANNER_VERSION}"
    echo "COMMAND..................${COMMAND}"
    echo
}

execute() {
    $COMMAND
}

DIFF_FILE='diff --git a/.github/mvn-settings.xml b/.github/mvn-settings.xml
index d5e4664b..b03cc023 100644
--- a/.github/mvn-settings.xml
+++ b/.github/mvn-settings.xml
@@ -14,6 +14,14 @@
             <enabled>false</enabled>
           </snapshots>
         </repository>
+        <repository>
+          <snapshots>
+              <enabled>false</enabled>
+          </snapshots>
+          <id>kogito</id>
+          <name>kogito</name>
+          <url>https://repository.jboss.org/nexus/content/groups/kogito-public/</url>
+        </repository>
       </repositories>
       <pluginRepositories>
         <pluginRepository>
'

if [ "$1" != "" ]; then
    # parseArgsAsEnv $@
    args=$(parseArgsAsEnv $@)
    status=$?
    if [ "$status" != "0" ]; then
        exit status
    fi
    eval $args
    execute
fi