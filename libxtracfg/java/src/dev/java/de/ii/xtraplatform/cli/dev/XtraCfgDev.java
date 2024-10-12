package de.ii.xtraplatform.cli.dev;

import de.ii.xtraplatform.cli.CommandHandler;
import de.ii.xtraplatform.cli.EntitiesHandler;

public class XtraCfgDev {

  public static void main(String[] args) {
    if (args.length == 0) {
      throw new IllegalArgumentException("No command given");
    }

    EntitiesHandler.DEV = true;
    String command =
        args[0]
            .replaceAll("\\{", "{\"")
            .replaceAll("\\}", "\"}")
            .replaceAll("\\:", "\":\"")
            .replaceAll("\\,", "\",\"");

    try {
      CommandHandler commandHandler = new CommandHandler();

      System.out.println("COMMAND: " + command);

      String result = commandHandler.handleCommand(command);

      System.out.println(result);

    } catch (Throwable e) {
      System.out.println("ERROR: " + command + " | " + e.getMessage());
      e.printStackTrace();
    }
  }
}
