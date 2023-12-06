import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class PullRequestSpec extends JenkinsPipelineSpecification {
    def groovyScript = null
    def projectCollection = ['projectA', 'projectB', 'projectC']

    def setup() {
        groovyScript = loadPipelineScriptForTest("vars/pullrequest.groovy")
        explicitlyMockPipelineVariable("out")
        groovyScript.metaClass.WORKSPACE = '/'
    }

    def "PR from fork getAuthorAndRepoForPr" () {
        setup:
        def env = [:]
        env['CHANGE_FORK']='contributor/fork'
        env['CHANGE_URL']='https://github.com/owner/repo/pull/1'
        groovyScript.getBinding().setVariable('env', env)
        when:
        def result = groovyScript.getAuthorAndRepoForPr()
        then:
        result == 'contributor/fork'
    }

    def "PR from origin getAuthorAndRepoForPr" () {
        setup:
        def env = [:]
        env['CHANGE_URL']='https://github.com/owner/repo/pull/1'
        groovyScript.getBinding().setVariable('env', env)
        when:
        def result = groovyScript.getAuthorAndRepoForPr()
        then:
        result == 'owner/repo'
    }
}