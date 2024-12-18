package org.matsim.application.analysis.impact;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.Dependency;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import picocli.CommandLine;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.util.Map;

@CommandLine.Command(
	name = "impact"
)
@CommandSpec(requireRunDirectory = true,
	dependsOn = {
		@Dependency(value = TripAnalysis.class, files = "trip_stats.csv"),
		@Dependency(value = AirPollutionAnalysis.class, files = "...")
	},
	produces = {
		"data.csv",
	}
)
public class ImpactAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(ImpactAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(ImpactAnalysis.class);

	@Override
	public Integer call() throws Exception {

		Config config = prepareConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Vehicles vehicles = scenario.getVehicles();

		System.out.println("Vehicles: ");

		for (Map.Entry<Id<Vehicle>, Vehicle> vehicleEntry : vehicles.getVehicles().entrySet()) {
			System.out.println(vehicleEntry.getKey());
			System.out.println(vehicleEntry.getValue());
		}


		String tripStatsPath = input.getPath("trip_stats.csv");
		System.out.println("Path: " + tripStatsPath);


		CsvReadOptions options = CsvReadOptions.builder(tripStatsPath)
			.header(true)
			.separator(',')
			.build();


		Table tripStatsTable = Table.read().usingOptions(options);
		System.out.println("TABLE");
		System.out.println(tripStatsTable.print());

		return 0;
	}

	private Config prepareConfig() {
		Config config = ConfigUtils.loadConfig(ApplicationUtils.matchInput("config.xml", input.getRunDirectory()).toAbsolutePath().toString());

		config.vehicles().setVehiclesFile(ApplicationUtils.matchInput("vehicles", input.getRunDirectory()).toAbsolutePath().toString());
		config.network().setInputFile(ApplicationUtils.matchInput("network", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setTransitScheduleFile(ApplicationUtils.matchInput("transitSchedule", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setVehiclesFile(ApplicationUtils.matchInput("transitVehicles", input.getRunDirectory()).toAbsolutePath().toString());
		config.plans().setInputFile(null);
		config.facilities().setInputFile(null);
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);

		return config;
	}
}
