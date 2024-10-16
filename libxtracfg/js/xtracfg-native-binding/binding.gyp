{
  'targets': [
    {
      'target_name': 'xtracfg-native',
      'sources': [ 'src/xtracfg_lib.cc' ],
       'libraries': [ '<(local_prefix)/../c/build/libxtracfg.a' ],
      'include_dirs': ["<!@(node -p \"require('node-addon-api').include\")", "<(local_prefix)/../c/include"],
      'dependencies': ["<!(node -p \"require('node-addon-api').gyp\")"],
      'cflags!': [ '-fno-exceptions', '-fPIC' ],
      'cflags_cc!': [ '-fno-exceptions', '-fPIC' ],
      'xcode_settings': {
        'GCC_ENABLE_CPP_EXCEPTIONS': 'YES',
        'CLANG_CXX_LIBRARY': 'libc++',
        'MACOSX_DEPLOYMENT_TARGET': '10.7'
      },
      'msvs_settings': {
        'VCCLCompilerTool': { 'ExceptionHandling': 1 },
      }
    }
  ]
}
