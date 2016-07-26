package com.ceabie.dexknife

import com.android.build.api.transform.Format
import com.android.build.api.transform.Transform
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.DexTransform
import org.gradle.api.Project
/**
 * the spilt tools for plugin 1.5.0.
 *
 * @author ceabie
 */
public class SplitToolsFor150 extends DexSplitTools {

    public static boolean isCompat() {
//         if (getAndroidPluginVersion() < 200) {
//             return true;
//         }

        return true;
    }

    public static void processSplitDex(Project project, Object variant) {
        TransformTask dexTask
//        TransformTask proGuardTask
        TransformTask jarMergingTask

        String name = variant.name.capitalize()
        boolean minifyEnabled = variant.buildType.minifyEnabled

        // find the task we want to process
        project.tasks.matching {
            ((it instanceof TransformTask) && it.name.endsWith(name)) // TransformTask
        }.each { TransformTask theTask ->
            Transform transform = theTask.transform
            String transformName = transform.name

//            if (minifyEnabled && "proguard".equals(transformName)) { // ProGuardTransform
//                proGuardTask = theTask
//            } else
            if (!minifyEnabled && "jarMerging".equals(transformName)) {
                jarMergingTask = theTask
            } else if ("dex".equals(transformName)) { // DexTransform
                dexTask = theTask
            }
        }

        if (dexTask != null && ((DexTransform) dexTask.transform).multiDex) {
            dexTask.inputs.file DEX_KNIFE_CFG_TXT

            dexTask.doFirst {
                startDexKnife()

                File mergedJar = null
                File mappingFile = variant.mappingFile
                DexTransform dexTransform = it.transform
                File fileAdtMainList = dexTransform.mainDexListFile

                DexKnifeConfig dexKnifeConfig = getDexKnifeConfig(project)

                // 非混淆的，从合并后的jar文件中提起mainlist；
                // 混淆的，直接从mapping文件中提取
                if (!minifyEnabled) {
                    if (jarMergingTask != null) {
                        Transform transform = jarMergingTask.transform
                        def outputProvider = jarMergingTask.outputStream.asOutput()
                        mergedJar = outputProvider.getContentLocation("combined",
                                transform.getOutputTypes(),
                                transform.getScopes(), Format.JAR)

                        println("DexKnife-From MergedJar: " + mergedJar)
                    }
                } else {
                    println("DexKnife-From Mapping: " + mappingFile)
                }

                if (processMainDexList(project, minifyEnabled, mappingFile, mergedJar,
                        fileAdtMainList, dexKnifeConfig)) {

                    // 替换 AndroidBuilder
                    MultiDexAndroidBuilder.proxyAndroidBuilder(dexTransform,
                            dexKnifeConfig.additionalParameters)

                    // 替换这个文件
                    fileAdtMainList.delete()
                    project.copy {
                        from 'maindexlist.txt'
                        into fileAdtMainList.parentFile
                    }
                }

                endDexKnife()
            }
        }
    }
}