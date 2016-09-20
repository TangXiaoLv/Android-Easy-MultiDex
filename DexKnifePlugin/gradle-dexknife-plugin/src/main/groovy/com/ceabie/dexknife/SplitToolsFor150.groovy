/*
 * Copyright (C) 2016 ceabie (https://github.com/ceabie/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ceabie.dexknife

import com.android.build.api.transform.Format
import com.android.build.api.transform.Transform
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.incremental.InstantRunBuildContext
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

    public static void processSplitDex(Project project, ApplicationVariant variant) {
        if (isInInstantRunMode(variant)) {
            System.err.println("DexKnife: Instant Run mode, DexKnife is auto disabled!")
            return
        }

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
            if ("jarMerging".equalsIgnoreCase(transformName)) {
                jarMergingTask = theTask
            } else if ("dex".equalsIgnoreCase(transformName)) { // DexTransform
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

                println("DexKnife Adt Main: " + fileAdtMainList)

                DexKnifeConfig dexKnifeConfig = getDexKnifeConfig(project)

                // 非混淆的，从合并后的jar文件中提起mainlist；
                // 混淆的，直接从mapping文件中提取
                if (minifyEnabled) {
                    println("DexKnife-From Mapping: " + mappingFile)
                } else {
                    if (jarMergingTask != null) {
                        Transform transform = jarMergingTask.transform
                        def outputProvider = jarMergingTask.outputStream.asOutput()
                        mergedJar = outputProvider.getContentLocation("combined",
                                transform.getOutputTypes(),
                                transform.getScopes(), Format.JAR)
                    }

                    println("DexKnife-From MergedJar: " + mergedJar)
                }

                if (processMainDexList(project, minifyEnabled, mappingFile, mergedJar,
                        fileAdtMainList, dexKnifeConfig)) {

                    int version = getAndroidPluginVersion(getAndroidGradlePluginVersion())
                    println("DexKnife: AndroidPluginVersion: " + version)

                    // after 2.2.0, it can additionalParameters, but it is a copy in task

                    // 替换 AndroidBuilder
                    InjectAndroidBuilder.proxyAndroidBuilder(dexTransform,
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

    private static boolean isInInstantRunMode(Object variant) {
        try {
            def scope = variant.getVariantData().getScope()
            InstantRunBuildContext instantRunBuildContext = scope.getInstantRunBuildContext()
            return instantRunBuildContext.isInInstantRunMode()
        } catch (Throwable e) {
        }

        return false
    }
}