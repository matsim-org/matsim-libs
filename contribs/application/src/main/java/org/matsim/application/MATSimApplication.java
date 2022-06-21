package org.matsim.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.commands.RunScenario;
import org.matsim.application.commands.ShowGUI;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.AutoComplete;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A helper class to define and execute MATSim scenarios, including a pipeline of prepare and analysis scripts.
 * This class provides a common scenario setup procedure and command line parsing.
 * Scenarios simply need to extend it and overwrite the *prepare methods if needed.
 * <p>
 * To run your application use:
 * <code>
 * MATSimApplication.run(RunScenario.class, args);
 * </code>
 * <p>
 * There are also other variants of this method, e.g. to run the scenario within code.
 * <p>
 * This class also automatically registers classes from the {@link Prepare} and {@link Analysis} annotations as subcommands.
 * These can be used to build a pipeline with command needed for preparation analysis.
 *
 * @see #run(Class, String...)
 * @see #execute(Class, Config, String...)
 * @see #prepare(Class, Config, String...)
 * @see Prepare
 * @see Analysis
 */
@CommandLine.Command(
		name = MATSimApplication.DEFAULT_NAME,
		description = {"", "Use the \"run\" command to execute the scenario, or any of the other available commands."},
		headerHeading = MATSimApplication.HEADER,
		parameterListHeading = "%n@|bold,underline Parameters:|@%n",
		optionListHeading = "%n@|bold,underline Options:|@%n",
		commandListHeading = "%n@|bold,underline Commands:|@%n",
		footerHeading = "\n",
		footer = "@|cyan If you would like to contribute or report an issue please go to https://github.com/matsim-org.|@",
		usageHelpWidth = 120,
		usageHelpAutoWidth = true,
		showDefaultValues = true,
		mixinStandardHelpOptions = true,
		abbreviateSynopsis = true,
		subcommands = {RunScenario.class, ShowGUI.class, CommandLine.HelpCommand.class, AutoComplete.GenerateCompletion.class}
)
public abstract class MATSimApplication implements Callable<Integer>, CommandLine.IDefaultValueProvider {

	private static final Logger log = LogManager.getLogger(MATSimApplication.class);

	public static final String COLOR = "@|bold,fg(81) ";
	static final String DEFAULT_NAME = "MATSimApplication";
	static final String HEADER = COLOR +
			"  __  __   _ _____ ___ _       \n" +
			" |  \\/  | /_\\_   _/ __(_)_ __  \n" +
			" | |\\/| |/ _ \\| | \\__ \\ | '  \\ \n" +
			" |_|  |_/_/ \\_\\_| |___/_|_|_|_|\n|@";

	@CommandLine.Option(names = "--config", description = "Path to config file used for the run.", order = 0)
	protected File scenario;

	@CommandLine.Option(names = "--yaml", description = "Path to yaml file with config params to overwrite.", required = false)
	protected Path specs;

	@CommandLine.Option(names = "--iterations", description = "Overwrite number of iterations (if greater than -1).", defaultValue = "-1")
	protected int iterations;

	@CommandLine.Option(names = "--runId", description = "Overwrite runId defined by the application")
	protected String runId;

	@CommandLine.Option(names = "--output", description = "Overwrite output folder defined by the application")
	protected Path output;

	@CommandLine.Option(names = "--post-processing", description = "Option for post-processing", defaultValue = "enabled")
	protected PostProcessOption post;

	/**
	 * This Map will never contain anything, because this argument is not parsed correctly, but instead will go into remainingArgs field.
	 *
	 * @see #remainingArgs
	 */
	@CommandLine.Option(names = {"-c:", "--config:"}, arity = "0..*", description = "Overwrite config values (e.g. --config:controler.runId=123)")
	private Map<String, String> configValues;

	@CommandLine.Unmatched
	private List<String> remainingArgs;

	/**
	 * Path to the default scenario config, if applicable.
	 */
	@Nullable
	private final String defaultScenario;

	/**
	 * Contains loaded config file.
	 */
	@Nullable
	private Config config;

