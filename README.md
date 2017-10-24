# ThirdPartUtils
第三方登录、分享的库封装，集成QQ、微信、微博三个最常用平台的登录、分享功能

登录：ThirdPartUtils.wxLogin()、ThirdPartUtils.qqLogin()、ThirdPartUtils.weiboLogin()

分享：ThirdPartUtils.wxShare()、ThirdPartUtils.qqShare()、ThirdPartUtils.weiboShare()

### 怎么用
1、把library目录扔进你的项目里并设置成Lib Module，修改根目录的build.gradle文件
```gradle
    allprojects {
        repositories {
            jcenter()
            maven { url "https://dl.bintray.com/thelasterstar/maven/" }
        }
    }
```
2、把ThirdPartUtils.java文件里标记todo的app key修改成自己项目在这三个平台上的key

3、把demo里的整个wxapi包和AndroidManifest里的WXEntryActivity声明copy到自己项目里（注意修改package）

4、在相应Activity里实现ThirdPartUtils.onActivityResult()方法

5、在使用登录、分享功能前调用一下ThirdPartUtils.init()

#### Tips
不提供gradle直接集成是因为代码很简单，你可以轻松弄懂并修改源码添加自定义功能，使之更适合自己的项目

> 微信sdk根据官方文档自动使用最新版本的

> QQ sdk为v3.3.0 lite

> 微博sdk为v4.1版（使用4.1.0版本的aar）