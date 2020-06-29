# UploadFirPlugin


## 描述

之前转测的时候经常需要找到 apk 包再拖到 betaqr.com（fir.im）上面上传，再把链接给到测试。<br/>
后来发现他们有提供 api，就写了个插件简化这个过程

## 分析

这个上传的操作可以放到 build apk 之后进行，就可以省去一些我们去到该网站上的操作。

## 使用

根 build.gradle
```
buildscript {
   
    repositories {
       ...
        maven { url 'https://jitpack.io' }
    }
    dependencies {
      ...
        classpath 'com.github.zak-ml:UploadFirPlugin:1.0.1'

    }
}
```
app build.gradle
```
apply plugin: 'uploadFir'

uploadFir {
    appName = "Upload Demo" // 上传的 app 名字
    apiToken = "cc972e5b5ad466bccf83319bf46a7909" // apiToken 需要在 fir.im 申请（这是我的 api_token）
    appChangeLog = "测试上传" // 修改日志
}
```

---

欢迎与我联系，一起交流（linhenji@163.com）
