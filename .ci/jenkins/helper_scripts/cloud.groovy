CLOUD_DEFAULT_PARAM_PREFIX = ''
CLOUD_OLD_PARAM_PREFIX = 'BASE'
CLOUD_NEW_PARAM_PREFIX = 'PROMOTE'

////////////////////////////////////////////////////////////////////////
// Container registry
////////////////////////////////////////////////////////////////////////

void loginContainerRegistry(String paramsPrefix) {
    if (getImageOpenshiftCredentials(paramsPrefix)) {
        loginOpenshiftRegistry(getImageOpenshiftAPI(paramsPrefix), getImageOpenshiftCredentials(paramsPrefix))
    } else if (getImageRegistryCredentials(paramsPrefix)) {
        loginContainerRegistry(getImageRegistry(paramsPrefix), getImageRegistryCredentials(paramsPrefix))
    }
}

void loginOpenshift(String openshiftApi, String openshiftCredsKey) {
    withCredentials([usernamePassword(credentialsId: getDeployImageOpenshiftCredentials(), usernameVariable: 'OC_USER', passwordVariable: 'OC_PWD')]) {
        sh "oc login --username=${OC_USER} --password=${OC_PWD} --server=${openshiftApi} --insecure-skip-tls-verify"
    }
}

void loginOpenshiftRegistry(String openshiftApi, String openshiftCredsKey) {
    loginOpenshift(openshiftApi, openshiftCredsKey)
    // username can be anything. See https://docs.openshift.com/container-platform/4.4/registry/accessing-the-registry.html#registry-accessing-directly_accessing-the-registry
    sh "set +x && ${env.CONTAINER_ENGINE} login -u anything -p \$(oc whoami -t) ${env.CONTAINER_TLS_OPTIONS} ${getOpenshiftRegistry()}"
}

void loginContainerRegistry(String registry, String credsId) {
    withCredentials([usernamePassword(credentialsId: credsId, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PWD')]) {
        sh "${env.CONTAINER_ENGINE} login -u ${REGISTRY_USER} -p ${REGISTRY_PWD} ${env.CONTAINER_TLS_OPTIONS} ${registry}"
    }
}

String getOpenshiftRegistry() {
    return sh(returnStdout: true, script: 'oc get routes -n openshift-image-registry | tail -1 | awk \'{print $2}\'').trim()
}

List pullImages(List imageNames, String paramsPrefix, String imageTag) {
    loginContainerRegistry(paramsPrefix)

    List pulledImages = []
    for (String imageName : imageNames) {
        catchError {
            pullImage(buildImageName(paramsPrefix, imageName, imageTag))
            pulledImages.add(imageName)
        }
    }

    return pulledImages
}

void pullImage(String fullImageName) {
    retry(env.MAX_REGISTRY_RETRIES ?: 1) {
        sh "${env.CONTAINER_ENGINE} pull ${env.CONTAINER_TLS_OPTIONS} ${fullImageName}"
    }
}

List pushImages(List imageNames, String paramsPrefix, String imageTag) {
    loginContainerRegistry(paramsPrefix)

    List pushedImages = []
    for (String imageName : imageNames) {
        catchError {
            pushImage(buildImageName(paramsPrefix, imageName, imageTag))
            pushedImages.add(imageName)
        }
    }

    return pushedImages
}

void pushImage(String fullImageName) {
    retry(env.MAX_REGISTRY_RETRIES ?: 1) {
        sh "${env.CONTAINER_ENGINE} push ${env.CONTAINER_TLS_OPTIONS} ${fullImageName}"
    }
}

void tagImages(List imageNames, String oldParamsPrefix, String oldImageTag, String newParamsPrefix, String newImageTag) {
    for (String imageName : imageNames) {
        tagImage(buildImageName(oldParamsPrefix, imageName, oldImageTag), buildImageName(newParamsPrefix, imageName, newImageTag))
    }
}

void tagImage(String oldImageName, String newImageName) {
    sh "${env.CONTAINER_ENGINE} tag ${oldImageName} ${newImageName}"
}

String getReducedTag(String version) {
    try {
        String[] versionSplit = version.split("\\.")
        return "${versionSplit[0]}.${versionSplit[1]}"
    } catch (error) {
        echo "${getNewImageTag()} cannot be reduced to the format X.Y"
    }
    return ''
}

boolean isQuayRegistry(String registry) {
    return registry == 'quay.io'
}

// Set images public on quay. Useful when new images are introduced.
void makeQuayNewImagesPublic(List images, String paramsPrefix) {
    String namespace = getImageNamespace(paramsPrefix)
    for (String imageName : images) {
        String repository = getFinalImageName(imageName, getImageNameSuffix(paramsPrefix))
        echo "Check and set public if needed Quay repository ${namespace}/${repository}"
        try {
            cloud.makeQuayImagePublic(namespace, repository, [ usernamePassword: getImageRegistryCredentials(paramsPrefix)])
        } catch (err) {
            echo "[ERROR] Cannot set image quay.io/${namespace}/${repository} as visible"
        }
    }
}

void cleanImages() {
    sh "${env.CONTAINER_ENGINE} rm -f \$(${env.CONTAINER_ENGINE} ps -a -q) || date"
    sh "${env.CONTAINER_ENGINE} rmi -f \$(${env.CONTAINER_ENGINE} images -q) || date"
}

////////////////////////////////////////////////////////////////////////
// Image information
////////////////////////////////////////////////////////////////////////

String getImageOpenshiftAPI(String paramsPrefix) {
    return getParamValue('IMAGE_OPENSHIFT_API', paramsPrefix)
}

String getImageOpenshiftCredentials(String paramsPrefix) {
    return getParamValue('IMAGE_OPENSHIFT_CREDS_KEY', paramsPrefix)
}

String getImageRegistryCredentials(String paramsPrefix) {
    return getParamValue('IMAGE_REGISTRY_CREDENTIALS', paramsPrefix)
}

String getImageRegistry(String paramsPrefix) {
    return getDeployImageOpenshiftAPI(paramsPrefix) ? getOpenshiftRegistry() : getParamValue('IMAGE_REGISTRY', paramsPrefix)
}

String getImageNamespace(String paramsPrefix) {
    return getDeployImageOpenshiftAPI(paramsPrefix) ? 'openshift' : getParamValue('IMAGE_NAMESPACE', paramsPrefix)
}

String getImageNameSuffix(String paramsPrefix) {
    return getParamValue('IMAGE_NAME_SUFFIX', paramsPrefix)
}

String getImageTag(String paramsPrefix) {
    String paramsTag = getParamValue('IMAGE_TAG', paramsPrefix)
    return paramsTag ?: 'imaginary_tag'
}

String buildImageName(String paramsPrefix, String imageName, String imageTag) {
    return "${getImageRegistry(paramsPrefix)}/${getImageNamespace(paramsPrefix)}/${getFinalImageName(imageName, paramsPrefix)}:${imageTag ?: getImageTag(paramsPrefix)}"
}

String getFinalImageName(String imageName, String paramsPrefix) {
    String suffix = getImageNameSuffix(paramsPrefix)
    return suffix ? "${imageName}-${suffix}" : imageName
}

////////////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////////////

String getParamValue(String key, String paramsPrefix) {
    return params["${paramsPrefix ? "${paramsPrefix}_" : ''}${key}"]
}

return this