	/**
	 * Constructor for an application without a default scenario path.
	 */
	public MATSimApplication() {
		defaultScenario = null;
	}

	/**
	 * Constructor
	 *
	 * @param defaultScenario path to the default scenario config
	 */
	public MATSimApplication(@Nullable String defaultScenario) {
		this.defaultScenario = defaultScenario;
	}

	/**
	 * Constructor for given config, that can be used from code.
	 */
	public MATSimApplication(@Nullable Config config) {
		this.config = config;
		this.defaultScenario = "<config from code>";
	}

	/**
	 * The main scenario setup procedure.
	 *
	 * @return return code
	 */
	@Override
	public Integer call() throws Exception {

		// load config if not present yet.
		if (config == null) {
			config = loadConfig(Objects.requireNonNull(scenario, "No default scenario location given").getAbsoluteFile().toString());
		} else {
			Config tmp = prepareConfig(config);
			config = tmp != null ? tmp : config;
		}

		Objects.requireNonNull(config);

		if (specs != null)
			applySpecs(config, specs);

		if (remainingArgs != null) {
			String[] args = remainingArgs.stream().map(s -> s.replace("-c:", "--config:")).toArray(String[]::new);
			ConfigUtils.applyCommandline(config, args);
		}

		if (iterations > -1)
			config.controler().setLastIteration(iterations - 1);

		if (output != null)
			config.controler().setOutputDirectory(output.toString());

		if (runId != null)
			config.controler().setRunId(runId);

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		prepareScenario(scenario);

		final Controler controler = new Controler(scenario);

		prepareControler(controler);

		// Check if simulation needs to be run
		if (post != PostProcessOption.post_process_only)
			controler.run();

		if (post != PostProcessOption.disabled) {

			List<MATSimAppCommand> commands = preparePostProcessing(Path.of(config.controler().getOutputDirectory()), config.controler().getRunId());

			for (MATSimAppCommand command : commands) {

				try {
					log.info("Running post-process command {}", command.getClass().getSimpleName());
					command.call();
				} catch (Exception e) {
					log.error("Error running post-processing", e);
				}

			}

		}


		return 0;
	}

