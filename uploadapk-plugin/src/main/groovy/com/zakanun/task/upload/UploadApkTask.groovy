package com.zakanun.task.upload

import com.android.build.gradle.api.BaseVariant
import com.zakanun.task.bean.IStreamListener
import com.zakanun.task.bean.ProgressFileBody
import groovy.json.JsonSlurper
import org.apache.http.Consts
import org.apache.http.HttpEntity
import org.apache.http.NameValuePair
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * 上传任务
 */
class UploadApkTask extends DefaultTask {

    @Input
    public BaseVariant variant
    @Input
    public Project targetProject

    public void setup() {
        description "Upload apk"
        group "Build"
    }

    @TaskAction
    public void upload() {
        UploadExtension extension = UploadExtension.getConfig(targetProject)

        def output = variant.outputs[0]
        if (output != null) {
            def apkFile = output.outputFile
            try {
                def file = new File(apkFile.path)
                if (extension.product == UploadExtension.FIR) {
                    def target = obtainFirUploadParam(extension.obtainUploadUrlParams)
                    if (target != null) {
                        def key = target['key'].toString()
                        def token = target['token'].toString()
                        def uploadUrl = target['upload_url'].toString()

                        println("key = $key, token = $token, uploadUrl = $uploadUrl")
                        if (key == null || token == null || uploadUrl == null) {
                            targetProject.logger.error("获取上传参数出错，结束...")
                            return
                        }

                        doFirUpload(extension.uploadParams, file, key, token, uploadUrl)
                    }
                } else if (extension.product == UploadExtension.PGY) {
                    doPgyUpload(extension.uploadParams, file)
                } else {
                    targetProject.logger.error("暂不支持其他的内测平台...")
                }
            } catch (Exception e) {
                println("apk 文件没找到... 原因: ${e.message}")
            }
        }
    }

    /**
     * 获取上传参数
     *
     * @param obtainUploadUrlParams 获取上传 url 参数
     * @return 获取结果
     */
    static def obtainFirUploadParam(Map<String, String> obtainUploadUrlParams) {
        CloseableHttpClient httpclient = HttpClients.createDefault()
        HttpPost apkApiPost = new HttpPost("http://api.bq04.com/apps")
        // 拼接参数
        List <NameValuePair> nvp = new ArrayList <NameValuePair>()
        for (kv in obtainUploadUrlParams) {
            nvp.add(new BasicNameValuePair(kv.key, kv.value))
        }
        apkApiPost.setEntity(new UrlEncodedFormEntity(nvp))
        CloseableHttpResponse apkApiResponse = httpclient.execute(apkApiPost)

        def target = null
        try {
            println(apkApiResponse.getStatusLine())
            HttpEntity entity = apkApiResponse.getEntity()
            def resultStr = EntityUtils.toString(entity)
            def jsonController = new JsonSlurper()
            def result = jsonController.parseText(resultStr)
            def cert = result['cert']
            if (cert == null) {
                def msg = result['msg']
                println("cert is null cause by " + msg == null ? "unknown" : msg)
            } else {
                def binary = cert['binary']
                if (binary != null) {
                    target = binary
                }
            }
            if (cert != null && target == null) {
                println("获取上传信息出错，结束...")
            }
            // 消耗掉response
            EntityUtils.consume(entity)
        } catch (IOException e) {
            println("${e.message}")
        } finally {
            try {
                httpclient.close()
                apkApiResponse.close()
            } catch (IOException e) {
                println("${e.message}")
            }
        }

        return target
    }

    /**
     * 执行上传
     *
     * @param uploadParams
     * @param file
     * @param key
     * @param token
     * @param uploadUrl
     * @return
     */
    static def doFirUpload(Map<String, String> uploadParams, File file,
                           String key, String token, String uploadUrl) {
        println("======================= 开始 FIR 上传 =======================")
        CloseableHttpClient httpClient = HttpClients.createDefault()
        HttpPost firApiPost = new HttpPost(uploadUrl)
        RequestConfig config = RequestConfig
                .custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(20000)
                .build()
        firApiPost.setConfig(config)
        ProgressFileBody fileBody = new ProgressFileBody(file,
                ContentType.APPLICATION_OCTET_STREAM,
                file.getName())
        fileBody.setListener(new IStreamListener() {
            @Override
            void onProgress(Integer progress) {
                println("> Task :upload-fir PROGRESS ==========> ${progress}%")
            }
        })
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
        ContentType contentType = ContentType.create("text/plain", Consts.UTF_8)
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        builder.setCharset(Consts.UTF_8)
        builder.addPart("file", fileBody)
        builder.addTextBody("key", key, contentType)
        builder.addTextBody("token", token, contentType)
        for (kv in uploadParams) {
            builder.addTextBody(kv.key, kv.value, contentType)
        }

        HttpEntity entity = builder.build()
        firApiPost.setEntity(entity)
        String result = ""
        CloseableHttpResponse response = httpClient.execute(firApiPost)
        try {
            HttpEntity resEntity = response.getEntity()
            if (entity != null) {
                result = EntityUtils.toString(resEntity, Consts.UTF_8)
                println()
                println("response content:" + result)
                println()
            }
            EntityUtils.consume(entity)
        } catch (IOException e) {
            println("${e.message}")
        } finally {
            try {
                httpClient.close()
                response.close()
                println("======================= 结束 FIR 上传 =======================")
            } catch (IOException e) {
                println("${e.message}")
            }
        }

        return result
    }

    static def doPgyUpload(Map<String, String> uploadParams, File file) {
        println("======================= 开始 PGY 上传 =======================")
        CloseableHttpClient httpClient = HttpClients.createDefault()
        HttpPost pgyApiPost = new HttpPost("https://upload.pgyer.com/apiv1/app/upload")
        RequestConfig config = RequestConfig
                .custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(20000)
                .build()
        pgyApiPost.setConfig(config)
        ProgressFileBody fileBody = new ProgressFileBody(file, ContentType.APPLICATION_OCTET_STREAM, file.getName())
        fileBody.setListener(new IStreamListener() {
            @Override
            void onProgress(Integer progress) {
                println("> Task :upload-pgy PROGRESS ==========> ${progress}%")
            }
        })
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
        ContentType contentType = ContentType.create("text/plain", Consts.UTF_8)
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        builder.setCharset(Consts.UTF_8)
        builder.addPart("file", fileBody)
        for (kv in uploadParams) {
            builder.addTextBody(kv.key, kv.value, contentType)
        }

        HttpEntity entity = builder.build()
        pgyApiPost.setEntity(entity)
        String result = ""
        CloseableHttpResponse response = httpClient.execute(pgyApiPost)
        try {
            HttpEntity resEntity = response.getEntity()
            if (entity != null) {
                result = EntityUtils.toString(resEntity, Consts.UTF_8)
                println()
                println("response content: " + result)
                println()
            }
            EntityUtils.consume(entity)
        } catch (IOException e) {
            println("${e.message}")
        } finally {
            try {
                httpClient.close()
                response.close()
                println("======================= 结束 PGY 上传 =======================")
            } catch (IOException e) {
                println("${e.message}")
            }
        }

        return result
    }
}