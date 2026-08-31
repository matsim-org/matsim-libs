package org.matsim.contrib.pseudosimulation.distributed;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** Defines and parses the legacy command line accepted by {@link SlaveControler}. */
final class SlaveCommandLineParser {

	private final Options options = createOptions();

	SlaveLaunchArguments parse(String[] arguments) throws ParseException {
		CommandLine commandLine = new DefaultParser().parse(options, arguments);
		String configFile = commandLine.hasOption("c")
				? commandLine.getOptionValue("c")
				: firstPositionalArgument(commandLine);
		return new SlaveLaunchArguments(
				configFile,
				commandLine.getOptionValue("h"),
				commandLine.getOptionValue("p"),
				commandLine.getOptionValue("t"));
	}

	String optionsDescription() {
		return options.toString();
	}

	@SuppressWarnings("deprecation") // Commons CLI 1.11 has no non-deprecated formatter replacement.
	void printHelp() {
		String header = "The MasterControler takes the following options:\n\n";
		String footer = "";
		new HelpFormatter().printHelp("MasterControler", header, options, footer, true);
	}

	private String firstPositionalArgument(CommandLine commandLine) {
		return commandLine.getArgs().length > 0 ? commandLine.getArgs()[0] : null;
	}

	private Options createOptions() {
		Options commandLineOptions = new Options();
		commandLineOptions.addOption(Option.builder("c")
				.longOpt("config")
				.desc("Config file location")
				.hasArg(true)
				.argName("CONFIG.XML")
				.required(false)
				.get());
		commandLineOptions.addOption("h", "host", true, "Host name or IP");
		commandLineOptions.addOption("p", "port", true, "Port number of MasterControler");
		commandLineOptions.addOption("t", "threads", true, "Number of threads for replanning.");
		return commandLineOptions;
	}
}
