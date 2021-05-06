set -euxo pipefail
 
# set the git remote for the PR
# e.g. I set evacchi for https://github.com/evacchi/kogito-runtimes
REMOTE=evacchi
BRANCH=master
PREFIX=master
# kogito or optaplanner
PROJECT=kogito
# kogito-runtimes or optaplanner
REPO=kogito-runtimes
QUARKUS_VERSION=2.0.0.Alpha1
MAVEN_VERSION=3.6.2
PR_BRANCH=bump-$PREFIX-quarkus-$QUARKUS_VERSION
 
# create branch named like version
git checkout -b $PR_BRANCH
 
# align third-party dependencies with Quarkus
mvn versions:compare-dependencies \
-pl :${PROJECT}-build-parent \
-DremotePom=io.quarkus:quarkus-bom:$QUARKUS_VERSION \
-DupdatePropertyVersions=true \
-DupdateDependencies=true \
-DgenerateBackupPoms=false
 
# update Quarkus version
mvn -pl :${PROJECT}-build-parent \
   versions:set-property \
   -Dproperty=version.io.quarkus \
   -DnewVersion=$QUARKUS_VERSION \
   -DgenerateBackupPoms=false
 
# pin Maven version
mvn -pl :${PROJECT}-build-parent \
versions:set-property \
-Dproperty=version.maven \
-DnewVersion=$MAVEN_VERSION \
-DgenerateBackupPoms=false
 
# commit all
git commit -am "Bump Quarkus $QUARKUS_VERSION"
 
# push the branch to a remote
git push -u $REMOTE $PR_BRANCH
 
# Open a PR to kogito-runtimes using the commit as a title
# e.g. see https://github.com/kiegroup/kogito-runtimes/pull/1200
gh pr create --fill --base $BRANCH -R kiegroup/$REPO
