import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class JenkinsfileRelease extends JenkinsPipelineSpecification {

	def '[Jenkinsfile.release] test load script' () {
		when:
			def Jenkinsfile = loadPipelineScriptForTest('Jenkinsfile.release')
		then:
			Jenkinsfile != null
	}
}
