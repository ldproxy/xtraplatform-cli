package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.JacksonSubTypes;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.JacksonProvider;
import de.ii.xtraplatform.store.app.ValueEncodingJackson;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import shadow.com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Collections;

public class XtraCfg {

  static {
    //System.load("/src/libxtracfg.so");
    //System.loadLibrary("xtracfg");
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      throw new IllegalArgumentException("No store directory given");
    }
    if (!args[0].startsWith("/")) {
      throw new IllegalArgumentException("Store directory has to be absolute");
    }

    System.out.println("STORE: " + args[0]);
    Path dataDirectory = Path.of(args[0]);

    Jackson jackson = new JacksonProvider(JacksonSubTypes::ids, false);
    ObjectMapper mapper = (new ValueEncodingJackson(jackson, false)).getMapper(ValueEncoding.FORMAT.JSON);
    CommandHandler commandHandler = new CommandHandler(mapper);

    try{
      Cli.execute(commandHandler);
    } catch (Throwable e) {
      System.out.println("ERROR " + e.getMessage());
    }
  }
}
