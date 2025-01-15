package org.matsim.application.options;

import org.apache.commons.lang3.StringUtils;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.CommandSpec;
import org.matsim.application.Dependency;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Automatically defines input options by reading the {@link CommandSpec} annotation.
 */
public final class InputOptions {

	/**
	 * Class original used to create this option.
	 */
	private final Class<? extends MATSimAppCommand> clazz;
	private final CommandSpec spec;
	private final Map<String, String> inputs = new HashMap<>();
	/**
	 * Needs to be present or the mixin will not be processed at all.
	 */
	@CommandLine.Option(names = "--unused-input-option", hidden = true)
	private String unused;
	private Path runDirectory;
	private String networkPath;
	private String populationPath;
	private String countsPath;
	private String eventsPath;

	private InputOptions(Class<? extends MATSimAppCommand> clazz, CommandSpec spec) {
		this.clazz = clazz;
		this.spec = spec;
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

		require = require.replace(".gz", "");
		String[] split = require.split("\\.");

		// Remove the suffix
		String s = split.length > 1
				? StringUtils.join(split, "-", 0, split.length - 1)
				: require;

		// replace special chars
		String result = s.replaceAll("_|/\"|\\\\|:|/", "-");

		// crs is a reserved name from other options
		if (result.equals("crs"))
			result = "crs-file";

		return result;
	}

	/**
	 * Creates a new instance by evaluating the {@link CommandSpec} annotation.
	 */
	public static InputOptions ofCommand(Class<? extends MATSimAppCommand> command) {
		CommandSpec spec = command.getAnnotation(CommandSpec.class);
		if (spec == null)
			throw new IllegalArgumentException("CommandSpec annotation must be present on " + command);

		return new InputOptions(command, spec);
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
			throw new IllegalArgumentException(String.format("The input '%s' is not registered as required in @CommandSpec.", name));

		return inputs.get(name);
	}

	/**
	 * Return the path to a file provided by command defined as dependency.
	 * @param clazz dependency
	 * @param name file name
	 *
	 * @return can be null if not provided and the dependency is not required.
	 */
	@Nullable
	public String getPath(Class<? extends MATSimAppCommand> clazz, String name) {
		Optional<Dependency> first = Arrays.stream(spec.dependsOn())
			.filter(d -> d.value().equals(clazz))
			.findFirst();

		if (first.isEmpty())
			throw new IllegalArgumentException(String.format("Dependency to '%s' is not defined in @CommandSpec.", clazz));

		if (first.get().required() && !inputs.containsKey(name))
			throw new IllegalArgumentException(String.format("Path to '%s' is required but not provided.", name));

		return inputs.get(name);
	}

	public Network getNetwork() {
		if (!spec.requireNetwork())
			throw new IllegalArgumentException("Network can not be accessed unless, requireNetwork=true.");

		return NetworkUtils.readNetwork(networkPath);
	}

	public String getNetworkPath() {
		return networkPath;
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

	public String getCountsPath() {
		if (!spec.requireEvents())
			throw new IllegalArgumentException("Counts can not be accessed unless, requireCounts=true.");

		return countsPath;
	}

	public Path getRunDirectory() {
		if (!spec.requireRunDirectory())
			throw new IllegalArgumentException("Run directory can not be accessed unless, requireRunDirectory=true.");

		return runDirectory;
	}

	@CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
	@SuppressWarnings("unused")
	void setSpec(CommandLine.Model.CommandSpec command) {
		AtomicBoolean flag = new AtomicBoolean(false);

		command.mixinStandardHelpOptions(true);

		if (!command.userObject().getClass().equals(clazz)) {

			throw new IllegalArgumentException(String.format("InputOptions in the implementation of '%s' is using class '%s' as reference. These two must be the same",
					command.userObject().getClass(), clazz));
		}

		Stream<String> inputFiles = Stream.concat(
			Arrays.stream(spec.requires()),
			Arrays.stream(spec.dependsOn()).flatMap( d-> Arrays.stream(d.files()))
		);

		inputFiles.forEach(require -> {
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
		});

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

		if (spec.requireCounts()) {
			command.add(createArg(flag, "--counts", "Path to input counts.", new CommandLine.Model.ISetter() {
				@Override
				public <T> T set(T value) {
					countsPath = value.toString();
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
}
