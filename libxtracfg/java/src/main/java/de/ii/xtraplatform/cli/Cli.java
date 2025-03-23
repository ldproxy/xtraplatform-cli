package de.ii.xtraplatform.cli;

public class Cli {

  //public static native void execute(CommandHandler commandHandler, Progress progress);

  static class NativeProgress implements Progress {
    @Override
    public native void update(String progress);
  }
}
