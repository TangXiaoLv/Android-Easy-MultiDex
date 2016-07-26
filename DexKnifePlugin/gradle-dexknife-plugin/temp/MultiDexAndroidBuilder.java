package com.ceabie.dexknife;

import com.android.build.gradle.internal.transforms.DexTransform;
import com.android.builder.core.AndroidBuilder;
import com.android.builder.core.ErrorReporter;
import com.android.ide.common.process.JavaProcessExecutor;
import com.android.ide.common.process.ProcessExecutor;
import com.android.utils.ILogger;

import java.lang.reflect.Field;

/**
 * proxy the androidBuilder that plugin 1.5.0 to add '--minimal-main-dex' options.
 *
 * @author ceabie
 */
public class MultiDexAndroidBuilder extends AndroidBuilder {
    public MultiDexAndroidBuilder(String projectId,
                                  String createdBy,
                                  ProcessExecutor processExecutor,
                                  JavaProcessExecutor javaProcessExecutor,
                                  ErrorReporter errorReporter,
                                  ILogger logger,
                                  boolean verboseExec) {

        super(projectId, createdBy, processExecutor,
                javaProcessExecutor, errorReporter,
                logger, verboseExec);
    }

    public static void proxyAndroidBuilder(DexTransform transform) throws Exception {
        Field fieldAndroidBuilder = accessibleField(DexTransform.class, "androidBuilder");
        AndroidBuilder orgAndroidBuilder = (AndroidBuilder) fieldAndroidBuilder.get(transform);
        fieldAndroidBuilder.set(transform, getProxyAndroidBuilder(orgAndroidBuilder));
    }

    private static AndroidBuilder getProxyAndroidBuilder(AndroidBuilder orgAndroidBuilder) throws Exception {

        Field fieldProjectId = accessibleField(AndroidBuilder.class, "mProjectId");
        Field fieldCreatedBy = accessibleField(AndroidBuilder.class, "mCreatedBy");
        Field fieldJavaProcessExecutor = accessibleField(AndroidBuilder.class, "mJavaProcessExecutor");
        Field fieldVerboseExec = accessibleField(AndroidBuilder.class, "mVerboseExec");
        Field fieldLibraryRequests = accessibleField(AndroidBuilder.class, "mLibraryRequests");

        MultiDexAndroidBuilder myAndroidBuilder = new MultiDexAndroidBuilder(
                (String) fieldProjectId.get(orgAndroidBuilder),
                (String) fieldCreatedBy.get(orgAndroidBuilder),
                orgAndroidBuilder.getProcessExecutor(),
                (JavaProcessExecutor)fieldJavaProcessExecutor.get(orgAndroidBuilder),
                orgAndroidBuilder.getErrorReporter(),
                orgAndroidBuilder.getLogger(),
                (boolean) fieldVerboseExec.get(orgAndroidBuilder));

        myAndroidBuilder.setTargetInfo(
                orgAndroidBuilder.getSdkInfo(),
                orgAndroidBuilder.getTargetInfo(),
                fieldLibraryRequests.get(orgAndroidBuilder));

//        myAndroidBuilder.mBootClasspathFiltered = orgAndroidBuilder.mBootClasspathFiltered
//        myAndroidBuilder.mBootClasspathAll = orgAndroidBuilder.mBootClasspathAll

        return myAndroidBuilder;
    }

    private static Field accessibleField(Class cls, String field) throws NoSuchFieldException {
        Field f = cls.getDeclaredField(field);
        f.setAccessible(true);
        return f;
    }
}
