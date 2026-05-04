package org.matsim.application.analysis.scenarioComparison;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.CommandSpec;
import org.matsim.application.Dependency;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
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


@CommandLine.Command(name = "difference", description = "Calculates difference in amount of car hours traveled between base and policy case.")
@CommandSpec(requireRunDirectory = true, dependsOn = {
	@Dependency(value = TripAnalysis.class, files = "trip_stats.csv"),
	@Dependency(value = TripAnalysis.class, files = "population_trip_stats.csv"),
//	@Dependency(value = TripAnalysis.class, files = "mode_share_per_dist.csv"),
	@Dependency(value = AirPollutionAnalysis.class, files = "emissions_total.csv")
}, produces = {"trip_stats_comparison.csv", "population_trip_stats_comparison.csv"})

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

	public static void main(String[] args) {
		new ScenarioComparisonAnalysis().execute(args);
	}

	private static void JoinAndWriteCsv(Table simTable, List<Table> policyTables, String[] constructorCompNames, String outputFileName, String columnJoiner, boolean allowDuplicateCols) {
		Table result = null;

			int i = 0;
			for (Table t : policyTables) {
				for (String mode : new HashSet<>(t.columnNames())) {
					if (!mode.equals("Info")) {
						t.column(mode).setName(mode + "_" + constructorCompNames[i]);
					}
				}
				i++;
			}
			result = policyTables.stream()
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

		File simTripsFile = new File(input.getPath(TripAnalysis.class, "trip_stats.csv")).getAbsoluteFile();
		File simPopTripsFile = new File(input.getPath(TripAnalysis.class, "population_trip_stats.csv")).getAbsoluteFile();
//		File simModePerDistFile = new File(input.getPath(TripAnalysis.class, "mode_share_per_dist.csv")).getAbsoluteFile();

		log.info("Sim trips file exists: {}", simTripsFile.exists());
		log.info("Sim trips file path: {}", simTripsFile.getAbsoluteFile());
		log.info("Sim pop. trips file exists: {}", simPopTripsFile.exists());
		log.info("Sim pop. trips file path: {}", simPopTripsFile.getAbsoluteFile());

		List<File> policyTripFiles = new LinkedList<>();
		List<File> policyPopTripFiles = new LinkedList<>();
		List<File> policyModePerDistFiles = new LinkedList<>();
		String[] compPaths = this.constructorCompPaths.split(",");
		String[] compNames = this.constructorCompNames.split(",");

		for (String file  : compPaths) {
			policyTripFiles.add(new File(new File(file).getAbsoluteFile() + "/analysis/population/trip_stats.csv"));
			policyPopTripFiles.add(new File(new File(file).getAbsoluteFile() + "/analysis/population/population_trip_stats.csv"));
			policyModePerDistFiles.add(new File(new File(file).getAbsoluteFile() + "/analysis/population/mode_share_per_dist.csv"));
		}

		log.info("Policy trips file exists: {}", !policyTripFiles.isEmpty());
		log.info("Policy trip absolute paths: {}", policyTripFiles);
		log.info("Policy pop. trips file exists: {}", !policyPopTripFiles.isEmpty());
		log.info("Policy pop. trip absolute paths: {}", policyPopTripFiles);
		log.info("Policy mode. trips file exists: {}", !policyModePerDistFiles.isEmpty());
		log.info("Policy mode. trip absolute paths: {}", policyModePerDistFiles);

		if (policyTripFiles.isEmpty() || !simTripsFile.exists()) {
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


			List<Table> policyDataTables = new ArrayList<>();
			for (int i = 0; i < policyTripFiles.size(); i++) {
					try {
						policyDataTables.add(Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(policyTripFiles.get(i).toURL())).sample(false).separator(CsvOptions.detectDelimiter(policyTripFiles.get(i).getPath()))));
					} catch (IOException e) {
						throw new RuntimeException(e);
				}
			}

			JoinAndWriteCsv(simDataTable, policyDataTables, compNames, "trip_stats_comparison.csv", "Info",true);
		}

		if (policyPopTripFiles.isEmpty() || !simPopTripsFile.exists()) {
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

			List<Table> policyDataTables = new ArrayList<>();
			for (int i = 0; i < policyTripFiles.size(); i++) {
				try {
					policyDataTables.add(Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(policyPopTripFiles.get(i).toURL())).sample(false).separator(CsvOptions.detectDelimiter(policyPopTripFiles.get(i).getPath()))));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			JoinAndWriteCsv(simDataTable, policyDataTables, compNames,"population_trip_stats_comparison.csv", "Info",true);
		}

		return 0;
	}
}
