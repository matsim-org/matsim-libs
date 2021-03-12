package org.matsim.application;

import com.google.common.collect.Lists;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.application.commands.ShowGUI;
import picocli.AutoComplete;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * There are also other variants of this method
 * <p>
 * This class also automatically registers classes from the {@link Prepare} and {@link Analysis} annotations as subcommands.
 * These can be used to build a pipeline with command needed for preparation analysis.
 *
 * @see #run(Class, String...)
 * @see #call(Class, Config, String...)
 * @see #prepare(Class, Config, String...)
 * @see Prepare
 * @see Analysis
 */
@CommandLine.Command(
        name = MATSimApplication.DEFAULT_NAME,
        description = {"", "If no subcommand is specified, this will run the scenario using the CONFIG"},
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
        subcommands = {CommandLine.HelpCommand.class, AutoComplete.GenerateCompletion.class, ShowGUI.class}
)
public abstract class MATSimApplication implements Callable<Integer>, CommandLine.IDefaultValueProvider {

    public static final String DEFAULT_NAME = "MATSimApplication";
    public static final String COLOR = "@|bold,fg(81) ";
    public static final String HEADER = COLOR +
            "  __  __   _ _____ ___ _       \n" +
            " |  \\/  | /_\\_   _/ __(_)_ __  \n" +
            " | |\\/| |/ _ \\| | \\__ \\ | '  \\ \n" +
            " |_|  |_/_/ \\_\\_| |___/_|_|_|_|\n|@";

    @CommandLine.Parameters(arity = "0..1", paramLabel = "CONFIG", description = "Scenario config used for the run.")
    protected File scenario;

    @CommandLine.Option(names = "--iterations", description = "Overwrite number of iterations (if greater than -1).", defaultValue = "-1")
    protected int iterations;

    /**
     * Path to the default scenario config, if applicable.
     */
    @Nullable
    protected final String defaultScenario;

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
            config = loadConfig(Objects.requireNonNull(scenario, "No default scenario location given").getAbsolutePath());
        } else {
            Config tmp = prepareConfig(config);
            config = tmp != null ? tmp : config;
        }

        Objects.requireNonNull(config);

        final Scenario scenario = ScenarioUtils.loadScenario(config);

        prepareScenario(scenario);

        final Controler controler = new Controler(scenario);

        prepareControler(controler);

        if (iterations > -1)
            config.controler().setLastIteration(iterations - 1);

        controler.run();
        return 0;
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
     * @param config initialized config
     * @return prepared {@link Config}, or null if same as input
     */
    @Nullable
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
     * Add and an option and value to run id and output folder.
     */
    protected void addRunOption(Config config, String option, Object value) {

        String postfix = "-" + option + "_" + value;

        String outputDir = config.controler().getOutputDirectory();
        if (outputDir.endsWith("/")) {
            config.controler().setOutputDirectory(outputDir.substring(0, outputDir.length() - 1) + postfix + "/");
        } else
            config.controler().setOutputDirectory(outputDir + postfix);

        config.controler().setRunId(config.controler().getRunId() + postfix);
    }

    /**
     * Add and an option to run id and output folder, delimited by "_".
     */
    protected void addRunOption(Config config, String option) {
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

        CommandLine cli = prepare(app);

        int code = cli.execute(args);
        System.exit(code);
    }

    /**
     * Calls an application class and forwards any exceptions.
     *
     * @param config input config, overwrites default given by scenario
     * @return return code, 0 indicates success and no errors
     */
    public static int call(Class<? extends MATSimApplication> clazz, Config config, String... args) {
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
     * @see #call(Class, Config, String...)
     */
    public static int call(Class<? extends MATSimApplication> clazz, String... args) {
        return call(clazz, null, args);
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
            System.err.println("Could not instantiate the application class");
            throw new RuntimeException(e);
        }

        CommandLine cli = prepare(app);
        CommandLine.ParseResult parseResult = cli.parseArgs(args);

        if (parseResult.errors().size() > 0)
            throw new RuntimeException(parseResult.errors().get(0));

        Config tmp = app.prepareConfig(config);
        config = tmp != null ? tmp : config;

        final Scenario scenario = ScenarioUtils.loadScenario(config);
        app.prepareScenario(scenario);

        final Controler controler = new Controler(scenario);
        app.prepareControler(controler);

        return controler;
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

    @CommandLine.Command(name = "prepare", description = "Contains all commands for preparing the scenario. (See help prepare)")
    public static class PrepareCommand implements Callable<Integer> {

        @CommandLine.Spec
        private CommandLine.Model.CommandSpec spec;

        @Override
        public Integer call() throws Exception {
            System.out.printf("No subcommand given. Chose on of: %s", spec.subcommands().keySet());
            return 0;
        }
    }

    @CommandLine.Command(name = "analysis", description = "Contains all commands for analysing the scenario. (See help analysis)")
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
        Class<?>[] value() default {};
    }

    /**
     * Classes from {@link #value()} will be registered as "analysis" subcommands.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Analysis {
        Class<?>[] value() default {};
    }

}
