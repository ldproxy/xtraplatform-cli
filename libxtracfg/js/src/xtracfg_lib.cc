#include <napi.h>
#include <libxtracfg.h>

using namespace Napi;

// Globale Variable für die Callback-Referenz
Napi::FunctionReference callbackRef;

void Progress(const char* progress) {
  Napi::Env env = callbackRef.Env();

  // Rufe die gespeicherte JavaScript-Funktion mit einem String auf
  callbackRef.Call(env.Global(), { Napi::String::New(env, progress) });
}


Napi::String ExecuteCommand(const Napi::CallbackInfo& info) {
  Napi::Env env = info.Env();

  // Überprüfen Sie die Anzahl der Argumente
  if (info.Length() < 1 || !info[0].IsString()) {
    Napi::TypeError::New(env, "String expected").ThrowAsJavaScriptException();
    return Napi::String::New(env, "");
  }

  // Holen Sie sich das Argument
  std::string command = info[0].As<Napi::String>().Utf8Value();

  // Rufen Sie die externe Funktion auf
  int err;
  char* result2 = xtracfg_execute(command.c_str(), &err);

  // Überprüfen Sie auf Fehler
  if (err != 0) {
    Napi::Error::New(env, "Error executing command").ThrowAsJavaScriptException();
    return Napi::String::New(env, "");
  }

  // Erstellen Sie das Ergebnis als Napi::String
  Napi::String result = Napi::String::New(env, result2);

  // Geben Sie den Speicher frei
  free(result2);

  return result;
}

void Subscribe(const Napi::CallbackInfo& info) {
  Napi::Env env = info.Env();

  // Überprüfen, ob das Argument eine Funktion ist
  if (!info[0].IsFunction()) {
    Napi::TypeError::New(env, "Function expected").ThrowAsJavaScriptException();
    return;
  }

  // Speichere die JavaScript-Funktion in der globalen Variable
  callbackRef = Napi::Persistent(info[0].As<Napi::Function>());

    // Registriere die Progress-Funktion bei der Bibliothek
  xtracfg_progress_subscribe(Progress);
}



void Cleanup(void* arg) {
  xtracfg_cleanup();
}


Napi::Object Init(Napi::Env env, Napi::Object exports) {
  xtracfg_init();

  napi_add_env_cleanup_hook(env, Cleanup, nullptr);

  exports.Set(Napi::String::New(env, "xtracfgLib"),
              Napi::Function::New(env, ExecuteCommand));
  exports.Set(Napi::String::New(env, "subscribe"),
              Napi::Function::New(env, Subscribe));
  return exports;
}

NODE_API_MODULE(addon, Init)

