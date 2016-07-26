package com.ceabie.dexknife

import org.gradle.api.Project

/**
 * the spilt tools for plugin 1.3.0.
 *
 * @author ceabie
 */
public class SplitToolsFor130 extends DexSplitTools {

    public static boolean isCompat(Object variant) {
        try {
            if (variant != null) {
                variant.dex

                return true
            }
        } catch (RuntimeException e) {
//            e.printStackTrace()
        }

        return false
    }

    public static void processSplitDex(Project project, Object variant) {
        def dex = variant.dex
        if (dex.multiDexEnabled) {
            dex.inputs.file DEX_KNIFE_CFG_TXT

            dex.doFirst {
                startDexKnife()

                DexKnifeConfig dexKnifeConfig = getDexKnifeConfig(project)

                def scope = variant.getVariantData().getScope()
                File mergedJar = scope.jarMergingOutputFile
                File mappingFile = variant.mappingFile
                File andMainDexList = scope.mainDexListFile
                boolean minifyEnabled = variant.buildType.minifyEnabled

                if (processMainDexList(project, minifyEnabled, mappingFile, mergedJar,
                        andMainDexList, dexKnifeConfig)) {
                    if (dex.additionalParameters == null) {
                        dex.additionalParameters = []
                    }

                    dex.additionalParameters += '--main-dex-list=maindexlist.txt'
                    dex.additionalParameters += dexKnifeConfig.additionalParameters
                }

                endDexKnife()
            }
        }
    }
}