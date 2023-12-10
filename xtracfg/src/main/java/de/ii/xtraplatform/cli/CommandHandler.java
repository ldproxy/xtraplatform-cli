package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.JacksonSubTypes;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.JacksonProvider;
import de.ii.xtraplatform.cli.cmd.*;
import de.ii.xtraplatform.cli.cmd.Upgrade;
import de.ii.xtraplatform.values.api.ValueEncodingJackson;
import de.ii.xtraplatform.values.domain.ValueEncoding;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.LogManager;
import shadow.com.fasterxml.jackson.core.JsonProcessingException;
import shadow.com.fasterxml.jackson.core.type.TypeReference;
import shadow.com.fasterxml.jackson.databind.ObjectMapper;

public class CommandHandler {

  static {
    LogManager.getLogManager().reset();
  }

  private static final TypeReference<LinkedHashMap<String, Object>> AS_MAP =
      new TypeReference<>() {};

  private final ObjectMapper jsonMapper;
  private Context context;

  public CommandHandler() {
    Jackson jackson = new JacksonProvider(JacksonSubTypes::ids, false);
    this.jsonMapper =
        (new ValueEncodingJackson(jackson, false)).getMapper(ValueEncoding.FORMAT.JSON);
  }

  public String handleCommand(String command) {
    return handleCommand(command, false, ignore -> {});
  }

  public String handleCommand(String command, boolean autoConnect, Consumer<String> tracker) {
    Consumer<Result> tracker2 =
        progress -> {
          try {
            tracker.accept(jsonMapper.writeValueAsString(progress.asMap()));
          } catch (JsonProcessingException e) {
            // ignore
          }
        };

    try {
      System.out.println("CALL " + command);
      Call call = Call.parse(jsonMapper.readValue(command, AS_MAP));

      Result result = handle(call, autoConnect, tracker2);

      return jsonMapper.writeValueAsString(result.asMap());
    } catch (JsonProcessingException e) {
      return String.format("{\"error\": \"Invalid call: %s\"}", e.getMessage());
    }
  }

  /*private Result handle(String command, Consumer<Result> tracker) {
    URI uri;
    try {
      uri = new URI(command);
    } catch (URISyntaxException e) {
      return Result.failure(String.format("Could not parse command: %s", e.getMessage()));
    }

    Call.Command cmd;
    String cmdString = uri.getPath().substring(1);
    try {
      cmd = Call.Command.valueOf(cmdString);
    } catch (Throwable e) {
      return Result.failure(String.format("Unknown command: %s", cmdString));
    }

    Map<String, String> parameters = parseParameters(uri.getQuery());

    return handle(cmd, parameters, tracker);
  }*/

  public boolean isConnected() {
    return Objects.nonNull(context)
        && Objects.nonNull(context.ldproxyCfg)
        && Objects.nonNull(context.layout);
  }

  private Result handle(Call call, boolean autoConnect, Consumer<Result> tracker) {
    if (call.command != Call.Command.connect && !isConnected()) {
      if (!autoConnect) {
        return Result.failure("Not connected to store");
      }

      Result result = handle(call.with(Call.Command.connect), false, ignore -> {});

      if (result.isFailure()) {
        return result;
      }
    }

    switch (call.command) {
      case connect:
        Context.Builder builder = new Context.Builder();
        Result result = new Connect(call.parameters).run(builder);

        this.context = builder.build();

        return result;
      case info:
        return new Info(call.parameters).run(context.layout);
      case check:
        return new Check(call.subcommand, call.parameters).run(context);
      case pre_upgrade:
        return new Upgrade(call.subcommand, call.parameters, true).run(context);
      case upgrade:
        return new Upgrade(call.subcommand, call.parameters, false).run(context);
      case auto:
        return new Auto(call.subcommand, call.parameters, tracker).run(context.ldproxyCfg);
      default:
        return Result.failure("Unknown command: " + call.command);
    }
  }

