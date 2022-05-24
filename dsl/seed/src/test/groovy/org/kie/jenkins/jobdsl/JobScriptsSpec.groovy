package org.kie.jenkins.jobdsl

import org.kie.jenkins.jobdsl.support.TestUtil
import hudson.model.Item
import hudson.model.View
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.GeneratedJob
import javaposse.jobdsl.dsl.GeneratedView
import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.plugin.JenkinsJobManagement
import javaposse.jobdsl.dsl.Folder
import jenkins.model.Jenkins
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests that all dsl scripts in the jobs directory will compile. All config.xml's are written to build/debug-xml.
 *
 * This runs against the jenkins test harness. Plugins providing auto-generated DSL must be added to the build dependencies.
 */
class JobScriptsSpec extends Specification {

    @Shared
    @ClassRule
    private JenkinsRule jenkinsRule = new JenkinsRule()

    @Shared
    private File outputDir = new File('./build/debug-xml')

    def setupSpec() {
        outputDir.deleteDir()
    }

    @Unroll
    void 'test script #file.absolutePath'(File file) {
        given:
        Map<String, ?> envVars = TestUtil.readBranchConfig()
        envVars.put('DEBUG', false)
        envVars.put('JOB_NAME', 'JOB_NAME')
        envVars.put('GENERATION_BRANCH', 'GENERATION_BRANCH')

        envVars.put('REPO_NAME', 'REPO_NAME')
        envVars.put('GIT_MAIN_BRANCH', 'GIT_MAIN_BRANCH')
        envVars.put('GIT_BRANCH', 'GIT_BRANCH')
        envVars.put('GIT_AUTHOR', 'GIT_AUTHOR')
        envVars.put('MAIN_BRANCHES', '{"default":"main"}')

        envVars.put('CUSTOM_BRANCH_KEY', 'CUSTOM_BRANCH_KEY')
        envVars.put('CUSTOM_REPOSITORIES', 'CUSTOM_REPOSITORIES')
        envVars.put('CUSTOM_AUTHOR', 'CUSTOM_AUTHOR')
        envVars.put('CUSTOM_MAIN_BRANCH', 'CUSTOM_MAIN_BRANCH')

        envVars.put('SEED_AUTHOR', 'SEED_AUTHOR')
        envVars.put('SEED_BRANCH', 'SEED_BRANCH')

        envVars.put('GIT_JENKINS_CONFIG_PATH', 'GIT_JENKINS_CONFIG_PATH')
        JobManagement jm = new JenkinsJobManagement(System.out, envVars, new File('.'))
        jm.createOrUpdateConfig(new Folder(jm, 'nightly'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'nightly.native'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'nightly.mandrel'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'nightly.quarkus-main'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'nightly.quarkus-branch'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'nightly.quarkus-lts'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'nightly.sonarcloud'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'release'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'tools'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'pullrequest'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'pullrequest.kogito-bdd'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'pullrequest.native'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'pullrequest.mandrel'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'pullrequest.quarkus-main'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'pullrequest.quarkus-branch'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'pullrequest.quarkus-lts'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'other'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'update-version'), true)
        jm.createOrUpdateConfig(new Folder(jm, 'kogito-runtimes.bdd'), true)

        when:
        GeneratedItems items = new DslScriptLoader(jm).runScript(file.text)
        writeItems(items, outputDir)

        then:
        noExceptionThrown()

        where:
        file << TestUtil.getJobFiles()
    }

    /**
     * Write the config.xml for each generated job and view to the build dir.
     */
    private void writeItems(GeneratedItems items, File outputDir) {
        Jenkins jenkins = jenkinsRule.jenkins
        items.jobs.each { GeneratedJob generatedJob ->
            String jobName = generatedJob.jobName
            Item item = jenkins.getItemByFullName(jobName)
            String text = new URL(jenkins.rootUrl + item.url + 'config.xml').text
            TestUtil.writeFile(new File(outputDir, 'jobs'), jobName, text)
        }

        items.views.each { GeneratedView generatedView ->
            String viewName = generatedView.name
            View view = jenkins.getView(viewName)
            String text = new URL(jenkins.rootUrl + view.url + 'config.xml').text
            TestUtil.writeFile(new File(outputDir, 'views'), viewName, text)
        }
    }

}
