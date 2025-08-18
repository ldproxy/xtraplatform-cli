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
import shadow.io.dropwizard.util.DataSize;

public class CommandHandler {

  static {
    LogManager.getLogManager().reset();
  }

  private static final TypeReference<LinkedHashMap<String, Object>> AS_MAP =
      new TypeReference<>() {};

  private final ObjectMapper jsonMapper;
  private Context context;
  private boolean noCommandsYet;
  private boolean explicitConnect;

  public CommandHandler() {
    Jackson jackson = new JacksonProvider(JacksonSubTypes::ids, false);
    this.jsonMapper =
        (new ValueEncodingJackson(jackson, DataSize.megabytes(10), false)).getMapper(ValueEncoding.FORMAT.JSON);
    this.noCommandsYet = true;
    this.explicitConnect = false;
  }

  public String handleCommand(String command) {
    return handleCommand(command, true, ignore -> {});
  }

  public String handleCommand(String command, Progress tracker) {
    return handleCommand(command, true, tracker);
  }

  public String handleCommand(String command, boolean autoConnect, Progress tracker) {
    Consumer<Result> tracker2 =
        progress -> {
          try {
            tracker.update(jsonMapper.writeValueAsString(progress.asMap()));
          } catch (JsonProcessingException e) {
            // ignore
          }
        };

    try {
      Call call = Call.parse(jsonMapper.readValue(command, AS_MAP));

      if (noCommandsYet) {
        this.noCommandsYet = false;
        this.explicitConnect = call.command == Call.Command.connect;
      }

      Result result = handle(call, autoConnect, tracker2);

      return jsonMapper.writeValueAsString(result.asMap());
    } catch (JsonProcessingException e) {
      return String.format("{\"error\": \"Invalid call: %s\"}", e.getMessage());
    } finally{
      if (autoConnect && !explicitConnect) {
        this.context = null;
      }
    }
  }

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
      case autoValue:
         return new AutoValue(call.subcommand, call.parameters, tracker, jsonMapper).run(context.ldproxyCfg);
      case schemas:
        return new Schemas(call.parameters).run(context.ldproxyCfg);
      case file_type:
        return new FileType(call.parameters).run(context.ldproxyCfg);
      default:
        return Result.failure("Unknown command: " + call.command);
    }
  }
}
