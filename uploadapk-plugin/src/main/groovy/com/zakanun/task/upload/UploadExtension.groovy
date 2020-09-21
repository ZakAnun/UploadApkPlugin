package com.zakanun.task.upload

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

    /**
     * extensionParams 为兼容不同平台都可以使用，暴露 map 参数列表作为参数列表
     */
    Map extensionParams = null

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