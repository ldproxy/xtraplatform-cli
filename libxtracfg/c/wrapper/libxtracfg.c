#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>

char* xtracfg_execute(const char* command, int* err) {
  JavaVM *jvm;
  JNIEnv *env;
  JavaVMInitArgs vm_args;
  JavaVMOption options[0];
  vm_args.version = JNI_VERSION_10;
  vm_args.nOptions = 0;
  vm_args.options = options;
  vm_args.ignoreUnrecognized = 0;

  jint res = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);
  if (JNI_OK != res) {
    printf("ERR\n");
    *err = 1;
    return "Could not create JVM";
  }

  jclass cls = (*env)->FindClass(env, "de/ii/xtraplatform/cli/CommandHandler");
  jmethodID cid = (*env)->GetMethodID(env, cls, "<init>", "()V"); 
  jmethodID mid = (*env)->GetMethodID(env, cls, "handleCommand", "(Ljava/lang/String;)Ljava/lang/String;");

  //printf("JVM %d %d %d\n", cls, cid, mid);

  jobject ch = (*env)->NewObject(env, cls, cid);

  jstring command2 = (*env)->NewStringUTF(env, command);

  jstring result2 = (jstring)(*env)->CallObjectMethod(env, ch, mid, command2);

  (*env)->DeleteLocalRef(env, command2);

  const char* result = (*env)->GetStringUTFChars(env, result2, NULL);

  char *result3 = malloc(strlen(result) + 1);
  strcpy(result3,result);
  //printf("%s", result3);

  (*jvm)->DestroyJavaVM(jvm);

  *err = 0;
  return result3;
 }

