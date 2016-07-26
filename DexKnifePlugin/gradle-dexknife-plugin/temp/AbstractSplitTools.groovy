package com.ceabie.dexknife

import org.gradle.api.Project


/**
 * the base of spilt tools.
 *
 * @author ceabie
 */
public abstract class AbstractSplitTools {
    protected Project mProject;

    public AbstractSplitTools(Project project) {
        mProject = project
    }

    public abstract void processSplitDex(Object variant);

    public static void processMainDexList(Project project, Object variant, File mergedJar) {
        genMainDexList(project, variant, getSecondPackages(project), mergedJar)
    }

    protected void processMainDexList(Object variant, File mergedJar) {
        genMainDexList(mProject, variant, getSecondPackages(mProject), mergedJar)
    }

    /**
     * 获得第二个分包的类过滤列表
     */
    private static ArrayList<String> getSecondPackages(Project project) {
        ArrayList<String> secDexPackages = new ArrayList<>()
        project.file("second_dex_package_list.txt").withReader { r ->
            for (it in r.readLines()) {
                String s = it.trim()
                if (s.size() > 0) {
                    secDexPackages.add(s)
                }
            }
        }

        return secDexPackages
    }

    /**
     * 生成主dex的类列表
     */
    private static void genMainDexList(Project project, Object variant,
                                         List<String> secDexPackages, File mergedJar) {
        println ":genMainDexList"

        String[] secPackages = null
        HashSet<String> secPackageSet = null
        def scope = variant.getVariantData().getScope()

        if (variant.buildType.minifyEnabled) {
            // 从mapping文件中收集混淆后的 class
            File mapping = variant.mappingFile;
            secPackageSet = getClassesFromMapping(mapping, secDexPackages)

            if (mergedJar == null) {
                mergedJar = scope.proguardOutputFile
            }
        } else {
            def size = secDexPackages.size()
            secPackages = new String[size]
            for (int i = 0; i < size; i++) {
                secPackages[i] = secDexPackages.get(i).replace('.', '/')
            }

            if (mergedJar == null) {
                // multi-dex/allclasses.jar
                mergedJar = scope.jarMergingOutputFile
            }
        }

        File keepFile = project.file("maindexlist.txt")
        keepFile.delete()

        // 获得 ADT 推荐的 maindexlist
        File andMainDexList = scope.mainDexListFile
        HashSet<String> mainCls = getAdtMainDexClasses(andMainDexList);

        def clsfile = new java.util.zip.ZipFile(mergedJar)

        for (entry in clsfile.entries()) {
            String entryName = entry.getName()

            if (entryName.endsWith(".class")) {
                boolean isSecDex = false
                if (secPackageSet != null) {
                    isSecDex = secPackageSet.contains(entryName)
                } else if (secPackages != null) {
                    for (String pack : secPackages) {
                        if (entryName.startsWith(pack)) {
                            isSecDex = true
                            break
                        }
                    }
                }

                // 如果ADT的类在主dex，则不放在第二个dex
                if (isSecDex && mainCls != null && mainCls.contains(entryName)) {
                    isSecDex = false;
                }

                if (!isSecDex) {
                    keepFile.withWriterAppend { w ->
                        w << entryName << '\n'
                    }
                } else {
                    println entryName
                }
            }

        }

        clsfile.close()
    }

    private static HashSet<String> getClassesFromMapping(File mapping, List<String> secDexPackages) {
        HashSet<String> secPackSet = new HashSet<>();
        mapping.withReader { r ->
            for (line in r.readLines()) {
                for (it in secDexPackages) {
                    if (line.startsWith(it)) {
                        int ip = line.indexOf("-> ")
                        if (ip != -1) {
                            def sMap = line.substring(ip + 3, line.length() - 1)
                            secPackSet.add(sMap.replace('.', '/') + ".class")
                        }
                    }
                }
            }
        }

        return secPackSet;
    }

    /**
    * 获取系统推荐在主dex的列表
    */
    private static HashSet<String> getAdtMainDexClasses(File outputDir) {
        HashSet<String> mainCls = new HashSet<>()
        outputDir.withReader { r ->
            for (it in r.readLines()) {
                if (it.endsWith(".class")) {
                    mainCls.add(it)
                }
            }
        }

        if (mainCls.size() == 0) {
            mainCls = null
        }

        return mainCls
    }
}