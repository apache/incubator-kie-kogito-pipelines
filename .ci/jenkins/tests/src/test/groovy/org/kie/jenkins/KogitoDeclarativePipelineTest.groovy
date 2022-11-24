package org.kie.jenkins

import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.ProjectSource.projectSource
import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.assertj.core.api.Assertions.assertThat

@groovy.transform.InheritConstructors
abstract class KogitoDeclarativePipelineTest extends DeclarativePipelineTest {

    Map testCallstack = [:]

    Map sharedLibsMock = [:]

    void setUp() throws Exception {
        super.setUp()
        sharedLibsMock.clear()

        helper.registerAllowedMethod('stringParam', [Map.class], { map -> return "${map.name}=${map.value}" })
        helper.registerAllowedMethod('booleanParam', [Map.class], { map -> return "${map.name}=${map.value}" })
        helper.registerAllowedMethod('string', [Map.class], { map ->
            if (map.containsKey('credentialsId')) {
                return map.get('variable')
            }
            return "${map.name}=${map.value}"
        })
    }

    /**
     * Helper for adding a params value in tests
     */
    void addParam(String name, Object val) {
        Map params = binding.getVariable('params') as Map
        if (params == null) {
            params = [:]
            binding.setVariable('params', params)
        }
        if (val != null) {
            params[name] = val
        }
    }

    /**
     * Helper for adding a environment value in tests
     */
    void addEnvVar(String name, String val) {
        if (!binding.hasVariable('env')) {
            binding.setVariable('env', new Expando(getProperty: { p -> this[p] }, setProperty: { p, v -> this[p] = v }))
        }
        def env = binding.getVariable('env') as Expando
        env[name] = val
    }

    //////////////////////////////////////////////////////////////////////////////////
    // Config File plugin
    //////////////////////////////////////////////////////////////////////////////////

    void mockConfigFilePlugin(String fileEnvVarName) {
        helper.registerAllowedMethod('configFile', [Map.class], { map -> return 'CONFIG_FILE' })
        helper.registerAllowedMethod('configFileProvider', [Collection.class, Closure.class], { collection, closure ->
            binding.setVariable(fileEnvVarName, fileEnvVarName)
            closure()
        })
    }

    void assertConfigFileCall(String fileId, String fileEnvVarName) {
        assertMethodCallContainsArg('configFileProvider', 'CONFIG_FILE')
        assertMethodCallContainsArg('configFile', "{fileId=${fileId}, variable=${fileEnvVarName}}")
    }

    void assertNoConfigFileCall(String settingsXmlFile, String fileEnvVarName) {
        assertMethodCallDoesNotContainArg('configFile', "{fileId=${fileId}, variable=${fileEnvVarName}}")
    }

    //////////////////////////////////////////////////////////////////////////////////
    // Junit plugin
    //////////////////////////////////////////////////////////////////////////////////

    void mockJUnitPlugin() {
        helper.registerAllowedMethod('junit', [Map.class], { map -> println 'junit' })
    }

    void assertJunitCall(String testResults) {
        assertMethodCallContainsArg('junit', testResults)
    }

