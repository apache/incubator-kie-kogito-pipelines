import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class JenkinsfileSeedBranch extends JenkinsPipelineSpecification {
	
	def '[Jenkinsfile.seed.branch] test load script' () {
		when:
			def Jenkinsfile = loadPipelineScriptForTest('Jenkinsfile.seed.branch')
		then:
			Jenkinsfile != null
	}
}
