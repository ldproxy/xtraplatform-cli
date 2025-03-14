
#ifdef __cplusplus 
extern "C" {

#endif
    typedef void (*progress_callback) (const char* msg);

    JNIEXPORT void JNICALL Java_de_ii_xtraplatform_cli_Cli_00024NativeProgress_update
    (JNIEnv *env2, jobject obj, jstring update);

    int xtracfg_init();

    void xtracfg_cleanup();

    void xtracfg_progress_subscribe(progress_callback progress_cb);

    char* xtracfg_execute(const char* command, int* err);

#ifdef __cplusplus
}
#endif
