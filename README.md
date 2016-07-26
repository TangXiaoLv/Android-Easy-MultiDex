#Android傻瓜式分包插件
注1：不想看前半部分的话可以直接跳过到最下面配置部分。
注2：本插件是基于[DexKnifePlugin 1.5.2](https://github.com/ceabie/DexKnifePlugin)优化改造而来，感谢ceabie的无私奉献。

##填坑之路
`坑1：65536 ，So easy! `
*原因：*Dalvik 的 invoke-kind 指令集中，method reference index 只留了 16 bits，最多能引用 65535 个方法。
参考=>[由Android 65K方法数限制引发的思考](http://jayfeng.com/2016/03/10/%E7%94%B1Android-65K%E6%96%B9%E6%B3%95%E6%95%B0%E9%99%90%E5%88%B6%E5%BC%95%E5%8F%91%E7%9A%84%E6%80%9D%E8%80%83/).
*解决：*
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

`坑2：Too many classes in –main-dex-list ，what？`
*原因：*通过上面的官方分包，已经把原Dex分为1主Dex加多从Dex，主Dex保留4大组件，Application，Annotation，multidex等及其必要的直接依赖。由于我们方法数已达到16W之巨，上百个Activity，所以成功的把主Dex又撑爆了。
*解决：*
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

`坑3：gradle 1.5.0之后不支持这种写法 ，what the fuck？`
*原因：*官方解释Gralde`1.5.0`以上已经将(jacoco, progard, multi-dex)统一移到[Transform API](http://tools.android.com/tech-docs/new-build-system/transform-api)里，然而Transform API并没有想象的那么简单好用，最后翻遍Google终于找到一个兼容Gradle `1.5.0`以上的分包插件[DexKnifePlugin](https://github.com/ceabie/DexKnifePlugin)。
参考=>这篇[Android 热修复使用Gradle Plugin1.5改造Nuwa插件](http://blog.csdn.net/sbsujjbcy/article/details/50839263)比较好的介绍了Transform API的使用。

`坑4：NoClassDefFoundError ，are you kiding me？`
*原因：*通过插件手动指定main dex中要保留的类，虽然分包成功，但是main dex中的类及其直接引用类很难通过手动的方式指定。
*解决方式：*
看了[美团Android DEX自动拆包及动态加载简介](http://tech.meituan.com/mt-android-auto-split-dex.html),他们是通过编写了一个能够自动分析Class依赖的脚本去算出主Dex需要包含的所有必要依赖。看来依赖脚本是跑不掉了。

`坑5：自定义脚本 ，read the fuck source！`
插件的工作流程：



##配置部分
`第一步：`