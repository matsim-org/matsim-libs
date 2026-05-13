package org.matsim.application.analysis.scenarioComparison;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.CommandSpec;
import org.matsim.application.Dependency;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.application.analysis.impact.ImpactAnalysis;
import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.joining.DataFrameJoiner;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.joining.JoinType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@CommandLine.Command(name = "difference", description = "Calculates difference in amount of car hours traveled between base and comparison case.")
@CommandSpec(requireRunDirectory = true, dependsOn = {
	@Dependency(value = TripAnalysis.class, files = "population_trip_stats.csv"),
	@Dependency(value = TripAnalysis.class, files = "mode_share.csv"),  // anchor for path resolution
	@Dependency(value = ImpactAnalysis.class, files = "emissions_%s.csv"),
	@Dependency(value = ImpactAnalysis.class, files = "general_%s.csv")
}, produces = {"trip_stats_comparison.csv", "population_trip_stats_comparison.csv", "general_impact_comparison.csv", "emissions_comparison.csv"})

public class ScenarioComparisonAnalysis implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(ScenarioComparisonAnalysis.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(ScenarioComparisonAnalysis.class);

	@CommandLine.Mixin
	private static final OutputOptions output = OutputOptions.ofCommand(ScenarioComparisonAnalysis.class);

	@CommandLine.Option(names = "--input-comp-paths", description = "Files with scenario comparison data")
	private String constructorCompPaths;

	@CommandLine.Option(names = "--input-comp-names", description = "Names of scenario comparisons")
	private String constructorCompNames;

	@CommandLine.Option(names = "--input-comp-args", description = "Arguments used in selected scenario comparisons")
	private String constructorCompArgs;

	public static void main(String[] args) {
		new ScenarioComparisonAnalysis().execute(args);
	}

	private static void JoinAndWriteCsv(Table simTable, List<Table> compTables, String[] constructorCompNames, String outputFileName, String[] columnJoiner, boolean allowDuplicateCols) {
		Table result = null;

			int i = 0;
			for (Table t : compTables) {
				for (String mode : new HashSet<>(t.columnNames())) {
						if (!Arrays.asList(columnJoiner).contains(mode)) {
							t.column(mode).setName(mode + "_" + constructorCompNames[i]);
					}
				}
				i++;
			}

			result = compTables.stream()
				.reduce(simTable, (left, right) ->
					new DataFrameJoiner(left, columnJoiner)
						.type(JoinType.LEFT_OUTER)
						.with(right)
						.allowDuplicateColumnNames(allowDuplicateCols)
						.join()
				);

			List<String> sortedColumns = result.columnNames().stream()
				.sorted()
				.collect(Collectors.toList());

			result = result.reorderColumns(sortedColumns.toArray(new String[0]));

		for (Column<?> col : new ArrayList<>(result.columns())) {
			if (col instanceof DoubleColumn || col instanceof FloatColumn) {
				StringColumn formatted = StringColumn.create(col.name());
				for (int j = 0; j < col.size(); j++) {
					if (col.isMissing(j)) {
						formatted.append("");
					} else {
						// hacky way to make all numbers have same amount if digits for alignment in table
						double val = ((NumericColumn<?>) col).getDouble(j);
						formatted.append("\u2060" + String.format("%.2f", val));
					}
					log.info("j={}, col.size()={}, formatted.size()={}", j, col.size(), formatted.size());
				}
				log.info("Final: col.size()={}, formatted.size()={}", col.size(), formatted.size());
				result.replaceColumn(col.name(), formatted);
			}
		}

		result = result.reorderColumns(sortedColumns.toArray(new String[0]));

		result.write().csv(output.getPath(outputFileName).toString());

	}

	@Override
	public Integer call() throws Exception {

		// added new logic in case simulation ran with old TripAnalysis.class
		// Try to find any concrete file matching the pattern "trip_stats_*.csv"
		File inputDir = new File(input.getPath(TripAnalysis.class, "mode_share.csv")).getParentFile();

		File[] matchingFiles = inputDir.listFiles(
			(dir, name) -> name.startsWith("trip_stats") && name.endsWith(".csv")
		);

		File simTripsFile;
		if (matchingFiles != null && matchingFiles.length > 0) {
			// New naming convention — pick the first match (or add logic to select the right one)
			simTripsFile = matchingFiles[0].getAbsoluteFile();
		} else {
			// Fall back to old naming convention
			simTripsFile = new File(inputDir, "trip_stats.csv").getAbsoluteFile();
		}

		File simEmissionsFile = new File(input.getPath(ImpactAnalysis.class, "emissions_%s.csv").replace("%s", "car")).getAbsoluteFile();
		File simGenImpactFile = new File(input.getPath(ImpactAnalysis.class, "general_%s.csv").replace("%s", "car")).getAbsoluteFile();


		File simPopTripsFile = new File(input.getPath(TripAnalysis.class, "population_trip_stats.csv")).getAbsoluteFile();
//		File simModePerDistFile = new File(input.getPath(TripAnalysis.class, "mode_share_per_dist.csv")).getAbsoluteFile();

		log.info("Sim trips file exists: {}", simTripsFile.exists());
		log.info("Sim trips file path: {}", simTripsFile.getAbsoluteFile());
		log.info("Sim pop. trips file exists: {}", simPopTripsFile.exists());
		log.info("Sim pop. trips file path: {}", simPopTripsFile.getAbsoluteFile());

		List<File> compTripFiles = new LinkedList<>();
		List<File> compEmissionsFiles = new LinkedList<>();
		List<File> compGenImpactFiles = new LinkedList<>();
		List<File> compPopTripFiles = new LinkedList<>();
		List<File> compModePerDistFiles = new LinkedList<>();
		String[] compPaths = this.constructorCompPaths.split(",");
		String[] compNames = this.constructorCompNames.split(",");

		for (String file  : compPaths) {
			// added new logic in case simulation ran with old TripAnalysis.class
			File compDir = new File(new File(file).getAbsoluteFile() + "/analysis/population/");

			File[] matchingCompFiles = compDir.listFiles(
				(dir, name) -> name.startsWith("trip_stats") && name.endsWith(".csv")
			);

			if (matchingCompFiles != null) {
				for (File f : matchingCompFiles) {
					compTripFiles.add(f);
				}
			}

//			compTripFiles.add(new File(new File(file).getAbsoluteFile() + "/analysis/population/trip_stats.csv"));
			compPopTripFiles.add(new File(new File(file).getAbsoluteFile() + "/analysis/population/population_trip_stats.csv"));
			compModePerDistFiles.add(new File(new File(file).getAbsoluteFile() + "/analysis/population/mode_share_per_dist.csv"));
			compEmissionsFiles.add(new File(new File(file).getAbsoluteFile() + "/analysis/impact/emissions_car.csv"));
			compGenImpactFiles.add(new File(new File(file).getAbsoluteFile() + "/analysis/impact/general_car.csv"));
		}

		log.info("comp trips file exists: {}", !compTripFiles.isEmpty());
		log.info("comp trip absolute paths: {}", compTripFiles);
		log.info("comp pop. trips file exists: {}", !compPopTripFiles.isEmpty());
		log.info("comp pop. trip absolute paths: {}", compPopTripFiles);
		log.info("comp mode. trips file exists: {}", !compModePerDistFiles.isEmpty());
		log.info("comp mode. trip absolute paths: {}", compModePerDistFiles);
		log.info("comp emissions file exists: {}", !compEmissionsFiles.isEmpty());
		log.info("comp emissions absolute paths: {}", compEmissionsFiles);
		log.info("comp general impact file exists: {}", !compGenImpactFiles.isEmpty());
		log.info("comp general impact absolute paths: {}", compGenImpactFiles);


		if (compTripFiles.isEmpty() || !simTripsFile.exists()) {
			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("trip_stats_comparison.csv").toString()), CSVFormat.DEFAULT)) {
				printer.printRecord("No trips files were found", 0, "user-group");
				log.warn("At least one trips file was not found. Please check log info to see which files are missing.");
			} catch (IOException ex) {
				log.error(ex);
			}
		} else {
			// make table data
			Table simDataTable = null;
			try {
				simDataTable = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(simTripsFile.toURL())).sample(false).separator(CsvOptions.detectDelimiter(simTripsFile.getPath())));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}


			List<Table> compDataTables = new ArrayList<>();
			for (int i = 0; i < compTripFiles.size(); i++) {
					try {
						compDataTables.add(Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(compTripFiles.get(i).toURL())).sample(false).separator(CsvOptions.detectDelimiter(compTripFiles.get(i).getPath()))));
					} catch (IOException e) {
						throw new RuntimeException(e);
				}
			}

			JoinAndWriteCsv(simDataTable, compDataTables, compNames, "trip_stats_comparison.csv", new String[]{"Info"},true);
		}

		if (compPopTripFiles.isEmpty() || !simPopTripsFile.exists()) {
			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("population_trip_stats_comparison.csv").toString()), CSVFormat.DEFAULT)) {
				printer.printRecord("No populations trip stats files were found", 0, "user-group");
				log.warn("At least one populations trip stats file was not found. Please check log info to see which files are missing.");
			} catch (IOException ex) {
				log.error(ex);
			}
		} else {
			// make table data
			Table simDataTable = null;
			try {
				simDataTable = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(simPopTripsFile.toURL())).sample(false).separator(CsvOptions.detectDelimiter(simPopTripsFile.getPath())));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			List<Table> compDataTables = new ArrayList<>();
			for (int i = 0; i < compPopTripFiles.size(); i++) {
				try {
					compDataTables.add(Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(compPopTripFiles.get(i).toURL())).sample(false).separator(CsvOptions.detectDelimiter(compPopTripFiles.get(i).getPath()))));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			JoinAndWriteCsv(simDataTable, compDataTables, compNames,"population_trip_stats_comparison.csv", new String[]{"Group"},true);
		}

		if (compEmissionsFiles.isEmpty() || !simEmissionsFile.exists()) {
			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("emissions_comparison.csv").toString()), CSVFormat.DEFAULT)) {
				printer.printRecord("No emisisons files were found", 0, "user-group");
				log.warn("At least one emisisons file was not found. Please check log info to see which files are missing.");
			} catch (IOException ex) {
				log.error(ex);
			}
		} else {
			// make table data
			Table simDataTable = null;
			try {
				simDataTable = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(simEmissionsFile.toURL())).sample(false).separator(CsvOptions.detectDelimiter(simEmissionsFile.getPath())));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			List<Table> compDataTables = new ArrayList<>();
			for (int i = 0; i < compEmissionsFiles.size(); i++) {
				try {
					compDataTables.add(Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(compEmissionsFiles.get(i).toURL())).sample(false).separator(CsvOptions.detectDelimiter(compEmissionsFiles.get(i).getPath()))));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			JoinAndWriteCsv(simDataTable, compDataTables, compNames,"emissions_comparison.csv", new String[]{"Description", "Unit"},true);
		}

		if (compGenImpactFiles.isEmpty() || !simGenImpactFile.exists()) {
			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("general_impact_comparison.csv").toString()), CSVFormat.DEFAULT)) {
				printer.printRecord("No emisisons files were found", 0, "user-group");
				log.warn("At least one emisisons file was not found. Please check log info to see which files are missing.");
			} catch (IOException ex) {
				log.error(ex);
			}
		} else {
			// make table data
			Table simDataTable = null;
			try {
				simDataTable = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(simGenImpactFile.toURL())).sample(false).separator(CsvOptions.detectDelimiter(simGenImpactFile.getPath())));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			List<Table> compDataTables = new ArrayList<>();
			for (int i = 0; i < compEmissionsFiles.size(); i++) {
				try {
					compDataTables.add(Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(compGenImpactFiles.get(i).toURL())).sample(false).separator(CsvOptions.detectDelimiter(compGenImpactFiles.get(i).getPath()))));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			JoinAndWriteCsv(simDataTable, compDataTables, compNames,"general_impact_comparison.csv", new String[]{"Description","Unit"},true);
		}

		return 0;
	}
}
