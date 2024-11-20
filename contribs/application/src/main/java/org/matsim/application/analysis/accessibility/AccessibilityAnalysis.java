package org.matsim.application.analysis.accessibility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.contrib.accessibility.AccessibilityFromEvents;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.ZonalSystemParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityOption;
import picocli.CommandLine;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(
	name = "accessibility-offline", description = "Offline Accessibility analysis.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"%s.csv"
	}
)
public class AccessibilityAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(AccessibilityAnalysis.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(AccessibilityAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(AccessibilityAnalysis.class);

//	@CommandLine.Option(names = "--grid-size", description = "Grid size in meter", defaultValue = "100")
//	private double gridSize;

	public static void main(String[] args) {
		new AccessibilityAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {


		//CONFIG
		// set necessary input files:
		Config config = ConfigUtils.loadConfig(ApplicationUtils.matchInput("config.xml", input.getRunDirectory()).toAbsolutePath().toString());
		config.controller().setOutputDirectory(input.getRunDirectory().toString());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.network().setInputFile(ApplicationUtils.matchInput("output_network.xml.gz", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setTransitScheduleFile(ApplicationUtils.matchInput("output_transitSchedule.xml.gz", input.getRunDirectory()).toAbsolutePath().toString());
		config.transit().setVehiclesFile(null);
		config.vehicles().setVehiclesFile(ApplicationUtils.matchInput("output_vehicles.xml.gz", input.getRunDirectory()).toAbsolutePath().toString());

		config.plans().setInputFile(ApplicationUtils.matchInput("output_plans.xml.gz", input.getRunDirectory()).toAbsolutePath().toString());
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.fromFile);
		config.facilities().setInputFile(ApplicationUtils.matchInput("output_facilities.xml.gz", input.getRunDirectory()).toAbsolutePath().toString());
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);

		for (DrtConfigGroup drtConfig : ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class).getModalElements()) {
			drtConfig.transitStopFile = "/Users/jakob/git/matsim-libs/examples/scenarios/kelheim/drt-stops.xml";
			drtConfig.removeParameterSet(drtConfig.getZonalSystemParams().get());
			drtConfig.plotDetailedCustomerStats = false;
		}
		config.routing().setRoutingRandomness(0);
		// todo: otherwise swissRailRaptor guice bindings are neccessary. But what if we need transit accessibility?
//		config.transit().setUseTransit(false);
		ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class );


		//SCENARIO
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Set<String> activityOptions = scenario.getActivityFacilities().getFacilities().values().stream().flatMap(fac -> fac.getActivityOptions().values().stream()).map(ActivityOption::getType).collect(Collectors.toSet());

		String eventsFile = ApplicationUtils.matchInput("output_events.xml.gz", input.getRunDirectory()).toString();

		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, eventsFile, List.of("trainStation","cityCenter"));
		builder.build().run();


		// read in, rename columns, and print out:

		for (String activityOption : activityOptions) {
			String filePath = input.getRunDirectory() + "/" + activityOption + "/accessibilities.csv";
			String outputPath = input.getRunDirectory() + "/analysis/accessibility/" + activityOption + ".csv";

			Path accAnalysisDirectory = Path.of(input.getRunDirectory() + "/analysis/accessibility/");
			if (!Files.exists(accAnalysisDirectory)) {
				try {
					// Create the directory
					Files.createDirectories(accAnalysisDirectory);
				} catch (IOException e) {
					System.out.println("Failed to create directory: " + e.getMessage());
				}
			}

			try {
				// Use CsvReadOptions to configure the CSV reading options
				CsvReadOptions options = CsvReadOptions.builder(filePath)
					.separator(',')        // Specify the separator if it's not a comma
					.header(true)          // Set to false if the file does not have a header
					.missingValueIndicator("") // Define how missing values are represented
					.build();

				// Read the CSV file into a Table object
				Table table = Table.read().csv(options);

				table.removeColumns("id");
				table.column("xcoord").setName("x");
				table.column("ycoord").setName("y");

				List<Column<?>> modCols = new ArrayList<>();
				for (Iterator<Column<?>> iterator = table.columns().iterator(); iterator.hasNext(); ) {
					Column<?> column = iterator.next();
					if (column.name().endsWith("_accessibility")) {
						DoubleColumn colMod = table.doubleColumn(column.name()).add(10).setName(column.name());
						modCols.add(colMod);
						iterator.remove();
					}
				}
				modCols.forEach(table::addColumns);

				// Write the modified table to a new CSV file
				CsvWriteOptions writeOptions = CsvWriteOptions.builder(outputPath)
					.separator(',') // Specify the separator if it's not a comma
					.header(true)   // Write the header to the output file
					.build();

				table.write().csv(writeOptions);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}



		return 0;
	}


}
