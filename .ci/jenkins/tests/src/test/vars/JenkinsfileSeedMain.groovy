import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class JenkinsfileSeedMain extends JenkinsPipelineSpecification {
	
	def '[Jenkinsfile.seed.main] test load script' () {
		when:
			def Jenkinsfile = loadPipelineScriptForTest('Jenkinsfile.seed.main')
		then:
			Jenkinsfile != null
	}
}
