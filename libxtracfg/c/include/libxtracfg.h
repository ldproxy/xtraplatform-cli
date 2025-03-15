#include <jni.h>

#ifndef _Included_libxtracfg
#define _Included_libxtracfg
#ifdef __cplusplus 
extern "C" {
#endif

    typedef void (*progress_callback) (const char* msg);

    JNIEXPORT void JNICALL Java_de_ii_xtraplatform_cli_Cli_00024NativeProgress_update
    (JNIEnv *, jobject, jstring);

    int xtracfg_init();

    void xtracfg_cleanup();

    void xtracfg_progress_subscribe(progress_callback progress_cb);

    char* xtracfg_execute(const char* command, int* err);

#ifdef __cplusplus
}
#endif
#endif
