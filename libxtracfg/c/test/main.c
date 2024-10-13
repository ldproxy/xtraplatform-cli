 #include <stdio.h>
 #include <stdlib.h>

 #include "../include/libxtracfg.h"

 void progress (const char* msg) {
    printf("%s\n", msg);
 }
 
 int main(int argc, char **argv) {
    if (argc != 2) {
        fprintf(stderr, "Usage:   %s <json command>\n", argv[0]);
        fprintf(stderr, "Example: %s '{\"command\": \"info\", \"source\": \"/Users/az/development/configs-ldproxy/embed\"}'\n", argv[0]);
        exit(1);
    }

    xtracfg_init();
    xtracfg_progress_subscribe(progress);

    int err = 0;
    char *result = xtracfg_execute( argv[1], &err);

    xtracfg_cleanup();

    if (err > 0) {
        fprintf(stderr, "Unexpected error: %s\n", result);
        free(result);
        exit(1);
    }

    printf("%s\n", result);

    free(result);
 }

