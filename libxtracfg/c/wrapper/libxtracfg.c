#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include "../include/libxtracfg.h"

JavaVM *jvm = NULL;
JNIEnv *env = NULL;
progress_callback progress_cb = NULL;

// called from java with progress, pass to callback
JNIEXPORT void JNICALL Java_de_ii_xtraplatform_cli_Cli_00024NativeProgress_update
  (JNIEnv *env2, jobject obj, jstring update) {
    if (progress_cb != NULL) {
      const char* msg = (*env)->GetStringUTFChars(env, update, NULL);

      progress_cb(msg);
    }
  }

int xtracfg_init() {
  JavaVMInitArgs vm_args;
  JavaVMOption options[0];
  vm_args.version = JNI_VERSION_10;
  vm_args.nOptions = 0;
  vm_args.options = options;
  vm_args.ignoreUnrecognized = 0;

  jint res = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);

  if (JNI_OK != res) {
    printf("ERR\n");
    return 1;
  }

  return 0;
}

void xtracfg_cleanup() {
  if (jvm != NULL) { 
    (*jvm)->DestroyJavaVM(jvm);
  }
}

void xtracfg_progress_subscribe(progress_callback callback) {
  progress_cb = callback;
}

char* xtracfg_execute(const char* command, int* err) {
  if (jvm == NULL || env == NULL) {
    printf("ERR\n");
    *err = 1;
    return "Not intialized";
  }

  jclass cls = (*env)->FindClass(env, "de/ii/xtraplatform/cli/XtraCfg");
  jmethodID mid = (*env)->GetStaticMethodID(env, cls, "execute", "(Ljava/lang/String;)Ljava/lang/String;");
  //printf("JVM %d %d\n", cls, mid);

  jstring command2 = (*env)->NewStringUTF(env, command);

  jstring result2 = (jstring)(*env)->CallStaticObjectMethod(env, cls, mid, command2);

  (*env)->DeleteLocalRef(env, command2);

  const char* result = (*env)->GetStringUTFChars(env, result2, NULL);

  char *result3 = malloc(strlen(result) + 1);
  strcpy(result3,result);
  //printf("%s", result3);D");

  *err = 0;
  return result3;
 }
