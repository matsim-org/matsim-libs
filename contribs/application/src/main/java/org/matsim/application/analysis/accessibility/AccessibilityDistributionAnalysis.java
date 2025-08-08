package org.matsim.application.analysis.accessibility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


@CommandLine.Command(
	name = "accessibility-offline", description = "Offline Accessibility Distribution analysis.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"%s/accessibilities_distribution.csv"
	}
)




public class AccessibilityDistributionAnalysis implements MATSimAppCommand {


	private static final Logger log = LogManager.getLogger(AccessibilityDistributionAnalysis.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(AccessibilityDistributionAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(AccessibilityDistributionAnalysis.class);


	public static void main(String[] args) {
		new AccessibilityDistributionAnalysis().execute(args);
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
			List<File> files = Arrays.stream(Objects.requireNonNull(folder.listFiles((dir, name) -> name.endsWith("accessibilities_simwrapper.csv")))).toList();

			for(File file : files) {

				String filePath = file.getAbsolutePath();

				String outputPath = file.getAbsolutePath().replace("accessibilities_simwrapper.csv", "accessibilities_distribution.csv");

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

					IntColumn population = table.intColumn("population");

					// === Define breakpoints here ===
					List<Double> breakpoints = Arrays.asList(-10.0, -8., -6., -4., -2., 0.0);  // You can change this list as needed
					Collections.sort(breakpoints);  // Ensure they are sorted

					// === Identify accessibility columns ===
					List<String> accessCols = new ArrayList<>();
					for (Column<?> col : table.columns()) {
						if (col.name().endsWith("_accessibility") && col.type() == ColumnType.DOUBLE) {
							accessCols.add(col.name());
						}
					}


					// === Compute max_accessibility for each row ===
					DoubleColumn maxAccessCol = DoubleColumn.create("max_accessibility");

					for (int row = 0; row < table.rowCount(); row++) {
						double max = Double.NEGATIVE_INFINITY;
						for (String colName : accessCols) {
							double val = table.doubleColumn(colName).get(row);
							if (!Double.isNaN(val)) {
								max = Math.max(max, val);
							}
						}
						// If all were NaN, set as NaN
						maxAccessCol.append(max == Double.NEGATIVE_INFINITY ? Double.NaN : max);
					}

					// Add the new column to the table
					table.addColumns(maxAccessCol);
					accessCols.add(maxAccessCol.name());

					// === Create bin labels ===
					List<String> binLabels = new ArrayList<>();
					binLabels.add("< " + breakpoints.get(0));
					for (int i = 0; i < breakpoints.size() - 1; i++) {
						binLabels.add("[" + breakpoints.get(i) + ", " + breakpoints.get(i + 1) + "]");
					}
					binLabels.add("> " + breakpoints.get(breakpoints.size() - 1));

					// === Initialize result table ===
					StringColumn binCol = StringColumn.create("bin", binLabels);
					Table result = Table.create("Accessibility Equity").addColumns(binCol);

					// === Compute population sums per bin and per mode ===
					for (String colName : accessCols) {
						DoubleColumn access = table.doubleColumn(colName);
						List<Double> counts = new ArrayList<>();

						// Below lowest
						counts.add(population.where(access.isLessThan(breakpoints.get(0))).sum());

						// Between breakpoints
						for (int i = 0; i < breakpoints.size() - 1; i++) {
							double lower = breakpoints.get(i);
							double upper = breakpoints.get(i + 1);
							counts.add(population.where(access.isGreaterThanOrEqualTo(lower)
								.and(access.isLessThanOrEqualTo(upper))).sum());
						}

						// Above highest
						counts.add(population.where(access.isGreaterThan(breakpoints.get(breakpoints.size() - 1))).sum());

						// Add to result table
						DoubleColumn modeCol = DoubleColumn.create(colName, counts);
						result.addColumns(modeCol);
					};

					// Save to CSV
					result.write().usingOptions(CsvWriteOptions.builder(outputPath).build());

					// Also print for confirmation
					System.out.println(result.print());

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}



		return 0;
	}


}
