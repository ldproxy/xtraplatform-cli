 #include <stdio.h>
 #include <stdlib.h>

 #include "../include/libxtracfg.h"

 int main(int argc, char **argv) {
    if (argc != 2) {
        fprintf(stderr, "Usage:   %s <json command>\n", argv[0]);
        fprintf(stderr, "Example: %s '{\"command\": \"info\", \"source\": \"/Users/az/development/configs-ldproxy/embed\"}'\n", argv[0]);
        exit(1);
    }

    int err = 0;
    char *result = xtracfg_execute( argv[1], &err);

    if (err > 0) {
        fprintf(stderr, "Unexpected error: %s\n", result);
        free(result);
        exit(1);
    }

    printf("%s\n", result);

    free(result);
 }

