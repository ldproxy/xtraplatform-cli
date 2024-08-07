package de.ii.xtraplatform.cli.cmd;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.cli.AutoValueHandler;
import de.ii.xtraplatform.cli.Result;
import shadow.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class AutoValue extends Common<LdproxyCfg>{

    enum Subcommand {
        analyze,
        generate
    }

    public final Subcommand subcommand;
    public final Map<String, String> parameters;
    public final Consumer<Result> tracker;
    public final ObjectMapper jsonMapper;

    public AutoValue(
            Optional<String> subcommand, Map<String, Object> parameters, Consumer<Result> tracker, ObjectMapper jsonMapper) {
        super(parameters);

        this.subcommand = requiredSubcommand(subcommand, Subcommand::valueOf);
        this.parameters =
                stringMap(
                        parameters, "apiId", "name", "collectionColors", "type");
        this.tracker = tracker;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Result run(LdproxyCfg ldproxyCfg) {
        /*
        // Call the preCheck method
        Result preCheckResult = AutoValueHandler.preCheck(parameters);

        // If the preCheck failed, return the result immediately
        if (preCheckResult.isFailure()) {
            return preCheckResult;
        }
*/

        try {
            switch (subcommand) {
                case analyze:
                    return AutoValueHandler.analyze(parameters, ldproxyCfg);
                case generate:
                    return AutoValueHandler.generate(parameters, ldproxyCfg, tracker, jsonMapper);
                default:
                    throw new IllegalStateException("Unexpected subcommand: " + subcommand);
            }
        } catch (Throwable e) {
            // e.printStackTrace();
            return Result.failure("Unexpected error: " + e.getMessage());
        }
    }
}
