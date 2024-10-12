
#ifdef __cplusplus 
extern "C" {

#endif
    typedef void (*progress_callback) (const char* msg);

    int xtracfg_init();

    void xtracfg_cleanup();

    void xtracfg_progress_subscribe(progress_callback progress_cb);

    char* xtracfg_execute(const char* command, int* err);

#ifdef __cplusplus
}
#endif
