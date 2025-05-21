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
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CommandLine.Command(
	name = "prepare-pois", description = "Prepare POIs.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"%s/pois_simwrapper.csv"
	}
)




public class PreparePois implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(PreparePois.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(PreparePois.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(PreparePois.class);


	public static void main(String[] args) {
		new PreparePois().execute(args);
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
			String outputPath = input.getRunDirectory() + "/analysis/accessibility/" + activityOption + "/pois_simwrapper.csv";
			String inputPath = input.getRunDirectory() + "/analysis/accessibility/" + activityOption + "/pois.csv";

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
				CsvReadOptions options = CsvReadOptions.builder(inputPath)
					.separator(',')        // Specify the separator if it's not a comma
					.header(true)          // Set to false if the file does not have a header
					.missingValueIndicator("") // Define how missing values are represented
					.build();

				// Read the CSV file into a Table object
				Table table = Table.read().csv(options);

//				table.removeColumns("id");
				table.column("xcoord").setName("x");
				table.column("ycoord").setName("y");

				String comment = "# EPSG:25832\n";

				try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
					writer.write(comment);
					table.write().csv(writer);
				}


			} catch (Exception e) {
				e.printStackTrace();
			}
		}



		return 0;
	}


}
