set -euxo pipefail
 
# set the git remote for the PR
# e.g. I set evacchi for https://github.com/evacchi/quarkus-platform
REMOTE=evacchi
BRANCH=main
KOGITO_VERSION=1.6.0.Final
OPTAPLANNER_VERSION=8.6.0.Final
PR_BRANCH=bump-kogito-$KOGITO_VERSION+optaplanner-$OPTAPLANNER_VERSION
# ensure main branch
git checkout $BRANCH
# create branch
git checkout -b $PR_BRANCH
# process versions
mvn \
versions:set-property \
-Dproperty=kogito-quarkus.version \
-DnewVersion=$KOGITO_VERSION \
-DgenerateBackupPoms=false
mvn \
versions:set-property \
-Dproperty=optaplanner-quarkus.version \
-DnewVersion=$OPTAPLANNER_VERSION \
-DgenerateBackupPoms=false
 
# update pom metadata
mvn validate -Pregen-kogito -N
 
# add custom repositories
cat update-platform-patch.diff | patch pom.xml
 
# commit all
git commit -am "Kogito $KOGITO_VERSION + OptaPlanner $OPTAPLANNER_VERSION"
# push the branch to a remote
git push -u $REMOTE $PR_BRANCH
# Open a PR to kogito-runtimes using the commit as a title
gh pr create --fill --base $BRANCH -R quarkusio/quarkus-platform
