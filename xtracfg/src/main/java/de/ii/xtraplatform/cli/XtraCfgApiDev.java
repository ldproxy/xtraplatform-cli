package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.JacksonSubTypes;
import de.ii.xtraplatform.base.domain.JacksonProvider;
import de.ii.xtraplatform.entities.app.ValueEncodingJackson;
import de.ii.xtraplatform.entities.domain.ValueEncoding;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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

  @OnOpen
  public void open(Session session) {
    session.setMaxIdleTimeout(0);
  }

  @OnMessage
  public void handleTextMessage(Session session, String message) throws IOException {
    RemoteEndpoint.Basic remote = session.getBasicRemote();

    Consumer<String> tracker =
        progress -> {
          try {
            remote.sendText(progress);
          } catch (IOException e) {
            // ignore
          }
        };

    try {
      Map<String, Object> parameters = jsonMapper.readValue(message, AS_MAP);

      String result = handleCommand(parameters, tracker);

      remote.sendText(result);
    } catch (JsonProcessingException e) {
      remote.sendText(String.format("{\"error\": \"Invalid command: %s\"}", e.getMessage()));
    }
  }

  public String handleCommand(Map<String, Object> commandMap, Consumer<String> tracker) {
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
        parameters +=
            "=" + URLEncoder.encode(parseParameter(commandMap, key), StandardCharsets.UTF_8);
      }

      String connect = "/connect" + parameters;

      String connectResult = commandHandler.handleCommand(connect);

      System.out.println(connectResult);

      String command = "/" + commandMap.get("command") + parameters;

      System.out.println("COMMAND: " + command);

      String result = commandHandler.handleCommand(command, tracker);

      System.out.println(result);

      return result;

    } catch (Throwable e) {
      System.out.println("ERROR: " + parameters + " | " + e.getMessage());
      e.printStackTrace();
      return String.format("{\"error\": \"Could not handle command: %s\"}", e.getMessage());
    }
  }

  private String parseParameter(Map<String, Object> parameters, String key) {
    if (Objects.equals(key, "types")) {
      Map<String, List<String>> types = (Map<String, List<String>>) parameters.get(key);

      return types.entrySet().stream()
          .map(
              entry ->
                  entry.getKey() + ":" + entry.getValue().stream().collect(Collectors.joining(",")))
          .collect(Collectors.joining("|"));
    }

    return parameters.get(key).toString();
  }
}
