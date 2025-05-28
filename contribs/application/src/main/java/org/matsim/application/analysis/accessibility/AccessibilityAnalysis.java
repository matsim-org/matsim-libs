package org.matsim.application.analysis.accessibility;

import it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.CSVWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.contrib.accessibility.AccessibilityModule.CONFIG_FILENAME_ACCESSIBILITY;


@CommandLine.Command(
	name = "accessibility-offline", description = "Offline Accessibility analysis.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"%s/accessibilities_simwrapper.csv"
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


		Set<String> activityOptions = null;
		try {
			activityOptions = Files.list(input.getRunDirectory().resolve("analysis/accessibility/"))
				.filter(Files::isDirectory)
				.map(Path::getFileName)
				.map(Path::toString).
				collect(Collectors.toSet());
		} catch (IOException e) {
			e.printStackTrace();
		}




		for (String activityOption : activityOptions) {
			String filePath = input.getRunDirectory() + "/analysis/accessibility/" + activityOption + "/accessibilities.csv";
			String outputPath = input.getRunDirectory() + "/analysis/accessibility/" + activityOption + "/accessibilities_simwrapper.csv";

			try {
				Path path = Path.of(outputPath);
				if (Files.exists(path)) {
					Files.delete(path);
					System.out.println("File deleted: " + outputPath);
				} else {
					System.out.println("File does not exist: " + outputPath);
				}
			} catch (IOException e) {
				System.err.println("Failed to delete file: " + e.getMessage());
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
				DoubleColumn xCol = table.doubleColumn("xcoord");
				DoubleColumn yCol = table.doubleColumn("ycoord");
				xCol.setName("x");
				yCol.setName("y");

				// Count Population per Pixel

				String populationPath = ApplicationUtils.matchInput("output_plans", input.getRunDirectory()).toAbsolutePath().toString();
				Config config = ConfigUtils.loadConfig(input.getRunDirectory() + "/analysis/accessibility/" + activityOption + "/" + CONFIG_FILENAME_ACCESSIBILITY);

				AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);

				LongColumn popCol = LongColumn.create("population");
				LongColumn ageCol = LongColumn.create("ageColumn");
				DoubleColumn incomeCol = DoubleColumn.create("incomeColumn");


				for (int i = 0; i < table.rowCount(); i++) {
					popCol.append(0L);
					ageCol.append(0);
					incomeCol.append(0.0);
				}



				Scenario scenario = ScenarioUtils.createScenario(config);


				new PopulationReader(scenario).readFile(populationPath);


				Int2DoubleMap incToCount = new Int2DoubleArrayMap();
				Int2DoubleMap incToPtAccess = new Int2DoubleArrayMap();

				for (Person person : scenario.getPopulation().getPersons().values()) {
					Double homeX = (Double) person.getAttributes().getAttribute("home_x");
					Double homeY = (Double) person.getAttributes().getAttribute("home_y");

					if (homeX == null || homeY == null) {
						continue; // Skip this person if home coordinates are not available
					}

					if(homeX > acg.getBoundingBoxRight() || homeX < acg.getBoundingBoxLeft() || homeY > acg.getBoundingBoxTop() || homeY < acg.getBoundingBoxBottom()) {
						continue;
					}

					int age = (Integer) person.getAttributes().getAttribute("age");
					double income = (Double) person.getAttributes().getAttribute("income");
					int incClass = Integer.parseInt((String) person.getAttributes().getAttribute("MiD:hheink_gr2"));



					// Find the closest pixel in the table
					double closestDistance = Double.MAX_VALUE;
					int closestRow = -1;
					for (int i = 0; i < table.rowCount(); i++) {
						double pixelX = xCol.getDouble(i);
						double pixelY = yCol.getDouble(i);
						double distance = Math.sqrt(Math.pow(homeX - pixelX, 2) + Math.pow(homeY - pixelY, 2));

						if (distance < closestDistance) {
							closestDistance = distance;
							closestRow = i;
						}
					}
					popCol.set(closestRow, popCol.getLong(closestRow) + 1);
					incomeCol.set(closestRow, incomeCol.getDouble(closestRow) + income);
					ageCol.set(closestRow, ageCol.getLong(closestRow) + age);

					// now the other way around
					double incCnt = incToCount.getOrDefault(incClass, 0);
					double ptAccess = incToPtAccess.getOrDefault(incClass, 0);
					incToCount.put(incClass, incCnt + 1);
					incToPtAccess.put(incClass, ptAccess + table.doubleColumn("pt_accessibility").getDouble(closestRow));
				}


				//
				for (int incGroup : incToCount.keySet()) {
					double count = incToCount.get(incGroup);
					double ptAccess = incToPtAccess.get(incGroup);
					if (count > 0) {
						incToPtAccess.put(incGroup, ptAccess / count);
					}
				}





				DoubleColumn ageAvg = ageCol.divide(popCol).setName("age");
				DoubleColumn incomeAvg = incomeCol.divide(popCol).setName("income");

				table.addColumns(popCol, ageAvg, incomeAvg);



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
