import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class JenkinsfileNightlyKogito extends JenkinsPipelineSpecification {
	def Jenkinsfile = null

    def setup() {
        Jenkinsfile = loadPipelineScriptForTest('Jenkinsfile.nightly')
        assert Jenkinsfile != null
    }

	def '[Jenkinsfile.nightly] constructKey: no prefix' () {
		when:
			String paramId = 'paramId'
			String key = Jenkinsfile.constructKey('', paramId)
		then:
			key == paramId
	}

	def '[Jenkinsfile.nightly] constructKey: prefix' () {
		when:
			String prefix = 'prefix'
			String paramId = 'paramId'
			String key = Jenkinsfile.constructKey(prefix, paramId)
		then:
			key == "${prefix}_${paramId}" 
	}
}
