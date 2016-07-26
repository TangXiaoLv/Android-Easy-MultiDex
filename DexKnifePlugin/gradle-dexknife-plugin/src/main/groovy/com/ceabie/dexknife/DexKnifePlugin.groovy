package com.ceabie.dexknife

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * the spilt tools plugin.
 *
 * @author ceabie
 */
public class DexKnifePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        //过滤Activity组件
        project.afterEvaluate {
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

        project.afterEvaluate {
            for (variant in project.android.applicationVariants) {
                if (isMultiDexEnabled(variant)) {
                    if (SplitToolsFor130.isCompat(variant)) {
                        SplitToolsFor130.processSplitDex(project, variant)
                    } else if (SplitToolsFor150.isCompat()) {
                        SplitToolsFor150.processSplitDex(project, variant)
                    } else {
                        println("DexKnife Error: Android gradle plugin only < 2.0.0.");
                    }
                } else {
                    println("DexKnife : MultiDexEnabled is false, it's not work.");
                }
            }
        }
    }

    private static boolean isMultiDexEnabled(variant) {
        def is = variant.buildType.multiDexEnabled
        if (is != null) {
            return is;
        }

        is = variant.mergedFlavor.multiDexEnabled
        if (is != null) {
            return is;
        }

        return false
    }

}