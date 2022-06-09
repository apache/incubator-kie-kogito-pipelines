import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class JenkinsfileNightlyKogito extends JenkinsPipelineSpecification {
	def Jenkinsfile = null

    def setup() {
        Jenkinsfile = loadPipelineScriptForTest('Jenkinsfile.nightly.kogito')
        assert Jenkinsfile != null
    }

	def '[Jenkinsfile.nightly.kogito] constructKey: no prefix' () {
		when:
			String paramId = 'paramId'
			String key = Jenkinsfile.constructKey('', paramId)
		then:
			key == paramId
	}

	def '[Jenkinsfile.nightly.kogito] constructKey: prefix' () {
		when:
			String prefix = 'prefix'
			String paramId = 'paramId'
			String key = Jenkinsfile.constructKey(prefix, paramId)
		then:
			key == "${prefix}_${paramId}" 
	}
}
