import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class JenkinsfileNightly extends JenkinsPipelineSpecification {
	
	def '[Jenkinsfile.nightly] test load script' () {
		when:
			def Jenkinsfile = loadPipelineScriptForTest('Jenkinsfile.nightly')
		then:
			Jenkinsfile != null
	}
}
