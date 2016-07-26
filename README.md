#Android傻瓜式分包插件
注1：不想看前半部分的话可以直接跳过到最下面配置部分。  
注2：本插件是基于[DexKnifePlugin 1.5.2](https://github.com/ceabie/DexKnifePlugin)优化改造而来，感谢ceabie的无私奉献。

##填坑之路  
###坑1：65536 ，So easy!   
**原因：**Dalvik 的 invoke-kind 指令集中，method reference index 只留了 16 bits，最多能引用 65535 个方法。  
参考=>[由Android 65K方法数限制引发的思考](http://jayfeng.com/2016/03/10/%E7%94%B1Android-65K%E6%96%B9%E6%B3%95%E6%95%B0%E9%99%90%E5%88%B6%E5%BC%95%E5%8F%91%E7%9A%84%E6%80%9D%E8%80%83/).  

**解决：**  
```
dependencies { 
	compile 'com.android.support:MultiDex:1.0.1'
}
```
继承 Application ，重写 attachBaseContext(Context)
```
@Override 
protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    MultiDex.install(this);
}
```

###坑2：Too many classes in –main-dex-list ，what？  
**原因：**通过上面的官方分包，已经把原Dex分为1主Dex加多从Dex，主Dex保留4大组件，Application，Annotation，multidex等及其必要的直接依赖。由于我们方法数已达到16W之巨，上百个Activity，所以成功的把主Dex又撑爆了。  

**解决：**
gradle
```
afterEvaluate { 
  tasks.matching { 
    it.name.startsWith('dex') 
  }.each { dx -> 
    if (dx.additionalParameters == null) { 
      dx.additionalParameters = []
    }  
    dx.additionalParameters += '--set-max-idx-number=48000' 
  } 
}
```
参考=>[Android Dex分包之旅](http://yydcdut.com/2016/03/20/split-dex/index.html)

###坑3：gradle 1.5.0之后不支持这种写法 ，what the fuck？  
**原因：**官方解释Gralde`1.5.0`以上已经将(jacoco, progard, multi-dex)统一移到[Transform API](http://tools.android.com/tech-docs/new-build-system/transform-api)里，然而Transform API并没有想象的那么简单好用，最后翻遍Google终于找到一个兼容Gradle `1.5.0`以上的分包插件[DexKnifePlugin](https://github.com/ceabie/DexKnifePlugin)。  
参考=>这篇[Android 热修复使用Gradle Plugin1.5改造Nuwa插件](http://blog.csdn.net/sbsujjbcy/article/details/50839263)比较好的介绍了Transform API的使用。

###坑4：NoClassDefFoundError ，are you kiding me？  
**原因：**通过插件手动指定main dex中要保留的类，虽然分包成功，但是main dex中的类及其直接引用类很难通过手动的方式指定。  

**解决方式：**  
看了[美团Android DEX自动拆包及动态加载简介](http://tech.meituan.com/mt-android-auto-split-dex.html),他们是通过编写了一个能够自动分析Class依赖的脚本去算出主Dex需要包含的所有必要依赖。看来依赖脚本是跑不掉了。

###坑5：自定义脚本 ，read the fuck source！  
**问题一：**放进主Dex里应该有哪些类，规则是什么？  
查看sdk\build-tools\platform-version\mainDexClasses.rules发现应该放进主Dex类有Instrumentation，Application，Activity，Service，ContentProvider，BroadcastReceiver，BackupAgent的所有子类。

**问题二：**gradle是在哪里计算出主Dex依赖？  
查看Gradle编译任务发现有如下3个编译任务：  
<img src="png/2.png" height= "100" width="400">  

运行collect任务，发现会在build/multi-dex目录下单独生成manifest_keep.txt文件，该文件其实就是通过上述规则扫描AndroidManifest生成。manifest_keep.txt保留的是所有需要放入主Dex里的类。还没完，接下来transformClassesWithMultidexlist任务会根据manifest_keep.txt生成必要依赖列表maindexlist.txt，这里面所有类才是真正放入主Dex里的。bingo，思路已经非常清晰，我们只需要控制manifest_keep.txt的类，依赖关系由系统帮我们生成，即可控制主Dex大小和方法数，安全可靠！  

<img src="png/3.png" height = "200" width="500">  
<img src="png/4.png" height = "200" width="500"> 

**问题三：**在哪里控制maindexlist.txt的大小？  
由问题一我们知道生成manifest_keep.txt的规则，对于绝大部分工程来说，manifest_keep.txt中80%是Activity，其实我们只需要在主Dex中保留首页 Activity、Laucher Activity 、欢迎页的 Activity 等启动时必要的Activity就OK了。


下图是Gradle的工作流程：
<img src="png/1.png" width="800">  
来源：[深入理解Android之Gradle](http://blog.csdn.net/innost/article/details/48228651)  

我们只需要在完成任务向量图之后，执行任务之前Hook一下collect任务，做下activity过滤就OK了，添加Gradle：
```
//需要加入主dex的Activity列表
def mainDexListActivity = ['WelcomeActivity', 'MainFunctionActivity']
afterEvaluate {
    project.tasks.each { task ->
        if (task.name.startsWith('collect') && task.name.endsWith('MultiDexComponents')) {
            println "main-dex-filter: found task $task.name"
            task.filter { name, attrs ->
                String componentName = attrs.get('android:name')
                if ('activity'.equals(name)) {
                    def result = mainDexListActivity.find {
                        componentName.endsWith("${it}")
                    }
                    return result != null
                } else {
                    return true
                }
            }
        }
    }
}
```

###坑6：主dex依然爆表，shit again！  
其实上面那段脚本已经成功筛选出我们想要的主Dex的manifest_keep和maindexlist，只是不知道为什么还是把所有类打进主Dex。这个时候就需要跟[DexKnifePlugin](https://github.com/ceabie/DexKnifePlugin)插件配合使用，首先在gradle中加上上述脚本，然后使用插件时在配置文件里加上 `-split **.**`和`#-donot-use-suggest`

###Congratulation
恭喜，填坑终于结束，不过还有点不爽的是需要同时维护Gradle脚本和插件的配置。
于是就将Gradle脚本整合进了插件，以后只要维护一个配置文件就行了。由于带有点业务特性，于是就单独开了个项目，读者可以根据自己需求自己选择。以下是整合插件的配置。

##配置部分
**第一步：将repo目录复制到项目根目录**  

**第二步：添加根目录Gradle**
```
buildscript {
    repositories {
        maven { 
			url uri('repo')
		}
    }

    dependencies {
        classpath 'com.ceabie.dextools:gradle-dexknife-plugin:2.0.0'
    }
}

```
**第三步：在你的App模块的build.gradle添加插件**
```
apply plugin: 'com.ceabie.dexnkife'
```

**第四步：在你的App模块目录下新建dexknife.txt，并自定义配置**
```
#为注释符

#-----------主Dex中必要依赖的脚本配置-----------
#默认保留四大组件中其他三大组件，Activity组件选择性保留(使用-just activity 选项),若为空默认保留所有Activity
-just activity com.ceabie.demo.MainActivity

#-----------附加类-----------
# 如果你想要某个包路径在maindex中，则使用 -keep 选项，即使他已经在分包的路径中.若为空，默认保留所有

# 保留单个类.
#-keep android.support.v7.app.AppCompatDialogFragment.class

# 这条配置可以指定这个包下类在第二及其他dex中.
#android.support.v?.**
#将全部类移出主Dex
-split **.**

# 不包含Android gradle 插件自动生成的miandex列表.(不用系统自带分包策略)
#-donot-use-suggest

# 不进行dex分包， 直到 dex 的id数量超过 65536.(设置自动执行分包策略)
#-auto-maindex

# 显示miandex的日志.
#-log-mainlist

```

**第五步：在 defaultConfig 或者 buildTypes中打开 multiDexEnabled true，否则不起作用**

##已知错误

**错误1：**
```
Error:Execution failed for task ':Toon:transformClassesWithDexForDebug'.> java.lang.NullPointerException (no error message)
```
发生此错误只要切换一次Gradle版本就OK了，比如1.5.0

**错误2：**
```
Unsupported major.minor version 52.0
```
将JDK升级到1.8
