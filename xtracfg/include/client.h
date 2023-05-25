#ifndef FUSION_H
#define FUSION_H

typedef const char* (*handle_command_func) (const char* command);

const char* handle_command_2(handle_command_func handle_command_cb, const char* command);

#endif
