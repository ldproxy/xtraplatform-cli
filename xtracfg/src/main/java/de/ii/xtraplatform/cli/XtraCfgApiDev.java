package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.JacksonSubTypes;
import de.ii.xtraplatform.base.domain.JacksonProvider;
import de.ii.xtraplatform.entities.app.ValueEncodingJackson;
import de.ii.xtraplatform.entities.domain.ValueEncoding;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import shadow.com.fasterxml.jackson.core.JsonProcessingException;
import shadow.com.fasterxml.jackson.core.type.TypeReference;
import shadow.com.fasterxml.jackson.databind.ObjectMapper;

@ServerEndpoint("/sock")
public class XtraCfgApiDev {

  private static final TypeReference<LinkedHashMap<String, Object>> AS_MAP =
      new TypeReference<LinkedHashMap<String, Object>>() {};

  private final CommandHandler commandHandler;
  private final ObjectMapper jsonMapper;

  public XtraCfgApiDev() {
    this.commandHandler = new CommandHandler();
    JacksonProvider jackson = new JacksonProvider(JacksonSubTypes::ids, false);
    this.jsonMapper =
        (new ValueEncodingJackson(jackson, false)).getMapper(ValueEncoding.FORMAT.JSON);
  }

  @OnMessage
  public void handleTextMessage(Session session, String message) throws IOException {
    RemoteEndpoint.Basic remote = session.getBasicRemote();
    try {

      Map<String, Object> parameters = jsonMapper.readValue(message, AS_MAP);

      String result = handleCommand(remote, parameters);

      remote.sendText(result);
    } catch (JsonProcessingException e) {
      remote.sendText(String.format("{\"error\": \"Invalid command: %s\"}", e.getMessage()));
    }
  }

  public String handleCommand(RemoteEndpoint.Basic remote, Map<String, Object> commandMap) {
    if (!commandMap.containsKey("command")) {
      return String.format("{\"error\": \"No 'command' given: %s\"}", commandMap);
    }

    // TODO?
    EntitiesHandler.DEV = true;
    String parameters = "";

    try {
      int i = 0;
      for (String key : commandMap.keySet()) {
        parameters += i++ == 0 ? "?" : "&";
        parameters += key;
        parameters += "=" + commandMap.get(key);
      }

      String connect = "/connect" + parameters;

      String connectResult = commandHandler.handleCommand(connect);

      System.out.println(connectResult);

      String command = "/" + commandMap.get("command") + parameters;

      System.out.println("COMMAND: " + command);

      String result = commandHandler.handleCommand(command);

      System.out.println(result);

      return result;

    } catch (Throwable e) {
      System.out.println("ERROR: " + parameters + " | " + e.getMessage());
      e.printStackTrace();
      return String.format("{\"error\": \"Could not handle command: %s\"}", e.getMessage());
    }
  }
}
