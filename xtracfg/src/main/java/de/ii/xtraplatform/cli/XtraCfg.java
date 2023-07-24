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

  public static void main2(String[] args) {
    if (args.length == 0) {
      throw new IllegalArgumentException("No store directory given");
    }

    try {
      CommandHandler commandHandler = new CommandHandler();

      String parameters = "";

      for (int i = 1; i < args.length; i++) {
        if (args[i].startsWith("-")) {
          parameters += i == 1 ? "?" : "&";
          parameters += args[i].replaceAll("-", "").replace("src", "source");
          if (args.length <= i + 1 || args[i + 1].startsWith("-")) {
            parameters += "=true";
          }
        } else {
          parameters += "=" + args[i];
        }
      }

      System.out.println("ARGS " + parameters);

      String connect = "/connect" + parameters;

      String connectResult = commandHandler.handleCommand(connect);

      System.out.println(connectResult);

      String command = "/" + args[0] + parameters;

      String result = commandHandler.handleCommand(command);

      System.out.println(result);

    } catch (Throwable e) {
      System.out.println("ERROR " + e.getMessage());
      e.printStackTrace();
    }
  }
}
