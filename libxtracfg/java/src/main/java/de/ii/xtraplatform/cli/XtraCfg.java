package de.ii.xtraplatform.cli;

public class XtraCfg {

  /*public static void main(String[] args) {
    try {
      CommandHandler commandHandler = new CommandHandler();
      Cli.NativeProgress progress = new Cli.NativeProgress();

      Cli.execute(commandHandler, progress);
    } catch (Throwable e) {
      System.out.println("ERROR " + e.getMessage());
      e.printStackTrace();
    }
  }*/

  public static String execute(String command) {
    try {
      CommandHandler commandHandler = new CommandHandler();
      Cli.NativeProgress progress = new Cli.NativeProgress();

      return commandHandler.handleCommand(command, progress);
    } catch (Throwable e) {
      System.out.println("ERROR " + e.getMessage());
      e.printStackTrace();
      return "";
    }
  }
}
