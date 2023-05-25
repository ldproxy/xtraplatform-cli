package de.ii.xtraplatform.cli;

import java.util.List;
import java.util.Map;
import shadow.com.fasterxml.jackson.core.JsonProcessingException;
import shadow.com.fasterxml.jackson.databind.ObjectMapper;

public class CommandHandler {

  private final ObjectMapper mapper;

  public CommandHandler(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public String handleCommand(String command) {

    /*String response =
    "{\n"
        + "\t\t\"results\": [\n"
        + "\t\t\t{\n"
        + "\t\t\t\t\"status\": \"ERROR\",\n"
        + "\t\t\t\t\"message\": \"Doh!\"\n"
        + "\t\t\t},\n"
        + "\t\t\t{\n"
        + "\t\t\t\t\"status\": \"WARNING\",\n"
        + "\t\t\t\t\"message\": \"Doh!\"\n"
        + "\t\t\t},\n"
        + "\t\t\t{\n"
        + "\t\t\t\t\"status\": \"SUCCESS\",\n"
        + "\t\t\t\t\"message\": \"Doh!\"\n"
        + "\t\t\t}\n"
        + "\t\t]\n"
        + "\t}";*/

    // logic goes here
    System.out.println("COMMAND: " + command);

    Map<String, List<Map<String, String>>> results =
        Map.of(
            "results",
            List.of(
                Map.of("status", "ERROR", "message", "Doh!"),
                Map.of("status", "WARNING", "message", "Doh!!"),
                Map.of("status", "SUCCESS", "message", "Doh!!!")));

    try {
      //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results));
      return mapper.writeValueAsString(results);
    } catch (JsonProcessingException e) {
      return String.format("{\"error\": %s}", e.getMessage());
    }
  }
}
