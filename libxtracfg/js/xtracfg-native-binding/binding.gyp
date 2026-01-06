{
  'targets': [
    {
      'target_name': 'xtracfg-native',
      'sources': [ 'src/xtracfg_lib.cc' ],
      'include_dirs': ["<!@(node -p \"require('node-addon-api').include\")", "<(local_prefix)/../c/include"],
      'dependencies': ["<!(node -p \"require('node-addon-api').gyp\")"],
      'cflags': ['-fPIC'],
      'cflags!': [ '-fno-exceptions'],
      'cflags_cc': ['-fPIC'],
      'cflags_cc!': [ '-fno-exceptions' ],
      'xcode_settings': {
        'GCC_ENABLE_CPP_EXCEPTIONS': 'YES',
        'CLANG_CXX_LIBRARY': 'libc++',
        'MACOSX_DEPLOYMENT_TARGET': '14.0',
        "OTHER_CFLAGS": [ "-fPIC"]
      },
      'msvs_settings': {
        'VCCLCompilerTool': { 
          'RuntimeLibrary': 2, # /MD
          'ExceptionHandling': 1, 
          'AdditionalOptions': [
            '/MD',
            '/EHsc',
          ],
          },
        'VCLinkerTool': { 
          'IgnoreDefaultLibraryNames': [ 'LIBCMT' ],
          'AdditionalOptions': [
            '/WHOLEARCHIVE:<(local_prefix)/../c/build/libxtracfg.lib',
          ],
          },
      },
      'conditions': [
        ['OS=="win"', {
          'libraries': [ '<(local_prefix)/../c/build/libxtracfg.lib', '<(local_prefix)/../c/build/libxtracfgjni_static_ext.lib' ]
        }, { # OS != "win"
          'libraries': [ '<(local_prefix)/../c/build/libxtracfg.a' ]
        }],
      ]
    }
  ]
}
