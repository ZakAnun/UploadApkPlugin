package com.zakli.task.upload

import com.alibaba.fastjson.JSONObject
import com.android.build.gradle.api.BaseVariant
import com.zakli.task.bean.IStreamListener
import com.zakli.task.bean.ProgressFileBody
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
class UploadFirTask extends DefaultTask {

    private static final String DOT_APK = ".apk"

    @Input
    public BaseVariant variant
    @Input
    public Project targetProject

    public void setup() {
        description "Upload fir apk"
        group "Build"
    }

    @TaskAction
    public void upload() {
        UploadExtension extension = UploadExtension.getConfig(targetProject)

        if (!isApiTokenValidated(extension.apiToken)) {
            targetProject.logger.error("需要传入 apiToken，可以到 fir.im 上进行申请")
            return
        }

        def variantName = variant.name.capitalize()
        def projectName = targetProject.name
        def applicationId = variant.getApplicationId()

        def iterator = variant.outputs.iterator()
        while (iterator.hasNext()) {
            def it = iterator.next()
            def apkFile = it.outputFile

            try {
                def file = new File(apkFile.path)

                if (isAppNameValidated(extension.appName)) {
                    extension.appName = "${projectName}-${variantName} ${variant.versionName}"
                } else {
                    def oldName = extension.appName
                    extension.appName = "${oldName}-${variantName} ${variant.versionName}"
                }

                if (isAppVersionValidated(extension.appVersion)) {
                    extension.appVersion = "${variant.versionCode}"
                }

                println("关键参数 " +
                        "applicationId = ${applicationId} \n" +
                        "apkPath = ${apkFile.path} \n" +
                        "apkSize = ${file.size()} \n" +
                        "apiToken = ${extension.apiToken} \n" +
                        "appName = ${extension.appName} \n" +
                        "versionCode = ${variant.versionCode} \n" +
                        "versionName = ${variant.versionName} \n" +
                        "flavorName = ${variant.flavorName} \n")

                def target = obtainUploadParam(applicationId, extension.apiToken)
                if (target != null && target instanceof JSONObject) {
                    def targetJson = target as JSONObject
                    def key = targetJson.getString("key")
                    def token = targetJson.getString("token")
                    def uploadUrl = targetJson.getString("upload_url")

                    if (key == null || token == null || uploadUrl == null) {
                        println("获取上传参数出错，结束...")
                        return
                    }

                    doUpload(extension, file, key, token, uploadUrl)
                }
            } catch (Exception e) {
                println("apk 文件没找到... 原因: ${e.message}")
            }
            // 只执行一次循环
            break
        }
    }

    /**
     * 获取上传参数
     *
     * @param bundleId
     * @param apiToken
     * @return
     */
    def obtainUploadParam(String bundleId, String apiToken) {

        CloseableHttpClient httpclient = HttpClients.createDefault()
        HttpPost firApiPost = new HttpPost("http://api.bq04.com/apps")
        // 拼接参数
        List <NameValuePair> nvp = new ArrayList <NameValuePair>()
        nvp.add(new BasicNameValuePair("type", "android"))
        nvp.add(new BasicNameValuePair("bundle_id", bundleId))
        nvp.add(new BasicNameValuePair("api_token", apiToken))
        firApiPost.setEntity(new UrlEncodedFormEntity(nvp))
        CloseableHttpResponse firApiResponse = httpclient.execute(firApiPost)

        def target = null
        try {
            println(firApiResponse.getStatusLine())
            HttpEntity entity = firApiResponse.getEntity()
            def resultStr = EntityUtils.toString(entity)
            JSONObject result = JSONObject.parse(resultStr)
            def cert = obtainCert(result)
            if (cert == null) {
                def msg = result.getString("msg")
                println("cert is null cause by " + msg == null ? "unknown" : msg)
            } else {
                def binary = obtainBinary(cert)
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
                firApiResponse.close()
            } catch (IOException e) {
                println("${e.message}")
            }
        }

        return target
    }

    /**
     * 获取 cert
     * @param result 请求数据
     * @return cert json 对象
     */
    def obtainCert(JSONObject result) {
        return ((JSONObject) result.get("cert"))
    }

    /**
     * 获取 binary
     * @param cert
     * @return
     */
    def obtainBinary(JSONObject cert) {
        return ((JSONObject) cert.get("binary"))
    }

    /**
     * 执行上传
     *
     * @param extension
     * @param file
     * @param key
     * @param token
     * @param uploadUrl
     * @return
     */
    def doUpload(UploadExtension extension, File file,
                 String key, String token, String uploadUrl) {
        println("======================= 开始上传 =======================")
        CloseableHttpClient httpClient = HttpClients.createDefault()
        HttpPost firApiPost = new HttpPost(uploadUrl)
        RequestConfig config = RequestConfig
                .custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(20000)
                .build()
        firApiPost.setConfig(config)
        ProgressFileBody fileBody = new ProgressFileBody(file, ContentType.APPLICATION_OCTET_STREAM,
                "${extension.appName}${DOT_APK}")
        fileBody.setListener(new IStreamListener() {
            @Override
            void onProgress(Integer progress) {
                println("> Task :xe-upload-fir PROGRESS ==========> ${progress}%")
            }
        })
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
        ContentType contentType = ContentType.create("text/plain", Consts.UTF_8)
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        builder.setCharset(Consts.UTF_8)
        builder.addPart("file", fileBody)
        builder.addTextBody("key", key, contentType)
        builder.addTextBody("token", token, contentType)
        builder.addTextBody("x:name", "${extension.appName}", contentType)
        builder.addTextBody("x:version", "${extension.appName}", contentType)
        builder.addTextBody("x:build", "${extension.appVersion}", contentType)
        builder.addTextBody("x:changelog", "${extension.appChangeLog}", contentType)

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
                println("======================= 结束上传 =======================")
            } catch (IOException e) {
                println("${e.message}")
            }
        }

        return result
    }

    def isApiTokenValidated(String apiToken) {
        return apiToken != null && apiToken.length() > 0
    }

    /**
     * appName 是否有值
     * @param appName
     * @return true 没有值，false 有值
     */
    def isAppNameValidated(String appName) {
        return appName == null || appName.length() == 0
    }

    /**
     * appVersion 是否有值
     * @param appVersion
     * @return true 没有值，false 有值
     */
    def isAppVersionValidated(String appVersion) {
        return appVersion == null || appVersion.length() == 0
    }

}