	/**
	 * Apply given specs to config.
	 */
	private static void applySpecs(Config config, Path specs) {

		if (!Files.exists(specs)) {
			throw new IllegalArgumentException("Desired run config does not exist:" + specs);
		}

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
				.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));

		try (BufferedReader reader = Files.newBufferedReader(specs)) {

			JsonNode node = mapper.readTree(reader);

			Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();

				ConfigGroup group = config.getModules().get(field.getKey());
				if (group == null) {
					log.warn("Config group not found: {}", field.getKey());
					continue;
				}

				applyNodeToConfigGroup(field.getValue(), group);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Sets the json config into
	 */
	private static void applyNodeToConfigGroup(JsonNode node, ConfigGroup group) {

		Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> field = fields.next();

			if (field.getValue().isArray()) {

				Collection<? extends ConfigGroup> params = group.getParameterSets(field.getKey());

				// single node and entry merged directly
				if (field.getValue().size() == 1 && params.size() == 1) {
					applyNodeToConfigGroup(field.getValue().get(0), params.iterator().next());
				} else {

					for (JsonNode item : field.getValue()) {

						Map.Entry<String, JsonNode> first = item.fields().next();
						Optional<? extends ConfigGroup> m = params.stream().filter(p -> p.getParams().get(first.getKey()).equals(first.getValue().textValue())).findFirst();
						if (m.isEmpty())
							throw new IllegalArgumentException("Could not find matching param by key" + first);

						applyNodeToConfigGroup(item, m.get());
					}
				}

			} else {

			    if (!field.getValue().isValueNode())
			        throw new IllegalArgumentException("Received complex value type instead of primitive: " + field.getValue());


			    if (field.getValue().isTextual())
                    group.addParam(field.getKey(), field.getValue().textValue());
			    else
                    group.addParam(field.getKey(), field.getValue().toString());
			}
		}
	}


	/**
	 * Custom module configs that will be added to the {@link Config} object.
	 *
	 * @return {@link ConfigGroup} to add
	 */
	protected List<ConfigGroup> getCustomModules() {
		return Lists.newArrayList();
	}

	/**
	 * Modules that are configurable via command line arguments.
	 */
	private List<ConfigGroup> getConfigurableModules() {
		return Lists.newArrayList(
				new ControlerConfigGroup(),
				new GlobalConfigGroup(),
				new QSimConfigGroup()
		);
	}

	/**
	 * Preparation step for the config.
	 *
	 * @param config base config loaded from user specified location
	 * @return prepared {@link Config}
	 */
	protected Config prepareConfig(Config config) {
		return config;
	}

	/**
	 * Preparation step for the scenario.
	 */
	protected void prepareScenario(Scenario scenario) {
	}

	/**
	 * Preparation step for the controller.
	 */
	protected void prepareControler(Controler controler) {
	}

	/**
	 * Preparation of {@link MATSimAppCommand} to run after the simulation has finished. The instances have to be fully constructed in this method
	 * no further arguments are passed down to them.
	 * @return list of commands to run.
	 */
	protected List<MATSimAppCommand> preparePostProcessing(Path outputFolder, String runId) {
		return List.of();
	}

	/**
	 * Add and an option and value to run id and output folder.
	 */
	protected final void addRunOption(Config config, String option, Object value) {

		String postfix;
		if ("".equals(value))
			postfix = "-" + option;
		else
			postfix = "-" + option + "_" + value;

		String outputDir = config.controler().getOutputDirectory();
		if (outputDir.endsWith("/")) {
			config.controler().setOutputDirectory(outputDir.substring(0, outputDir.length() - 1) + postfix + "/");
		} else
			config.controler().setOutputDirectory(outputDir + postfix);

		// dot should not be part of run id
		config.controler().setRunId(config.controler().getRunId() + postfix.replace(".", ""));
	}

	/**
	 * Add and an option to run id and output folder, delimited by "_".
	 */
	protected final void addRunOption(Config config, String option) {
		addRunOption(config, option, "");
	}

	private Config loadConfig(String path) {
		List<ConfigGroup> customModules = getCustomModules();

		final Config config = ConfigUtils.loadConfig(path, customModules.toArray(new ConfigGroup[0]));
		Config prepared = prepareConfig(config);

		return prepared != null ? prepared : config;
	}

	@Override
	public String defaultValue(CommandLine.Model.ArgSpec argSpec) throws Exception {
		Object obj = argSpec.userObject();
		if (obj instanceof Field) {
			Field field = (Field) obj;
			if (field.getName().equals("scenario") && field.getDeclaringClass().equals(MATSimApplication.class)) {
				return defaultScenario;
			}
		}

		return null;
	}

	/**
	 * Run the application class and terminates when done.
	 * This should never be used in tests and only in main methods.
	 */
	public static void run(Class<? extends MATSimApplication> clazz, String... args) {
		MATSimApplication app;
		try {
			app = clazz.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			System.err.println("Could not instantiate the application class");
			e.printStackTrace();
			System.exit(1);
			return;
		}

		// GUI does not pass any argument
		boolean runInGUi = "true".equals(System.getenv("MATSIM_GUI"));
		if (runInGUi) {

			// prepend necessary options
			List<String> l = Lists.newArrayList("run", "--config");
			l.addAll(Arrays.asList(args));

			args = l.toArray(new String[0]);
		}

		prepareArgs(args);

		CommandLine cli = prepare(app);

		CommandLine.ParseResult parsed = cli.parseArgs(args);

		if (!parsed.hasSubcommand()) {
			cli.usage(System.out);
			System.exit(2);
		}

		List<String> unmatched = unmatched(parsed);
		if (!unmatched.isEmpty()) {
			throw new RuntimeException("Unknown arguments: " + unmatched);
		}

		int code = cli.execute(args);
		System.exit(code);
	}

	/**
	 * Calls an application class and forwards any exceptions.
	 *
	 * @param config input config, overwrites default given by scenario
	 * @return return code, 0 indicates success and no errors
	 */
	public static int execute(Class<? extends MATSimApplication> clazz, Config config, String... args) {
		MATSimApplication app;
		try {
			if (config != null)
				app = clazz.getDeclaredConstructor(Config.class).newInstance(config);
			else
				app = clazz.getDeclaredConstructor().newInstance();

		} catch (NoSuchMethodException e) {
			throw new RuntimeException("The scenario class must have public constructors!", e);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Could not instantiate the application class", e);
		}

		prepareArgs(args);

		CommandLine cli = prepare(app);
		AtomicReference<Exception> exc = new AtomicReference<>();
		cli.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
			exc.set(ex);
			return 2;
		});

		int code = cli.execute(args);

		if (code > 0) {
			Exception e = exc.get();
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException("Application exited with error", e);

		}

		return code;
	}

	/**
	 * Calls an application class with its default config.
	 *
	 * @see #execute(Class, Config, String...)
	 */
	public static int execute(Class<? extends MATSimApplication> clazz, String... args) {
		return execute(clazz, null, args);
	}


	/**
	 * Prepare and return controller without running the scenario.
	 * This allows to configure the controller after setup has been run.
	 */
	public static Controler prepare(Class<? extends MATSimApplication> clazz, Config config, String... args) {

		MATSimApplication app;
		try {
			app = clazz.getDeclaredConstructor(Config.class).newInstance(config);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Could not instantiate the application class", e);
		}

		CommandLine cli = prepare(app);
		CommandLine.ParseResult parseResult = cli.parseArgs(args);

		if (parseResult.errors().size() > 0)
			throw new RuntimeException(parseResult.errors().get(0));

		List<String> unmatched = unmatched(parseResult);
		if (!unmatched.isEmpty()) {
			throw new RuntimeException("Unknown arguments: " + unmatched);
		}

		Config tmp = app.prepareConfig(config);
		config = tmp != null ? tmp : config;

		if (app.specs != null) {
			applySpecs(config, app.specs);
		}

		if (app.remainingArgs != null) {
			String[] extraArgs = app.remainingArgs.stream().map(s -> s.replace("-c:", "--config:")).toArray(String[]::new);
			ConfigUtils.applyCommandline(config, extraArgs);
		}

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		app.prepareScenario(scenario);

		final Controler controler = new Controler(scenario);
		app.prepareControler(controler);

		return controler;
	}

	/**
	 * Command argument needs to be at the correct position.
	 */
	private static void prepareArgs(String[] args) {

		// run needs to be at the last position
		if (args.length > 0 && args[0].equals("run")) {
			System.arraycopy(args, 1, args, 0, args.length - 1);
			args[args.length - 1] = "run";
		}
	}

	/**
	 * Check if unmatched options are correct.
	 */
	private static List<String> unmatched(CommandLine.ParseResult parseResult) {
		List<String> unmatched = Lists.newArrayList(parseResult.unmatched());
		ListIterator<String> it = unmatched.listIterator();

		// previous string
		String prev = null;
		while (it.hasNext()) {

			String current = it.next();

			// filter string handled by matsim parser
			if (current.startsWith("-c:") || current.startsWith("--config:"))
				it.remove();

			// separate arguments should also be valid, e.g --c:global.threads 5
			if (!current.startsWith("--") && prev != null && (prev.startsWith("-c:") || prev.startsWith("--config:")))
				it.remove();

			prev = current;
		}

		return unmatched;
	}

	private static CommandLine prepare(MATSimApplication app) {
		CommandLine cli = new CommandLine(app);

		if (cli.getCommandName().equals(DEFAULT_NAME))
			cli.setCommandName(app.getClass().getSimpleName());

		setupOptions(cli, app);
		setupSubcommands(cli, app);

		List<ConfigGroup> modules = Lists.newArrayList();
		modules.addAll(app.getConfigurableModules());
		modules.addAll(app.getCustomModules());

		// setupConfig(cli, modules);
		return cli;
	}

	private static void setupOptions(CommandLine cli, MATSimApplication app) {

		CommandLine.Model.CommandSpec spec = cli.getCommandSpec();
		String[] header = spec.usageMessage().header();
		// set formatting for header
		if (header.length == 1) {
			spec.usageMessage().header(COLOR + " " + header[0].trim() + "|@%n");
		}

		spec.defaultValueProvider(app);
	}

	/**
	 * Processes the {@link Prepare} annotation and inserts command automatically.
	 */
	private static void setupSubcommands(CommandLine cli, MATSimApplication app) {

		if (app.getClass().isAnnotationPresent(Prepare.class)) {
			Prepare prepare = app.getClass().getAnnotation(Prepare.class);
			cli.addSubcommand("prepare", new PrepareCommand());
			CommandLine subcommand = cli.getSubcommands().get("prepare");

			for (Class<?> aClass : prepare.value()) {
				subcommand.addSubcommand(aClass);
			}
		}

		if (app.getClass().isAnnotationPresent(Analysis.class)) {
			Analysis analysis = app.getClass().getAnnotation(Analysis.class);
			cli.addSubcommand("analysis", new AnalysisCommand());
			CommandLine subcommand = cli.getSubcommands().get("analysis");

			for (Class<?> aClass : analysis.value()) {
				subcommand.addSubcommand(aClass);
			}
		}
	}

	/**
	 * Inserts modules config into command line, but not used at the moment
	 */
	@Deprecated
	private static void setupConfig(CommandLine cli, List<ConfigGroup> modules) {

		CommandLine.Model.CommandSpec spec = cli.getCommandSpec();
		for (ConfigGroup module : modules) {

			CommandLine.Model.ArgGroupSpec.Builder group = CommandLine.Model.ArgGroupSpec.builder()
					.headingKey(module.getName())
					.heading(module.getName() + "\n");

			for (Map.Entry<String, String> param : module.getParams().entrySet()) {

				// Escape format symbols
				String desc = module.getComments().get(param.getKey());
				if (desc != null)
					desc = desc.replace("%", "%%");

				group.addArg(CommandLine.Model.OptionSpec.builder("--" + module.getName() + "-" + param.getKey())
						.hideParamSyntax(true)
						.hidden(false)
						.description((desc != null ? desc + " " : "") + "Default: ${DEFAULT-VALUE}")
						.defaultValue(param.getValue())
						.build());

			}

			spec.addArgGroup(group.build());
		}
	}

	@CommandLine.Command(name = "prepare", description = "Contains all commands for preparing the scenario. (See help prepare)",
			subcommands = CommandLine.HelpCommand.class)
	public static class PrepareCommand implements Callable<Integer> {

		@CommandLine.Spec
		private CommandLine.Model.CommandSpec spec;

		@Override
		public Integer call() throws Exception {
			System.out.printf("No subcommand given. Chose on of: %s", spec.subcommands().keySet());
			return 0;
		}
	}

	@CommandLine.Command(name = "analysis", description = "Contains all commands for analysing the scenario. (See help analysis)",
			subcommands = CommandLine.HelpCommand.class)
	public static class AnalysisCommand implements Callable<Integer> {

		@CommandLine.Spec
		private CommandLine.Model.CommandSpec spec;

		@Override
		public Integer call() throws Exception {
			System.out.printf("No subcommand given. Chose on of: %s", spec.subcommands().keySet());
			return 0;
		}
	}

	/**
	 * Classes from {@link #value()} will be registered as "prepare" subcommands.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	public @interface Prepare {
		Class<? extends MATSimAppCommand>[] value() default {};
	}

	/**
	 * Classes from {@link #value()} will be registered as "analysis" subcommands.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	public @interface Analysis {
		Class<? extends MATSimAppCommand>[] value() default {};
	}

	/**
	 * Option to switch post processing behavour
	 */
	public enum PostProcessOption {

		enabled,

		/**
		 * Does not run the post-processing.
		 */
		disabled,

		/**
		 * Does not run the simulation, but only post-processing.
		 */
		post_process_only

	}
}
