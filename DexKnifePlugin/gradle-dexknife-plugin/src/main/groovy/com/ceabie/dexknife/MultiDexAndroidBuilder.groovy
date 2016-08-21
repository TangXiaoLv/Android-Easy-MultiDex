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
import com.android.ide.common.process.JavaProcessExecutor
import com.android.ide.common.process.ProcessException
import com.android.ide.common.process.ProcessExecutor
import com.android.ide.common.process.ProcessOutputHandler
import com.android.utils.ILogger

import java.lang.reflect.Field

/**
 * proxy the androidBuilder that plugin 1.5.0 to add '--minimal-main-dex' options.
 *
 * @author ceabie
 */
public class MultiDexAndroidBuilder extends AndroidBuilder {

    Collection<String> mAddParams;

    public MultiDexAndroidBuilder(String projectId,
                                  String createdBy,
                                  ProcessExecutor processExecutor,
                                  JavaProcessExecutor javaProcessExecutor,
                                  ErrorReporter errorReporter,
                                  ILogger logger,
                                  boolean verboseExec) {
        super(projectId, createdBy, processExecutor, javaProcessExecutor, errorReporter, logger, verboseExec)
    }

    @Override
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

        if (mAddParams != null) {
            if (additionalParameters == null) {
                additionalParameters = []
            }

            additionalParameters += mAddParams //'--minimal-main-dex'
        }

        super.convertByteCode(inputs, outDexFolder, multidex, mainDexList, dexOptions,
                additionalParameters, incremental, optimize, processOutputHandler)
    }

//    public static void proxyAndroidBuilder(TransformTask task) {
//        task.setAndroidBuilder(getProxyAndroidBuilder(task.getBuilder()))
//    }

    public static void proxyAndroidBuilder(DexTransform transform, Collection<String> addParams) {
        if (addParams != null && addParams.size() > 0) {
            accessibleField(DexTransform.class, "androidBuilder")
                    .set(transform, getProxyAndroidBuilder(transform.androidBuilder, addParams))
        }
    }

    private static AndroidBuilder getProxyAndroidBuilder(AndroidBuilder orgAndroidBuilder,
                                                         Collection<String> addParams) {
        MultiDexAndroidBuilder myAndroidBuilder = new MultiDexAndroidBuilder(
                orgAndroidBuilder.mProjectId,
                orgAndroidBuilder.mCreatedBy,
                orgAndroidBuilder.getProcessExecutor(),
                orgAndroidBuilder.mJavaProcessExecutor,
                orgAndroidBuilder.getErrorReporter(),
                orgAndroidBuilder.getLogger(),
                orgAndroidBuilder.mVerboseExec)

        myAndroidBuilder.setTargetInfo(
                orgAndroidBuilder.getSdkInfo(),
                orgAndroidBuilder.getTargetInfo(),
                orgAndroidBuilder.mLibraryRequests)

        myAndroidBuilder.mAddParams = addParams
//        myAndroidBuilder.mBootClasspathFiltered = orgAndroidBuilder.mBootClasspathFiltered
//        myAndroidBuilder.mBootClasspathAll = orgAndroidBuilder.mBootClasspathAll

        myAndroidBuilder
    }

    private static Field accessibleField(Class cls, String field) {
        Field f = cls.getDeclaredField(field)
        f.setAccessible(true)
        f
    }
}
