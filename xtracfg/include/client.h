#ifndef FUSION_H
#define FUSION_H

typedef const char* (*handle_command_func) (const char* command);

typedef void (*progress_func) (const char* msg);

const char* handle_command_2(handle_command_func handle_command_cb, const char* command, progress_func progress_cb);

void progress(char* msg);

#endif