    void assertNoJunitCall(String testResults) {
        assertMethodCallDoesNotContainArg('junit', testResults)
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Test Call Stack
    // This allows to register some specific call done during the test
    // And check those are called/not called
    // Useful when the declarative pipeline is not handling those
    // Main example would be on shared library calls
    ////////////////////////////////////////////////////////////////////////////////

    void registerTestCallstack(String callstackId, String callstackValue = '') {
        def values = []
        if (!testCallstack.containsKey(callstackId)) {
            testCallstack[callstackId] = values
        } else {
            values = testCallstack.get(callstackId)
        }
        values.add(callstackValue)
    }

    void assertTestCallstackDoesNotContain(String callstackId) {
        assertThat(testCallstack.containsKey(callstackId))
            .as('Test callstack does NOT contain call to %s', callstackId)
            .isFalse()
    }

    void assertTestCallstackContains(String callstackId, String callstackValue = '') {
        assertThat(testCallstack.containsKey(callstackId))
            .as('Test callstack contains call to %s', callstackId)
            .isTrue()
        if (callstackValue) {
            assertThat(testCallstack.get(callstackId).any { value ->
                    value == callstackValue
            })
                .as('Test callstack contains call to %s with value %s', callstackId, callstackValue)
                .isTrue()
        }
    }

    void printCallStack() {
        super.printCallStack()

        println '---------------------------------------------------------------'
        println 'Test Call Stack'
        println '---------------------------------------------------------------'

        testCallstack.each { id, values ->
            values.each { val ->
                println "\t${id}=${val}"
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Shared lib mocking
    ////////////////////////////////////////////////////////////////////////////////

    void mockSharedLibraries(String... sharedLibNames) {
        helper.libLoader.preloadLibraryClasses = true
        sharedLibNames.each {
            helper.registerSharedLibrary(library(it)
                .defaultVersion('mock')
                .allowOverride(true)
                .implicit(false)
                .targetPath('mock')
                .retriever(projectSource())
                .build())
        }
    }

    void mockSharedLibVarsCall(String moduleName, String methodName, Closure mockClosure = null) {
        def moduleMock = [:]
        if (sharedLibsMock.containsKey(moduleName)) {
            moduleMock = sharedLibsMock.get(moduleName)
        } else {
            sharedLibsMock.put(moduleName, moduleMock)
            binding.setVariable(moduleName, moduleMock)
        }

        moduleMock[methodName] = mockClosure ?: { registerTestCallstack("${moduleName}.${methodName}") }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Util assert methods
    ////////////////////////////////////////////////////////////////////////////////

    void assertEnvironmentVariableContains(String envKey, String value) {
        assertThat(binding.getVariable('env')."${envKey}")
            .as('Env var %s contains %s', envKey, value)
            .contains(value)
    }

    void assertEnvironmentVariableEqual(String envKey, String value) {
        assertThat(binding.getVariable('env')."${envKey}")
            .as('Env var %s is equal to %s', envKey, value)
            .isEqualTo(value)
    }

    void assertEnvironmentVariableDoesNotContain(String envKey, String value) {
        assertThat(binding.getVariable('env')."${envKey}")
            .as('Env var %s does NOT contain %s', envKey, value)
            .doesNotContain(value)
    }

    void assertEnvironmentVariableNotEqual(String envKey, String value) {
        assertThat(binding.getVariable('env')."${envKey}")
            .as('Env var %s is NOT equal to %s', envKey, value)
            .isNotEqualTo(value)
    }

    void assertStageCall(String stageName) {
        assertMethodCallContainsArg('stage', stageName)
    }

    void assertNoStageCall(String stageName) {
        assertMethodCallDoesNotContainArg('stage', stageName)
    }

    void assertDirCall(String directory) {
        assertMethodCallContainsArg('dir', directory)
    }

    void assertNoDirCall(String directory) {
        assertMethodCallDoesNotContainArg('dir', directory)
    }

    void assertShCall(String command) {
        assertMethodCallContainsArg('sh', command)
    }

    void assertNoShCall(String command) {
        assertMethodCallDoesNotContainArg('sh', command)
    }

    void assertArchiveArtifactsCall(String artifacts) {
        assertMethodCallContainsArg('archiveArtifacts', artifacts)
    }

    void assertNoArchiveArtifactsCall(String artifacts) {
        assertMethodCallDoesNotContainArg('archiveArtifacts', artifacts)
    }

    void assertEchoCall(String message) {
        assertMethodCallContainsArg('echo', message)
    }

    void assertNoEchoCall(String message) {
        assertMethodCallDoesNotContainArg('echo', message)
    }

    void assertTimeoutCall(String time, String unit) {
        assertMethodCallContainsArg('timeout', "{time=${time}, unit=${unit}}")
    }

    void assertWithCredentialsStringCall(String credsId, String envVar) {
        assertMethodCallContainsArg('withCredentials', envVar)
        assertMethodCallContainsArg('string', "{credentialsId=${credsId}, variable=${envVar}}")
    }

    void assertNoWithCredentialsStringCall(String settingsXmlFile, String fileEnvVarName) {
        assertMethodCallDoesNotContainArg('string', "{credentialsId=${credsId}, variable=${envVar}}")
    }

    void assertBuildCall(String jobName, Map parameters, Boolean shouldWait = null, Boolean shouldPropagate=null) {
        Map buildParams = [
            job: jobName,
            wait: shouldWait?.toBoolean(),
            parameters: parameters.collect { "${it.key}=${it.value}" },
            propagate: shouldPropagate?.toBoolean(),
        ]
        assertMethodCall('build', buildParams)
    }

    void assertNoBuildCall(String jobName) {
        assertMethodCallDoesNotContainArg('build', jobName)
    }

    void assertMethodCall(String methodName, Map jobParams = [:]) {
        assertThat(helper.callStack.findAll { call ->
            call.methodName == methodName
        }.size())
        .as('Method %s is called', methodName)
        .isGreaterThan(0)

        if (jobParams) {
            String jobParamsStr = "{${jobParams.collect { "${it.key}=${it.value}" }.join(', ')}}"
            assertThat(helper.callStack.findAll { call ->
                call.methodName == methodName
            }.any { call ->
                callArgsToString(call) == jobParamsStr
            })
            .as('Method %s is called with arguments %s', methodName, jobParams)
            .isTrue()
        }
    }

    void assertNoMethodCall(String methodName) {
        assertThat(helper.callStack.findAll { call ->
            call.methodName == methodName
        }.size())
        .as('Method %s is NOT called', methodName)
        .isLessThanOrEqualTo(0)
    }

    void assertMethodCallContainsArg(String methodName, String... args) {
        assertThat(helper.callStack.findAll { call ->
            call.methodName == methodName
        }.any { call ->
            boolean ok = true
            args.each { arg ->
                ok = ok && callArgsToString(call).contains(arg)
            }
            return ok
        })
        .as('Method %s is called with arguments containing %s', methodName, "${args}")
        .isTrue()
    }

    void assertMethodCallDoesNotContainArg(String methodName, String arg) {
        assertThat(helper.callStack.findAll { call ->
            call.methodName == methodName
        }.any { call ->
            callArgsToString(call).contains(arg)
        })
        .as('Method %s is called with arguments NOT containing %s', methodName, arg)
        .isFalse()
    }

}
