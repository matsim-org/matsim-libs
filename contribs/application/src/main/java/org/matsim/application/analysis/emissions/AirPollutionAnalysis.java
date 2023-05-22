package org.matsim.application.analysis.emissions;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.population.StuckAgentAnalysis;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

import java.nio.file.Path;

import static org.matsim.application.ApplicationUtils.globFile;

@CommandLine.Command(name = "air-pollution", description = "TODO")
@CommandSpec(requireRunDirectory = true, produces = {})

public class AirPollutionAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(AirPollutionAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(AirPollutionAnalysis.class);

	@CommandLine.Option(names = "--hbefa-warm", required = true)
	private String hbefaWarmFile;

	@CommandLine.Option(names = "--hbefa-cold", required = true)
	private String hbefaColdFile;

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions();

	public static void main(String[] args) {
		new AirPollutionAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(ApplicationUtils.matchInput("vehicles", input.getRunDirectory()));
		config.network().setInputFile(ApplicationUtils.matchInput("network", input.getRunDirectory()));
		config.transit().setTransitScheduleFile(ApplicationUtils.matchInput("transitSchedule", input.getRunDirectory()));
		config.transit().setVehiclesFile(ApplicationUtils.matchInput("transitVehicles", input.getRunDirectory()));
		config.global().setCoordinateSystem(crs.getInputCRS());
		config.plans().setInputFile(null);
		config.parallelEventHandling().setNumberOfThreads(null);
		config.parallelEventHandling().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);

		final String eventsFile = ApplicationUtils.matchInput("events", input.getRunDirectory());

		Scenario scenario = ScenarioUtils.loadScenario(config);

		return 0;
	}
}
