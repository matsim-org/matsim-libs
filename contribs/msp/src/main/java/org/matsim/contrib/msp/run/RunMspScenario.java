package org.matsim.contrib.msp.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import picocli.CommandLine;

/**
 * Run script for MSP scenarios.
 * <p>
 * This is the generic entry point of the MSP contrib. It does not contain any scenario specific
 * setup; the config is passed in via {@code --config}. Scenario specific run classes should extend
 * {@link MATSimApplication} themselves (and may use this class as a blueprint) instead of adding
 * special cases here.
 * <p>
 * Usage:
 * <pre>
 *   java -cp matsim-msp.jar org.matsim.contrib.msp.run.RunMspScenario run --config path/to/config.xml
 * </pre>
 *
 * @see MATSimApplication
 */
@CommandLine.Command(header = ":: MSP Scenario ::", version = "1.0")
public class RunMspScenario extends MATSimApplication {

	/**
	 * Default constructor, needed by picocli to instantiate the class from the command line.
	 */
	public RunMspScenario() {
		super();
	}

	/**
	 * Constructor for a config that is set up in code, e.g. from a test.
	 */
	public RunMspScenario(Config config) {
		super(config);
	}

	public static void main(String[] args) {
		MATSimApplication.run(RunMspScenario.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		// nothing to do yet
	}

	@Override
	protected void prepareControler(Controler controler) {
		// nothing to bind yet
	}
}
