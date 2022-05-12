import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class JenkinsfileRelease extends JenkinsPipelineSpecification {

	def '[Jenkinsfile.release.run] test load script' () {
		when:
			def Jenkinsfile = loadPipelineScriptForTest('Jenkinsfile.release.run')
		then:
			Jenkinsfile != null
	}
}
