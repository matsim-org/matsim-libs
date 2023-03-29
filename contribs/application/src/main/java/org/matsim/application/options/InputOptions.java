package org.matsim.application.options;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Automatically defines input options by reading the {@link CommandSpec} annotation.
 */
public final class InputOptions {

	/**
	 * Needs to be present or the mixin will not be processed at all.
	 */
	@CommandLine.Option(names = "--unused-input-option", hidden = true)
	private String unused;

	private final CommandSpec spec;

	private final Map<String, String> inputs = new HashMap<>();

	private Path runDirectory;
	private String networkPath;
	private String populationPath;
	private String eventsPath;

	private InputOptions(CommandSpec spec) {
		this.spec = spec;
	}

	/**
	 * Get the first configured input.
	 */
	public String getPath() {
		if (spec.requires().length == 0)
			throw new IllegalArgumentException("Spec input definition is empty. Input can not be accessed.");

		return getPath(spec.requires()[0]);
	}

	/**
	 * Get the input that was configured for a specific name.
	 */
	public String getPath(String name) {
		if (!inputs.containsKey(name))
			throw new IllegalArgumentException(String.format("The input '%s' is not registered", name));

		return inputs.get(name);
	}

	public Network getNetwork() {
		if (!spec.requireNetwork())
			throw new IllegalArgumentException("Network can not be accessed unless, requireNetwork=true.");

		return NetworkUtils.readNetwork(networkPath);
	}

	public Population getPopulation() {
		if (!spec.requirePopulation())
			throw new IllegalArgumentException("Population can not be accessed unless, requirePopulation=true.");

		return PopulationUtils.readPopulation(populationPath);
	}

	public String getEventsPath() {
		if (!spec.requireEvents())
			throw new IllegalArgumentException("Events can not be accessed unless, requireEvents=true.");

		return eventsPath;
	}

	public Path getRunDirectory() {
		if (!spec.requireRunDirectory())
			throw new IllegalArgumentException("Run directory can not be accessed unless, requireRunDirectory=true.");

		return runDirectory;
	}

	@CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
	void setSpec(CommandLine.Model.CommandSpec command) {
		AtomicBoolean flag = new AtomicBoolean(false);

		command.mixinStandardHelpOptions(true);

		// TODO: refactor the arg building
		// other input args

		for (String require : spec.requires()) {
			if (require.isBlank())
				throw new IllegalArgumentException("Require argument can not be blank.");

			command.add(createArg(flag, "--input-" + argName(require), "Path to input " + require + ".",
					new CommandLine.Model.ISetter() {
						@Override
						public <T> T set(T value) {
							inputs.put(require, (String) value);
							return value;
						}
					}));
		}

		if (spec.requireNetwork()) {
			command.add(createArg(flag, "--network", "Path to input network.", new CommandLine.Model.ISetter() {
				@Override
				public <T> T set(T value) {
					networkPath = value.toString();
					return value;
				}
			}));
		}

		if (spec.requireEvents()) {
			command.add(createArg(flag, "--events", "Path to input events.", new CommandLine.Model.ISetter() {
				@Override
				public <T> T set(T value) {
					eventsPath = value.toString();
					return value;
				}
			}));
		}

		if (spec.requirePopulation()) {
			command.add(createArg(flag, "--population", "Path to input plans / population.", new CommandLine.Model.ISetter() {
				@Override
				public <T> T set(T value) {
					populationPath = value.toString();
					return value;
				}
			}));
		}

		if (spec.requireRunDirectory()) {
			command.add(createArg(flag, "--run-directory", "Path to input run directory.", new CommandLine.Model.ISetter() {
				@Override
				public <T> T set(T value) {
					runDirectory = Path.of(value.toString());
					return value;
				}
			}));
		}
	}

	private CommandLine.Model.OptionSpec createArg(AtomicBoolean flag, String name, String description, CommandLine.Model.ISetter setter) {
		CommandLine.Model.OptionSpec.Builder arg = CommandLine.Model.OptionSpec.
				builder(argNames(flag, "--input", name))
				.initialValue("")
				.type(String.class)
				.setter(setter)
				.description(description)
				.required(true);

		return arg.build();
	}

	/**
	 * Return the argument names, the first specified argument will also be named with {@code first}.
	 */
	static String[] argNames(AtomicBoolean flag, String first, String name) {
		if (!flag.get()) {
			flag.set(true);
			return new String[]{first, name};
		}
		return new String[]{name};
	}

	/**
	 * Return the argument name of an input file.
	 */
	public static String argName(String require) {
		String[] split = require.split("\\.");

		return split[0].replaceAll("_|/\"|\\\\|:|/", "-");
	}

	/**
	 * Creates a new instance by evaluating the {@link CommandSpec} annotation.
	 */
	public static InputOptions ofCommand(Class<? extends MATSimAppCommand> command) {
		CommandSpec spec = command.getAnnotation(CommandSpec.class);
		if (spec == null)
			throw new IllegalArgumentException("CommandSpec annotation must be present on " + command);

		return new InputOptions(spec);
	}
}
