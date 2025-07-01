package org.matsim.application.analysis.accessibility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
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

import java.io.File;
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

			String folderNameForActivityOption = input.getRunDirectory() + "/analysis/accessibility/" + activityOption;
			File folder = new File(folderNameForActivityOption);
			List<File> files = Arrays.stream(Objects.requireNonNull(folder.listFiles((dir, name) -> name.endsWith("accessibilities.csv")))).toList();

			for(File file : files) {

				String filePath = file.getAbsolutePath();

				String outputPath = file.getAbsolutePath().replace("accessibilities.csv", "accessibilities_simwrapper.csv");

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

//					table.removeColumns("id");
					DoubleColumn xCol = table.doubleColumn("xcoord");
					DoubleColumn yCol = table.doubleColumn("ycoord");
					xCol.setName("x");
					yCol.setName("y");


					// COPY VALUES FOR OTHER MODES INTO ALL TIME SLICES

					table.sortOn("time", "id");
					Table filledTable = Table.create(table.name());

					for (Column<?> col : table.columns()) {
						filledTable.addColumns(col.emptyCopy());
					}


					table.addColumns(
						table.column("time").asStringColumn().setName("time_cat"),
						table.intColumn("id").asStringColumn().setName("id_cat")
					);

					// Group keys
					String[] groupCols = {"id_cat"};

					table.sortOn(groupCols);

					List<String> colNames = table.columnNames();

					table.splitOn(groupCols).forEach(slice -> {

						Table group = slice.asTable();

						for(String accessColName : colNames) {
							if (!accessColName.endsWith("accessibility")) {
								continue;
							}

							DoubleColumn accessCol = group.doubleColumn(accessColName);
							int nonMissingCount = (int) accessCol.asList().stream()
								.filter(d -> d != null && !d.isNaN())
								.count();

							if (nonMissingCount == 0) {
								throw new RuntimeException("Group with time/x/y has no accessibility value at all: " +
									group.getString(0, "time") + ", " +
									group.getString(0, "id"));
							} else if (nonMissingCount == 1) {
								double fillValue = accessCol.asList().stream()
									.filter(d -> d != null && !d.isNaN())
									.findFirst().get();  // safe because count == 1

								for (int i = 0; i < group.rowCount(); i++) {
									Double val = accessCol.getDouble(i);
									if (val == null || val.isNaN()) {
										accessCol.set(i, fillValue);
									}
								}
							}
						}
						// else: all values present, do nothing

						filledTable.append(group);
					});



					table = filledTable;


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

						// Find the closest pixel in the table
						double closestDistance = Double.MAX_VALUE;
						List<Integer> closestRows = new ArrayList<>();
						for (int i = 0; i < table.rowCount(); i++) {
							double pixelX = xCol.getDouble(i);
							double pixelY = yCol.getDouble(i);
							double distance = Math.sqrt(Math.pow(homeX - pixelX, 2) + Math.pow(homeY - pixelY, 2));

							if (distance < closestDistance) {
								closestDistance = distance;
								closestRows.clear();
								closestRows.add(i);
							} else if (distance == closestDistance) {
								closestRows.add(i);
							}
						}

						for (int closestRow : closestRows) {
							popCol.set(closestRow, popCol.getLong(closestRow) + 1);
							incomeCol.set(closestRow, incomeCol.getDouble(closestRow) + income);
							ageCol.set(closestRow, ageCol.getLong(closestRow) + age);
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

		}



		return 0;
	}


}
