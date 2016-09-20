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

import com.android.build.gradle.internal.transforms.DexTransform
import com.android.builder.core.AndroidBuilder
import com.android.builder.core.DexOptions
import com.android.builder.core.ErrorReporter
import com.android.builder.sdk.TargetInfo
import com.android.ide.common.process.JavaProcessExecutor
import com.android.ide.common.process.ProcessException
import com.android.ide.common.process.ProcessExecutor
import com.android.ide.common.process.ProcessOutputHandler
import com.android.utils.ILogger
import groovy.transform.CompileStatic

import java.lang.reflect.Field

/**
 * proxy the androidBuilder that plugin 1.5.0 to add '--minimal-main-dex' options.
 *
 * @author ceabie
 */
public class InjectAndroidBuilder extends AndroidBuilder {

    Collection<String> mAddParams;
    AndroidBuilder mAndroidBuilder;

    public InjectAndroidBuilder(String projectId,
                                String createdBy,
                                ProcessExecutor processExecutor,
                                JavaProcessExecutor javaProcessExecutor,
                                ErrorReporter errorReporter,
                                ILogger logger,
                                boolean verboseExec) {
        super(projectId, createdBy, processExecutor, javaProcessExecutor, errorReporter, logger, verboseExec)
    }

//    @Override // for < 2.2.0
    public void convertByteCode(Collection<File> inputs,
                                File outDexFolder,
                                boolean multidex,
                                File mainDexList,
                                DexOptions dexOptions,
                                List<String> additionalParameters,
                                boolean incremental,
                                boolean optimize,
                                ProcessOutputHandler processOutputHandler)
            throws IOException, InterruptedException, ProcessException {

        println("DexKnife: convertByteCode before 2.2.0")
        if (mAddParams != null) {
            if (additionalParameters == null) {
                additionalParameters = new ArrayList<>()
            }

            mergeParams(additionalParameters, mAddParams)
        }

        // groovy call super has bug
        mAndroidBuilder.convertByteCode(inputs, outDexFolder, multidex, mainDexList, dexOptions,
                additionalParameters, incremental, optimize, processOutputHandler);
    }

//    @Override for >= 2.2.0
    public void convertByteCode(Collection<File> inputs,
                                File outDexFolder,
                                boolean multidex,
                                File mainDexList,
                                final DexOptions dexOptions,
                                boolean optimize,
                                ProcessOutputHandler processOutputHandler)
            throws IOException, InterruptedException, ProcessException {

        println("DexKnife:convertByteCode after 2.2.0")

        DexOptions dexOptionsProxy = dexOptions

        if (mAddParams != null) {
            List<String> additionalParameters = dexOptions.getAdditionalParameters()
            if (additionalParameters == null) {
                additionalParameters = new ArrayList<>()
            }

            mergeParams(additionalParameters, mAddParams)
        }

        mAndroidBuilder.convertByteCode(inputs, outDexFolder, multidex, mainDexList, dexOptionsProxy,
                optimize, processOutputHandler);
    }

    @CompileStatic
    @Override
    List<File> getBootClasspath(boolean includeOptionalLibraries) {
        return mAndroidBuilder.getBootClasspath(includeOptionalLibraries)
    }

    @CompileStatic
    @Override
    List<String> getBootClasspathAsStrings(boolean includeOptionalLibraries) {
        return mAndroidBuilder.getBootClasspathAsStrings(includeOptionalLibraries)
    }


    @CompileStatic
    static void mergeParams(List<String> additionalParameters, Collection<String> addParams) {
        List<String> mergeParam = new ArrayList<>()
        for (String param : addParams) {
            if (!additionalParameters.contains(param)) {
                mergeParam.add(param)
            }
        }

        if (mergeParam.size() > 0) {
            additionalParameters.addAll(mergeParam)
        }
    }


    public static void proxyAndroidBuilder(DexTransform transform, Collection<String> addParams) {
        if (addParams != null && addParams.size() > 0) {
            accessibleField(DexTransform.class, "androidBuilder")
                    .set(transform, getProxyAndroidBuilder(transform.androidBuilder, addParams))
        }
    }

    private static AndroidBuilder getProxyAndroidBuilder(AndroidBuilder orgAndroidBuilder,
                                                         Collection<String> addParams) {
        InjectAndroidBuilder myAndroidBuilder = new InjectAndroidBuilder(
                orgAndroidBuilder.mProjectId,
                orgAndroidBuilder.mCreatedBy,
                orgAndroidBuilder.getProcessExecutor(),
                orgAndroidBuilder.mJavaProcessExecutor,
                orgAndroidBuilder.getErrorReporter(),
                orgAndroidBuilder.getLogger(),
                orgAndroidBuilder.mVerboseExec)

        // if >= 2.2.0
        def to = myAndroidBuilder.respondsTo("setTargetInfo", TargetInfo.class)
        if (to.size() > 0) {
            myAndroidBuilder.setTargetInfo(orgAndroidBuilder.getTargetInfo())
            myAndroidBuilder.setSdkInfo(orgAndroidBuilder.getSdkInfo())
            myAndroidBuilder.setLibraryRequests(orgAndroidBuilder.mLibraryRequests)
        } else {
            myAndroidBuilder.setTargetInfo(
                    orgAndroidBuilder.getSdkInfo(),
                    orgAndroidBuilder.getTargetInfo(),
                    orgAndroidBuilder.mLibraryRequests)
        }

        myAndroidBuilder.mAddParams = addParams
        myAndroidBuilder.mAndroidBuilder = orgAndroidBuilder
//        myAndroidBuilder.mBootClasspathFiltered = orgAndroidBuilder.mBootClasspathFiltered
//        myAndroidBuilder.mBootClasspathAll = orgAndroidBuilder.mBootClasspathAll

        return myAndroidBuilder
    }

    @CompileStatic
    private static Field accessibleField(Class cls, String field) {
        Field f = cls.getDeclaredField(field)
        f.setAccessible(true)
        return f
    }
}
