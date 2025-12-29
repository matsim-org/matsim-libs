package org.matsim.application.analysis.accessibility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;

import picocli.CommandLine;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
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
	name = "accessibility", description = "Accessibility analysis.",
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
