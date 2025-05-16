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
				table.column("xcoord").setName("x");
				table.column("ycoord").setName("y");

				//added 10 to accessibility because grid map can't currently handle negative vals
				int modifier = 0;
				List<Column<?>> modCols = new ArrayList<>();
				DoubleColumn walkColMod = null;
				for (Iterator<Column<?>> iterator = table.columns().iterator(); iterator.hasNext(); ) {
					Column<?> column = iterator.next();
					if (column.name().equals("teleportedWalk_accessibility")) {
						walkColMod = table.doubleColumn(column.name()).add(modifier).setName(column.name());
						break;
					}
				}

				for (Iterator<Column<?>> iterator = table.columns().iterator(); iterator.hasNext(); ) {
					Column<?> column = iterator.next();
					if (column.name().endsWith("_accessibility")) {
						DoubleColumn colMod = table.doubleColumn(column.name()).add(modifier).setName(column.name());
						modCols.add(colMod);
						if(walkColMod!=null){
							DoubleColumn modColWithoutWalk = colMod.subtract(walkColMod).setName(column.name() + "_diff");
							modCols.add(modColWithoutWalk);
						}
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
