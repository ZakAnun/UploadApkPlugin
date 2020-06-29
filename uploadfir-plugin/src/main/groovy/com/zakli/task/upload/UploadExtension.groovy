package com.zakli.task.upload

import org.gradle.api.Project

/**
 * 上传的参数
 */
class UploadExtension {

    /**
     * apiToken
     */
    String apiToken = ""

    /**
     * appName
     */
    String appName = ""

    /**
     * appVersionName
     */
    String appVersionName = ""

    /**
     * appVersion
     */
    String appVersion = ""

    /**
     * appChangeLog
     */
    String appChangeLog = ""

    UploadExtension(Project project) {

    }

    public static UploadExtension getConfig(Project project) {
        UploadExtension config = project.getExtensions().findByType(UploadExtension.class)
        if (config == null) {
            config = new UploadExtension()
        }
        return config
    }

}