package org.matsim.application;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.AutoComplete;
import picocli.CommandLine;

import jakarta.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
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

	private static final String ARGS_DELIMITER = "§$";

	public static final String COLOR = "@|bold,fg(81) ";
	static final String DEFAULT_NAME = "MATSimApplication";
	static final String HEADER = COLOR +
			"  __  __   _ _____ ___ _       \n" +
			" |  \\/  | /_\\_   _/ __(_)_ __  \n" +
			" | |\\/| |/ _ \\| | \\__ \\ | '  \\ \n" +
			" |_|  |_/_/ \\_\\_| |___/_|_|_|_|\n|@";

	@CommandLine.Option(names = "--config", description = "Path to config file used for the run.", order = 0)
	protected String configPath;

	@CommandLine.Option(names = "--yaml", description = "Path to yaml file with config params to overwrite.", required = false)
	protected String specs;

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
	 * @param defaultConfigPath path to the default scenario config
	 */
	public MATSimApplication(@Nullable String defaultConfigPath) {
		this.defaultScenario = defaultConfigPath;
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
			String path = Objects.requireNonNull( configPath, "No default scenario location given" );
			List<ConfigGroup> customModules = getCustomModules();

			final Config config1 = ConfigUtils.loadConfig(IOUtils.resolveFileOrResource(path), customModules.toArray(new ConfigGroup[0] ) );
			Config prepared = prepareConfig( config1 );

			config = prepared != null ? prepared : config1;
			// (The above lines of code come from inlining so maybe it happened there: I cannot see how prepared could be null but config1 not except if user code returns null which I would consider a bug.  kai, aug'24)
		} else {
			Config tmp = prepareConfig(config);
			config = tmp != null ? tmp : config;
		}

		Objects.requireNonNull(config);

		if (specs != null)
			ApplicationUtils.applyConfigUpdate(config, IOUtils.resolveFileOrResource(specs));

		if (remainingArgs != null) {
			String[] args = remainingArgs.stream().map(s -> s.replace("-c:", "--config:")).toArray(String[]::new);
			ConfigUtils.applyCommandline(config, args);
		}

		if (iterations > -1)
			config.controller().setLastIteration(iterations);

		if (output != null)
			config.controller().setOutputDirectory(output.toString());

		if (runId != null)
			config.controller().setRunId(runId);

		final Scenario scenario = createScenario(config);

		prepareScenario(scenario);

		final Controler controler = new Controler(scenario);

		prepareControler(controler);

		// Check if simulation needs to be run
		if (post != PostProcessOption.post_process_only)
			controler.run();

		if (post != PostProcessOption.disabled) {

			List<MATSimAppCommand> commands = preparePostProcessing(Path.of(config.controller().getOutputDirectory()), config.controller().getRunId());

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

	String getConfigPath() {
		return configPath;
	}

	@Nullable
	String getDefaultScenario() {
		return defaultScenario;
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
	protected List<ConfigGroup> getConfigurableModules() {
		return Lists.newArrayList(
				new ControllerConfigGroup(),
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
	 * Allows scenario creation with other loadScenario signatures
	 * e.g. with AttributeConverter
	 */
	protected Scenario createScenario(Config config) {
		return ScenarioUtils.loadScenario(config);
	}

	/**
	 * Preparation of {@link MATSimAppCommand} to run after the simulation has finished. The instances have to be fully constructed in this method
	 * no further arguments are passed down to them.
	 *
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

		String outputDir = config.controller().getOutputDirectory();
		if (outputDir.endsWith("/")) {
			config.controller().setOutputDirectory(outputDir.substring(0, outputDir.length() - 1) + postfix + "/");
		} else
			config.controller().setOutputDirectory(outputDir + postfix);

		// dot should not be part of run id
		config.controller().setRunId(config.controller().getRunId() + postfix.replace(".", ""));
	}

	/**
	 * Add and an option to run id and output folder, delimited by "_".
	 */
	protected final void addRunOption(Config config, String option) {
		addRunOption(config, option, "");
	}

	@Override
	public String defaultValue(CommandLine.Model.ArgSpec argSpec) throws Exception {
		Object obj = argSpec.userObject();
		if (obj instanceof Field field) {
			// Make sure default config path is propagated to the field
			if (field.getName().equals("configPath") && field.getDeclaringClass().equals(MATSimApplication.class)) {
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
		MATSimApplication app = newInstance(clazz, null);

		// GUI does not pass any argument
		boolean runInGUi = "true".equals(System.getenv("MATSIM_GUI"));
		if (runInGUi) {

			// prepend necessary options
			List<String> l = Lists.newArrayList("run", "--config");
			l.addAll(Arrays.asList(args));

			args = l.toArray(new String[0]);

			// Pass stored args to the instance as well
			if (System.getenv().containsKey("MATSIM_GUI_ARGS")) {
				String[] guiArgs = System.getenv("MATSIM_GUI_ARGS").split(ARGS_DELIMITER);
				if (guiArgs.length > 0)
					args = ApplicationUtils.mergeArgs(args, guiArgs);
			}
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
	 * <p>Convenience method to run a scenario from code or automatically with gui when desktop application is detected.
	 * This method may also be used to predefine some default arguments.</p>
	 *
	 * <p>With respect to args it looks like arguments are treated in the following sequence (programmed in the run method):
	 * <ul>
	 *         <li>ConfigUtils.loadConfig without args</li>
	 *         <li>prepareConfig which is usually overwritten</li>
	 *         <li>config options from some yaml file which can be provided as a command line option</li>
	 *         <li>config options on command line </li>
	 * </ul></p>
	 *
	 * <p>defaultArgs could be used to provide defaults when calling this method here; they would go in addition to what is coming in from "upstream" which is typically the command line.</p>
	 *
	 * <p>There are many execution paths that can be reached from this class, but a typical one for matsim-scenarios seems to be:<ul>
	 * <li> This method runs MATSimApplication.run( TheScenarioClass.class , args ).</li>
	 * <li> That run class will instantiate an instance of TheScenarioClass (*), then do some args consistenty checking, then call the piccoli execute method. </li>
	 * <li> The piccoli execute method will essentially call the "call" method of MATSimApplication. </li>
	 * <li> I think that in the described execution path, this.config in that call method will initially be null.  (The ctor of MATSimApplication was called via reflection at (*); I think that it was called there without a config argument.) </li>
	 * <li> This call method then will do: <ul>
	 * 		<li> getCustomModules() (which is empty by default but can be overriden) </li>
	 * 		<li>ConfigUtils.loadConfig(...) _without_ passing on the args</li>
	 * 		<li>prepareConfig(...) (which is empty by default but is typically overridden, in this case in OpenBerlinScenario).  In our case, this sets the typical scoring params and the typical replanning strategies.
	 * 		<li>next one can override the config from some yaml file provided as a commandline option
	 * 		<li> next args is parsed and set
	 * 		<li>then some standard CL options are detected and set
	 * 		<li>then createScenario(config) is called (which can be overwritten but is not)
	 * 		<li>then prepareScenario(scenario) is called (which can be overwritten but is not)
	 * 		<li>then a standard controler is created from scenario
	 * 		<li>then prepareControler is called which can be overwritten
	 * 	</ul>
	 * 	</ul>
	 * 	</p>
	 *
	 * @param clazz class of the scenario to run
	 * @param args pass arguments from the main method
	 * @param defaultArgs predefined default arguments that will always be present
	 */
	public static void runWithDefaults(Class<? extends MATSimApplication> clazz, String[] args, String... defaultArgs) {

		if (ApplicationUtils.isRunFromDesktop() && args.length == 0) {

			System.setProperty("MATSIM_GUI_DESKTOP", "true");

			if (defaultArgs.length > 0) {
				String value = String.join(ARGS_DELIMITER, defaultArgs);
				System.setProperty("MATSIM_GUI_ARGS", value);
			}

			// args are empty when run from desktop and is not used
			run(clazz, "gui");

		} else {
			// run if no other command is present
			if (args.length > 0) {
				// valid command is present
				if (args[0].equals("run") || args[0].equals("prepare") || args[0].equals("analysis") || args[0].equals("gui") ){
					// If this is a run command, the default args can be applied
					if (args[0].equals("run"))
						args = ApplicationUtils.mergeArgs(args, defaultArgs);

				} else {
					// Automatically add run command
					String[] runArgs = ApplicationUtils.mergeArgs(new String[]{"run"}, defaultArgs);
					args = ApplicationUtils.mergeArgs(runArgs, args);
				}

			} else
				// Automatically add run command
				args = ApplicationUtils.mergeArgs(new String[]{"run"}, defaultArgs);

			log.info("Running {} with: {}", clazz.getSimpleName(), String.join(" ", args));

			run(clazz, args);
		}
	}

	/**
	 * Calls an application class and forwards any exceptions.
	 *
	 * @param config input config, overwrites default given by scenario
	 * @return return code, 0 indicates success and no errors
	 */
	public static int execute(Class<? extends MATSimApplication> clazz, Config config, String... args) {
		MATSimApplication app = newInstance(clazz, config);

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
	public static Controler prepare(MATSimApplication app, Config config, String... args) {
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
			ApplicationUtils.applyConfigUpdate(config, IOUtils.resolveFileOrResource(app.specs));
		}

		if (app.remainingArgs != null) {
			String[] extraArgs = app.remainingArgs.stream().map(s -> s.replace("-c:", "--config:")).toArray(String[]::new);
			ConfigUtils.applyCommandline(config, extraArgs);
		}

		final Scenario scenario = app.createScenario(config);
		app.prepareScenario(scenario);

		final Controler controler = new Controler(scenario);
		app.prepareControler(controler);

		return controler;
	}

	/**
	 * Prepare and return controller without running the scenario.
	 * This allows to configure the controller after setup has been run.
	 * This method tries to use one of the constructors of the given class automatically.
	 *
	 * @see #prepare(MATSimApplication, Config, String...)
	 */
	public static Controler prepare(Class<? extends MATSimApplication> clazz, Config config, String... args) {

		MATSimApplication app = newInstance(clazz, config);

		return prepare(app, config, args);
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

	private static MATSimApplication newInstance(Class<? extends MATSimApplication> clazz, Config config) {

		// Try constructor with config first
		// if that fails try default constructor
		if (config != null) {
			try {
				return clazz.getDeclaredConstructor(Config.class).newInstance(config);
			} catch (NoSuchMethodException e) {
				// Continue
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Could not instantiate the application class", e);
			}
		}

		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("The scenario class must have public constructors!", e);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Could not instantiate the application class", e);
		}

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

	@CommandLine.Command(name = "prepare", description = "Contains all commands for preparing the scenario. (See prepare help; \n" +
									     "  needs to be ... \"prepare\", \"help\" ) if run from Java.)",
			// (This used to be "help prepare", which works as well.  However, "prepare help <subcommand>" then also works while "help
			// prepare <subcommand>" does not.  So I think that "prepare help" saves some time in understanding help options.  kai, nov'22)
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
	 * Option to switch post-processing behaviour
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
