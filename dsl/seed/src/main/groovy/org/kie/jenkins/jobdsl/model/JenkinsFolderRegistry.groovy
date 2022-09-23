package org.kie.jenkins.jobdsl.model

import org.kie.jenkins.jobdsl.model.JobType
import org.kie.jenkins.jobdsl.utils.EnvUtils

class JenkinsFolderRegistry {

    static final Map<String, JenkinsFolder> FOLDER_REGISTRY = [:]

    private JenkinsFolderRegistry() {
    }

    static boolean hasFolder(String folderName) {
        return FOLDER_REGISTRY.containsKey(folderName)
    }

    static void register(JenkinsFolder folder) {
        if (hasFolder(folder.getName())) {
            throw new RuntimeException("Trying to register a new folder with name '${folder.getName()}' but this one is already existing in registry...")
        }
        if (folder.environmentName && !folder.jobType.isEnvironmentDependent()) {
            throw new RuntimeException("Trying to register a new folder of type ${folder.jobType.getName()} for the environment ${folder.environmentName} but this job type is not environment dependent...")
        }
        FOLDER_REGISTRY.put(folder.getName(), folder)
    }

    static JenkinsFolder getOrRegisterFolder(def script, JobType jobType, String envName = '') {
        // Create folder struct
        Map defaultEnv = [:]
        if (envName) {
            defaultEnv = EnvUtils.getEnvironmentEnvVars(script, envName)
        }
        JenkinsFolder folder = new JenkinsFolder(jobType, defaultEnv, envName)

        if (hasFolder(folder.getName())) {
            return getFolder(folder.getName())
        }

        register(folder)
        return folder
    }

    static JenkinsFolder getFolder(String folderName) {
        if (hasFolder(folderName)) {
            return FOLDER_REGISTRY.get(folderName)
        }
        return null
    }

    static getPullRequestFolder(def script, String envName = '') {
        return getOrRegisterFolder(script, JobType.PULL_REQUEST, envName)
    }

    static getNightlyFolder(def script, String envName = '') {
        return getOrRegisterFolder(script, JobType.NIGHTLY, envName)
    }

}
