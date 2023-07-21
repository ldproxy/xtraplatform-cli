package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.JacksonSubTypes;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.JacksonProvider;
import de.ii.xtraplatform.store.app.ValueEncodingJackson;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import shadow.com.fasterxml.jackson.databind.ObjectMapper;

public class XtraCfg {

  static {
    // System.load("/src/dist/libxtracfg.so");
    // System.loadLibrary("xtracfg");
  }

  public static void main2(String[] args) {
    try {
      Jackson jackson = new JacksonProvider(JacksonSubTypes::ids, false);
      ObjectMapper mapper =
          (new ValueEncodingJackson(jackson, false)).getMapper(ValueEncoding.FORMAT.JSON);
      CommandHandler commandHandler = new CommandHandler(mapper);

      Cli.execute(commandHandler);
    } catch (Throwable e) {
      System.out.println("ERROR " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      throw new IllegalArgumentException("No store directory given");
    }

    try {
      Jackson jackson = new JacksonProvider(JacksonSubTypes::ids, false);
      ObjectMapper mapper =
          (new ValueEncodingJackson(jackson, false)).getMapper(ValueEncoding.FORMAT.JSON);
      CommandHandler commandHandler = new CommandHandler(mapper);

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
    }
  }
}
