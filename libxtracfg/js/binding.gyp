{
  'targets': [
    {
      'target_name': 'xtracfg-lib-native',
      'sources': [ 'src/xtracfg_lib.cc' ],
       'libraries': [ '/Users/pascal/Documents/GitHub/xtraplatform-cli/libxtracfg/c/build/libxtracfg.a' ],
      'include_dirs': ["<!@(node -p \"require('node-addon-api').include\")", "/Users/pascal/Documents/GitHub/xtraplatform-cli/libxtracfg/c/include"],
      'dependencies': ["<!(node -p \"require('node-addon-api').gyp\")"],
      'cflags!': [ '-fno-exceptions' ],
      'cflags_cc!': [ '-fno-exceptions' ],
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