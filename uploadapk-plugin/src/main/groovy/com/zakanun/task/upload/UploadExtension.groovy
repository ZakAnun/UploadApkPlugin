package com.zakanun.task.upload

import org.gradle.api.Project

/**
 * 上传的参数
 */
class UploadExtension {

    static final String FIR = "fir"
    static final String PGY = "pgy"

    /**
     * 目前支持的内测厂商（默认是 fir）
     *
     * 1、fir
     * 2、pgy
     */
    String product = "fir"

    /**
     * obtainUploadUrlParams 获取上传凭证参数
     */
    Map obtainUploadUrlParams = null

    /**
     * 上传参数
     */
    Map uploadParams = null

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