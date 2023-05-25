#include "de_ii_xtraplatform_cli_Cli.h"
#include "client.h"
#include "libxtracfg.h"
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

const char* handle_command_2(handle_command_func handle_command_cb, const char* command) {
    return handle_command_cb(command);
}

JavaVM *javaVM;
JNIEnv* jniEnv; 
jobject handler;
jmethodID methodId;

const char* exec(const char* command) {
      //printf("JNI Command: %s\n", command);

      jint res = (*javaVM)->AttachCurrentThread(javaVM, (void**)&jniEnv, NULL);
      if (JNI_OK != res) {
          printf("JNI no thread");
          return NULL;
      }
      
      jstring command2 = (*jniEnv)->NewStringUTF(jniEnv, command);

      jstring result2 = (jstring)(*jniEnv)->CallObjectMethod(jniEnv, handler, methodId, command2);

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

JNIEXPORT void JNICALL Java_de_ii_xtraplatform_cli_Cli_execute
  (JNIEnv* jni, jclass thisObject, jobject commandHandler) {

    jint res = (*jni)->GetJavaVM(jni, &javaVM);
    if (JNI_OK != res) {
        printf("JNI no vm");
        return;
    }

    jniEnv = jni;
    handler = (*jni)->NewGlobalRef(jni, commandHandler);

    // Find the id of the Java method to be called
    jclass commandHandlerClass = (*jni)->GetObjectClass(jni, commandHandler);
    methodId = (*jni)->GetMethodID(jni, commandHandlerClass, "handleCommand", "(Ljava/lang/String;)Ljava/lang/String;");


    handle_command_func hc = &exec;

    cmd_execute(hc);
}
