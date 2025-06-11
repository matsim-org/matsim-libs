package org.matsim.application.options;

import org.apache.commons.lang3.ArrayUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.matsim.application.options.InputOptions.argName;
import static org.matsim.application.options.InputOptions.argNames;

/**
 * Automatically defines output options by reading the {@link CommandSpec} annotation.
 */
public final class OutputOptions {

	private final CommandSpec spec;
	private final Map<String, Path> outputs = new HashMap<>();
	/**
	 * Needs to be present or the mixin will not be processed at all.
	 */
	@CommandLine.Option(names = "--unused-output-option", hidden = true)
	private String unused;

	private OutputOptions(CommandSpec spec) {
		this.spec = spec;
	}

	/**
	 * Creates a new instance by evaluating the {@link CommandSpec} annotation.
	 */
	public static OutputOptions ofCommand(Class<? extends MATSimAppCommand> command) {
		CommandSpec spec = command.getAnnotation(CommandSpec.class);
		if (spec == null)
			throw new IllegalArgumentException("CommandSpec annotation must be present on " + command);
		return new OutputOptions(spec);
	}

	/**
	 * Get the first configured output path.
	 */
	public Path getPath() {

		if (spec.produces().length == 0)
			throw new IllegalArgumentException("There is no output defined for this command");

		return getPath(spec.produces()[0]);
	}

	/**
	 * Get the output path configured for a specific name.
	 */
	public Path getPath(String name) {

		if (!ArrayUtils.contains(spec.produces(), name))
			throw new IllegalArgumentException(String.format("The output file '%s' is not defined in the @CommandSpec", name));

		Path output = outputs.containsKey(name) ? outputs.get(name) : Path.of(name);
		try {
			Files.createDirectories(output.toAbsolutePath().getParent());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return output;
	}

	/**
	 * Get path for a specific output name and replace a placeholder (%s) in the name.
	 */
	public Path getPath(String name, String placeholder) {

		if (!ArrayUtils.contains(spec.produces(), name))
			throw new IllegalArgumentException(String.format("The output file '%s' is not defined in the @CommandSpec", name));
		if (!name.contains("%s"))
			throw new IllegalArgumentException(String.format("File %s does not contain placeholder %%s", name));

		Path output = outputs.containsKey(name) ? outputs.get(name) : Path.of(name);
		if (!output.toString().contains("%s"))
			throw new IllegalArgumentException(String.format("Argument %s does not contain placeholder %%s", output));

		Path outputReplaced = Path.of(String.format(output.toString(), placeholder));
		try {
			Files.createDirectories(outputReplaced.toAbsolutePath().getParent());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return outputReplaced;
	}

	@CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
	void setSpec(CommandLine.Model.CommandSpec command) {

		command.mixinStandardHelpOptions(true);

		AtomicBoolean flag = new AtomicBoolean(false);
		for (String produce : spec.produces()) {

			CommandLine.Model.OptionSpec.Builder arg = CommandLine.Model.OptionSpec.
					builder(argNames(flag, "--output", "--output-" + argName(produce)))
					.type(Path.class)
					.setter(new CommandLine.Model.ISetter() {
						@Override
						public <T> T set(T value) {
							outputs.put(produce, (Path) value);
							return value;
						}
					})
					.defaultValue(produce)
					.description("Desired output path for " + produce + ".")
					.required(false);

			command.add(arg.build());
		}
	}
}
