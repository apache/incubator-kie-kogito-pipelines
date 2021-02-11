import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import hudson.plugins.git.GitSCM

class JenkinsfilePrBddTests extends JenkinsPipelineSpecification {
	def Jenkinsfile = null

    def params = []
    def changeAuthor = 'user'
    def changeBranch = 'user-branch'
    def changeTarget = 'user-target'

    def setup() {
        Jenkinsfile = loadPipelineScriptForTest('Jenkinsfile.pr.bdd-tests')

        explicitlyMockPipelineVariable('githubscm')
        Jenkinsfile.getBinding().setVariable('params', params)
        Jenkinsfile.getBinding().setVariable('changeAuthor', changeAuthor)
        Jenkinsfile.getBinding().setVariable('changeBranch', changeBranch)
        Jenkinsfile.getBinding().setVariable('changeTarget', changeTarget)
    }

	def '[JenkinsfilePrBddTests.groovy] getPRRepoName' () {
		setup:
            Jenkinsfile.getBinding().setVariable('env', ['ghprbGhRepository' : 'foo/bar'])
		when:
			def repoName = Jenkinsfile.getPRRepoName()
		then:
            repoName == 'bar'
	}

	def '[JenkinsfilePrBddTests.groovy] addAuthorBranchParamsIfExist: change branch exists' () {
		setup:
            getPipelineMock('githubscm.getRepositoryScm')('repo', changeAuthor, changeBranch) >> 'repo'
		when:
			Jenkinsfile.addAuthorBranchParamsIfExist(params, 'repo')
		then:
            1 * getPipelineMock('string.call').call(['name' : 'GIT_AUTHOR', 'value' : changeAuthor])
            1 * getPipelineMock('string.call').call(['name' : 'BUILD_BRANCH_NAME', 'value' : changeBranch])
	}

	def '[JenkinsfilePrBddTests.groovy] addAuthorBranchParamsIfExist: change target exists' () {
		setup:
            getPipelineMock('githubscm.getRepositoryScm')('repo', changeAuthor, changeBranch) >> null
            getPipelineMock('githubscm.getRepositoryScm')('repo', 'kiegroup', changeTarget) >> 'repo'
		when:
			Jenkinsfile.addAuthorBranchParamsIfExist(params, 'repo')
		then:
            1 * getPipelineMock('string.call').call(['name' : 'BUILD_BRANCH_NAME', 'value' : changeTarget])
	}

	def '[JenkinsfilePrBddTests.groovy] addAuthorBranchParamsIfExist: change branch/target doesn\'t exist' () {
		setup:
            getPipelineMock('githubscm.getRepositoryScm')('repo', changeAuthor, changeBranch) >> null
            getPipelineMock('githubscm.getRepositoryScm')('repo', 'kiegroup', changeTarget) >> null
		when:
			Jenkinsfile.addAuthorBranchParamsIfExist(params, 'repo')
		then:
            0 * getPipelineMock('string.call').call(['name' : 'GIT_AUTHOR', 'value' : changeAuthor])
            0 * getPipelineMock('string.call').call(['name' : 'BUILD_BRANCH_NAME', 'value' : changeBranch])
            0 * getPipelineMock('string.call').call(['name' : 'BUILD_BRANCH_NAME', 'value' : changeTarget])
	}

	def '[JenkinsfilePrBddTests.groovy] getOptaPlannerBranch' () {
        setup:
            Jenkinsfile.getBinding().setVariable('changeTarget', '1.0.x')
		when:
			def optaPlannerBranch = Jenkinsfile.getOptaPlannerBranch()
		then:
            optaPlannerBranch == '8.0.x'
	}

	def '[JenkinsfilePrBddTests.groovy] doesBranchExist: doesn\'t exist' () {
		setup:
            getPipelineMock('githubscm.getRepositoryScm')('repo', changeAuthor, changeBranch) >> null
		when:
            def repo = Jenkinsfile.doesBranchExist('repo', changeAuthor, changeBranch)
		then:
            repo == false
	}

	def '[JenkinsfilePrBddTests.groovy] doesBranchExist: exists' () {
		setup:
            getPipelineMock('githubscm.getRepositoryScm')('repo', changeAuthor, changeBranch) >> 'repo'
		when:
            def repo = Jenkinsfile.doesBranchExist('repo', changeAuthor, changeBranch)
		then:
            repo == true
	}

    def '[JenkinsfilePrBddTests.groovy] addExamplesParams: user branch exists' () {
        setup:
            getPipelineMock('githubscm.getRepositoryScm')('kogito-examples', changeAuthor, changeBranch) >> 'repo'
        when:
            Jenkinsfile.addExamplesParams(params)
        then:
            1 * getPipelineMock('string.call').call(['name' : 'EXAMPLES_URI', 'value' : "https://github.com/${changeAuthor}/kogito-examples"])
            1 * getPipelineMock('string.call').call(['name' : 'EXAMPLES_REF', 'value' : changeBranch])
    }

    def '[JenkinsfilePrBddTests.groovy] addExamplesParams: target branch exists' () {
        setup:
            getPipelineMock('githubscm.getRepositoryScm')('kogito-examples', changeAuthor, changeBranch) >> null
            getPipelineMock('githubscm.getRepositoryScm')('kogito-examples', 'kiegroup', changeTarget) >> 'repo'
        when:
            Jenkinsfile.addExamplesParams(params)
        then:
            1 * getPipelineMock('string.call').call(['name' : 'EXAMPLES_URI', 'value' : 'https://github.com/kiegroup/kogito-examples'])
            1 * getPipelineMock('string.call').call(['name' : 'EXAMPLES_REF', 'value' : changeTarget])
    }

    def '[JenkinsfilePrBddTests.groovy] addExamplesParams: neither exist' () {
        setup:
            getPipelineMock('githubscm.getRepositoryScm')('kogito-examples', changeAuthor, changeBranch) >> null
            getPipelineMock('githubscm.getRepositoryScm')('kogito-examples', 'kiegroup', changeTarget) >> null
        when:
            Jenkinsfile.addExamplesParams(params)
        then:
            1 * getPipelineMock('string.call').call(['name' : 'EXAMPLES_URI', 'value' : 'https://github.com/kiegroup/kogito-examples'])
            1 * getPipelineMock('string.call').call(['name' : 'EXAMPLES_REF', 'value' : 'master'])
    }
}
