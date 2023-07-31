package de.ii.xtraplatform.cli;

public class XtraCfg {

  public static void main(String[] args) {
    try {
      CommandHandler commandHandler = new CommandHandler();

      Cli.execute(commandHandler);
    } catch (Throwable e) {
      System.out.println("ERROR " + e.getMessage());
    }
  }
}