  /*private Result handle(
      Call.Command cmd, Map<String, String> parameters, Consumer<Result> tracker) {
    boolean verbose = flag(parameters, "verbose");
    boolean debug = flag(parameters, "debug");
    boolean ignoreRedundant = flag(parameters, "ignoreRedundant");
    Optional<String> path = Optional.ofNullable(Strings.emptyToNull(parameters.get("path")));
    Optional<Call.Subcommand> subcommand = subcommand(parameters);

    if (subcommand.isPresent() && subcommand.get() == Call.Subcommand.unknown) {
      return Result.failure(String.format("Unknown command: %s", parameters.get("subcommand")));
    }

    // System.out.println("J - COMMAND " + cmd + " " + parameters);

    if (cmd != Call.Command.connect && (Objects.isNull(ldproxyCfg) || Objects.isNull(layout))) {
      return Result.failure("Not connected to store");
    }

    Result result = Result.empty();

    switch (cmd) {
      case connect:
        return connect(parameters, verbose);
      case info:
        return info();
      case check:
        if (subcommand.isEmpty() || subcommand.get() == Call.Subcommand.cfg) {
          result = result.merge(CfgHandler.check(ldproxyCfg, ignoreRedundant, verbose, debug));
        }
        if (
        //subcommand.isEmpty() ||
         subcommand.isPresent()
            && subcommand.get() == Call.Subcommand.defaults) {
          result =
              result.merge(
                  EntitiesHandler.check(
                      ldproxyCfg, Type.Default, path, ignoreRedundant, verbose, debug));
        }
        if (subcommand.isEmpty() || subcommand.get() == Call.Subcommand.entities) {
          result =
              result.merge(
                  EntitiesHandler.check(
                      ldproxyCfg, Type.Entity, path, ignoreRedundant, verbose, debug));
        }
        if (subcommand.isEmpty() || subcommand.get() == Call.Subcommand.layout) {
          result = result.merge(LayoutHandler.check(layout, verbose));
        }
        if (result.isEmpty()) {
          result.success("Everything is fine");
        }
        return result;
      case pre_upgrade:
        if (subcommand.isEmpty() || subcommand.get() == Call.Subcommand.cfg) {
          boolean force = flag(parameters, "force");

          result =
              result.merge(
                  CfgHandler.preUpgrade(ldproxyCfg, ignoreRedundant, force, verbose, debug));
        }
        if (subcommand.isEmpty() || subcommand.get() == Call.Subcommand.entities) {
          boolean force = flag(parameters, "force");

          result =
              result.merge(
                  EntitiesHandler.preUpgrade(
                      ldproxyCfg, Type.Entity, path, ignoreRedundant, force, verbose, debug));
        }
        if (subcommand.isEmpty() || subcommand.get() == Call.Subcommand.layout) {
          result = result.merge(LayoutHandler.preUpgrade(layout, verbose));
        }
        if (result.isEmpty()) {
          result.success("Nothing to do");
        }
        return result;
      case upgrade:
        if (subcommand.isEmpty() || subcommand.get() == Call.Subcommand.cfg) {
          boolean backup = flag(parameters, "backup");
          boolean force = flag(parameters, "force");

          result =
              result.merge(
                  CfgHandler.upgrade(ldproxyCfg, backup, ignoreRedundant, force, verbose, debug));
        }
        if (subcommand.isEmpty() || subcommand.get() == Call.Subcommand.entities) {
          boolean backup = flag(parameters, "backup");
          boolean force = flag(parameters, "force");

          result =
              result.merge(
                  EntitiesHandler.upgrade(
                      ldproxyCfg,
                      Type.Entity,
                      path,
                      backup,
                      ignoreRedundant,
                      force,
                      verbose,
                      debug));
        }
        if (subcommand.isEmpty() || subcommand.get() == Call.Subcommand.layout) {
          result = result.merge(LayoutHandler.upgrade(layout, verbose));
        }
        if (result.isEmpty()) {
          result.success("Nothing to do");
        }
        return result;
      case auto:
        return AutoHandler.handle(parameters, ldproxyCfg, path, verbose, debug, tracker);
      default:
        return Result.failure("Unknown command: " + cmd);
    }
  }

  private Result connect(Map<String, String> parameters, boolean verbose) {
    try {
      this.ldproxyCfg = LdproxyCfg.create(Path.of(parameters.get("source")));
      this.layout = Layout.of(Path.of(parameters.get("source")));

      if (verbose) {
        return Result.ok(String.format("Store source: %s", layout.info().label()));
      }
    } catch (Throwable e) {
      e.printStackTrace();
      return Result.failure(e.getMessage());
    }
    return Result.empty();
  }

  private Result info() {
    return null;
  }

  private static Map<String, String> parseParameters(String query) {
    List<NameValuePair> parameters = URLEncodedUtils.parse(query, StandardCharsets.UTF_8);
    Map<String, String> params = new LinkedHashMap<>();

    for (NameValuePair nvp : parameters) {
      if (params.containsKey(nvp.getName())) {
        params.put(nvp.getName(), params.get(nvp.getName()) + "|" + nvp.getValue());
      } else {
        params.put(nvp.getName(), nvp.getValue());
      }
    }

    return params;
  }

  private static boolean flag(Map<String, String> parameters, String flag) {
    return Objects.equals(parameters.getOrDefault(flag, "false"), "true");
  }

  private static Optional<Call.Subcommand> subcommand(Map<String, String> parameters) {
    if (!parameters.containsKey("subcommand") || parameters.get("subcommand").isEmpty()) {
      return Optional.empty();
    }

    try {
      return Optional.of(Call.Subcommand.valueOf(parameters.get("subcommand")));
    } catch (Throwable e) {
      return Optional.of(Call.Subcommand.unknown);
    }
  }*/
}
