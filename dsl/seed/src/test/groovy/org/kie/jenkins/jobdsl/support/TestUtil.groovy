package org.kie.jenkins.jobdsl.support

import org.yaml.snakeyaml.Yaml

import groovy.io.FileType
import java.util.Properties

class TestUtil {

    static List<File> getJobFiles() {
        List<File> files = []
        new File('jobs').eachFileRecurse(FileType.FILES) {
            if (it.name.endsWith('.groovy')) {
                files << it
            }
        }
        files
    }

    /**
     * Write a single XML file, creating any nested dirs.
     */
    static void writeFile(File dir, String name, String xml) {
        List tokens = name.split('/')
        File folderDir = tokens[0..<-1].inject(dir) { File tokenDir, String token ->
            new File(tokenDir, token)
        }
        folderDir.mkdirs()

        File xmlFile = new File(folderDir, "${tokens[-1]}.xml")
        xmlFile.text = xml
    }

    static Map readBranchConfig() {
        Map config = new Yaml().load(('./branch_config.yaml' as File).text)

        Map props = [:]
        fillEnvProperties(props, '', config)
        return props
    }

    static void fillEnvProperties(Map envProperties, String envKeyPrefix, Map propsMap) {
        propsMap.each { key, value ->
            String newKey = generateEnvKey(envKeyPrefix, key)
            if (value instanceof Map) {
                fillEnvProperties(envProperties, newKey, value as Map)
            } else if (value instanceof List) {
                envProperties[newKey] = (value as List).join(',')
            } else {
                envProperties[newKey] = value
            }
        }
    }

    static String generateEnvKey(String envKeyPrefix, String key) {
        return (envKeyPrefix ? "${envKeyPrefix}_${key}" : key).toUpperCase()
    }

}
