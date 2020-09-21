# UploadApkPlugin


## 描述

之前转测的时候经常需要找到 apk 包再拖到 betaqr.com（fir.im）上面上传，再把链接给到测试。<br/>
后来发现他们有提供 api，就写了个插件简化这个过程<br/>
最近想了一下，这个插件或者可以支持不只一个内测分发平台，就尝试把蒲公英的上传也接入了 pgyer.com <br/>
另外把之前的配置参数替换成 map 的形式，并且把版本号也调整了，为了后面可以愉快的加版本号......<br/>

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
        classpath 'com.github.ZakAnun:UploadApkPlugin:1.0.0001' // 请使用最新版本~

    }
}
```
app build.gradle
```
apply plugin: 'uploadApk'

uploadApk {
	// product 是传厂商的名字，目前仅支持 fir 和 pgy...
    product = "fir"
    // fir 需要先通过接口获取上传的 url 和 token，所以提供了获取上传 url 的参数配置
    obtainUploadUrlParams = ['type': "android",
                             'bundle_id': "your application id",
                             'api_token': "your api token"]
    // 真正的上传参数（来源: https://www.betaqr.com/docs/publish)
    uploadParams = ['x:name': "you app name",
                    'x:version': "your app version",
                    'x:build': "your app build version",
                    'x:changelog': "your app changlog"]
// 下面是蒲公英的配置（来源: https://www.pgyer.com/doc/api#uploadApp）
//    product = "pgy"
//    uploadParams = ['uKey': "your uKey",
//                    '_api_key': "your _api_key",
//                    'installType': "the installType you want (optional)",
//                    'password': "this password you need (optional)",
//                    'updateDescription': "changlog (optional)",
//                    'channelShortcut': "channel (optional)"]
}
```

## 运行插件任务

方法一: 双击 build tab 下对应的 task <br/>
方法二: `./gradlew :app:assembleVariantNameUploadApk` （这里面的 variantName 表示是什么包）

## 可能的问题

1、执行完上传任务后，如果没有报错，但却没有把 apk 上传到指定到平台上的话，<br/>
需要检查是否已经在该网站上执行实名操作（插件也会提示）
2、demo 中直接执行上传是不会成功的，需要写入对应平台的信息~

欢迎与我联系，一起交流（linhenji@163.com）
