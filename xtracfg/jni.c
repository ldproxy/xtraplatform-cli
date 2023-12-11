#include "de_ii_xtraplatform_cli_Cli.h"
#include "client.h"
#include "libxtracfg.h"
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

JavaVM *javaVM;
JNIEnv* jniEnv; 
jobject handler;
jobject tracker;
jmethodID methodId;
progress_func progress_cb2;

// called from go, calls exec with command
// saves go progress callback, used in Java_de_ii_xtraplatform_cli_Cli_00024NativeProgress_update
const char* handle_command_2(handle_command_func handle_command_cb, const char* command, progress_func progress_cb) {
    progress_cb2 = progress_cb;

    return handle_command_cb(command);
}

// callback indirectly called from go with the command
// calls the java CommandHandler and returns the result to go
const char* exec(const char* command) {
      //printf("JNI Command: %s\n", command);

      jint res = (*javaVM)->AttachCurrentThread(javaVM, (void**)&jniEnv, NULL);
      if (JNI_OK != res) {
          printf("JNI no thread");
          return NULL;
      }
      
      jstring command2 = (*jniEnv)->NewStringUTF(jniEnv, command);

      jstring result2 = (jstring)(*jniEnv)->CallObjectMethod(jniEnv, handler, methodId, command2, tracker);

      (*jniEnv)->DeleteLocalRef(jniEnv, command2);

      const char* result = (*jniEnv)->GetStringUTFChars(jniEnv, result2, NULL);

      res = (*javaVM)->DetachCurrentThread(javaVM);
      if (JNI_OK != res) {
          printf("JNI no thread end");
          return NULL;
      }

      //printf("JNI Result: %s\n", result);

      return result;
    }

// entrypoint, called from java
JNIEXPORT void JNICALL Java_de_ii_xtraplatform_cli_Cli_execute
  (JNIEnv* jni, jclass thisObject, jobject commandHandler, jobject progress) {

    jint res = (*jni)->GetJavaVM(jni, &javaVM);
    if (JNI_OK != res) {
        printf("JNI no vm");
        return;
    }

    jniEnv = jni;
    handler = (*jni)->NewGlobalRef(jni, commandHandler);
    tracker = (*jni)->NewGlobalRef(jni, progress);

    // Find and save the id of the Java method to be called
    jclass commandHandlerClass = (*jni)->GetObjectClass(jni, commandHandler);
    methodId = (*jni)->GetMethodID(jni, commandHandlerClass, "handleCommand", "(Ljava/lang/String;Lde/ii/xtraplatform/cli/Progress;)Ljava/lang/String;");
    
    handle_command_func hc = &exec;

    // switch to go, pass exec above as callback
    cmd_execute(hc);

    (*jni)->DeleteGlobalRef(jni, handler);
    (*jni)->DeleteGlobalRef(jni, tracker);
}

// called from java with progress, pass to go callback
JNIEXPORT void JNICALL Java_de_ii_xtraplatform_cli_Cli_00024NativeProgress_update
  (JNIEnv *, jobject, jstring update) {
    const char* msg = (*jniEnv)->GetStringUTFChars(jniEnv, update, NULL);

    progress_cb2(msg);
  }
