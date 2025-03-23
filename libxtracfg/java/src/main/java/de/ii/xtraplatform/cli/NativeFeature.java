package de.ii.xtraplatform.cli;

import com.oracle.svm.core.jdk.NativeLibrarySupport;
import com.oracle.svm.core.jdk.PlatformNativeLibrarySupport;
import com.oracle.svm.hosted.FeatureImpl;
import com.oracle.svm.hosted.c.NativeLibraries;
import org.graalvm.nativeimage.hosted.Feature;

// @AutomaticFeature
public class NativeFeature implements Feature {

  @Override
  public void beforeAnalysis(BeforeAnalysisAccess access) {
    // Treat "Native" as a built-in library.
    NativeLibrarySupport.singleton().preregisterUninitializedBuiltinLibrary("xtracfg");
    // Treat JNI calls in "HelloWorld" as calls to built-in library.
    PlatformNativeLibrarySupport.singleton()
        .addBuiltinPkgNativePrefix("de_ii_xtraplatform_cli_Cli");
    NativeLibraries nativeLibraries =
        ((FeatureImpl.BeforeAnalysisAccessImpl) access).getNativeLibraries();
    // Add "jvm" as a dependency to "Native".
    nativeLibraries.addStaticJniLibrary("xtracfg");
  }
}
