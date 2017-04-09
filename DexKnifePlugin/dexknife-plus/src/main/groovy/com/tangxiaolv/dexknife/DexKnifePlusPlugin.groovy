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
package com.tangxiaolv.dexknife

import com.ceabie.dexknife.DexKnifePlugin
import com.ceabie.dexknife.DexSplitTools
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * the spilt tools plugin.
 */
public class DexKnifePlusPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        DexKnifeExtension dexKnifeExtension = project.extensions.create('dexKnife', DexKnifeExtension)
        project.afterEvaluate {
            boolean hasApp = project.plugins.hasPlugin("com.android.application")
            print("-hasApp = ${hasApp}\n")
            if (!hasApp) {
                throw new IllegalStateException("'android' plugin required.")
            }

            final def variants = project.android.applicationVariants
            variants.each{variant ->
                if (dexKnifeExtension.enabled) {
                    boolean checkProductFlavor = dexKnifeExtension.productFlavor == "" || dexKnifeExtension.productFlavor.equalsIgnoreCase(variant.flavorName)
                    boolean checkBuildType = dexKnifeExtension.buildType == "" || dexKnifeExtension.buildType.equalsIgnoreCase(variant.buildType.name)
                    if (checkProductFlavor && checkBuildType) {
                        printf "-DexKnifePlugin Enable = true\n";
                        printf "-DexKnifePlugin checkProductFlavor = ${checkProductFlavor}\n";
                        printf "-DexKnifePlugin checkBuildType = ${checkBuildType}\n";
                        printf "-DexKnifePlugin buildType.name = ${variant.buildType.name}\n";
                        printf "-DexKnifePlugin flavorName = ${variant.flavorName}\n";

                        filterActivity(project);

                        DexKnifePlugin.dexKnifeProcessVariant(project, variant)
                    }
                } else {
                    printf "-DexKnifePlugin Enable = false\n";
                }
            }
        }
    }

    //filter Activity
    private static void filterActivity(Project project) {
        File file = project.file(DexSplitTools.DEX_KNIFE_CFG_TXT)
        if (file != null) {
            def justActivitys = [];
            file.eachLine { line ->
                //printf "read line ${line}\n";
                if (line.startsWith('-just activity')) {
                    line = line.replaceAll('-just activity', '').trim();
                    justActivitys.add(line)
                }
            }
            printf "-just activity size = ${justActivitys.size()}\n";
            if (justActivitys.size() != 0) {
                project.tasks.each { task ->
                    if (task.name.startsWith('collect') && task.name.endsWith('MultiDexComponents')) {
                        println "main-dex-filter: found task $task.name"
                        task.filter { name, attrs ->
                            String componentName = attrs.get('android:name')
                            if ('activity'.equals(name)) {
                                def result = justActivitys.find {
                                    componentName.endsWith("${it}")
                                }
                                def bool = result != null;
                                if (bool) {
                                    printf "main-dex-filter: keep ${componentName}\n"
                                }
                                return bool
                            }
                            return true
                        }
                    }
                }
            }
        }
    }
}