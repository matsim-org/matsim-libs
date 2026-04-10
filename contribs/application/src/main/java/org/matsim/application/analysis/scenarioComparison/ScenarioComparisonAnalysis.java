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
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;


@CommandLine.Command(name = "difference", description = "Calculates difference in amount of car hours traveled between base and policy case.")
@CommandSpec(requireRunDirectory = true, dependsOn = {
	@Dependency(value = TripAnalysis.class, files = "trip_stats.csv"),
	@Dependency(value = TripAnalysis.class, files = "population_trip_stats.csv"),
	@Dependency(value = AirPollutionAnalysis.class, files = "emissions_total.csv")
}, produces = {"trip_stats_comparison.csv", "population_trip_stats_comparison.csv"})

public class ScenarioComparisonAnalysis implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(ScenarioComparisonAnalysis.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(ScenarioComparisonAnalysis.class);

	@CommandLine.Mixin
	private static final OutputOptions output = OutputOptions.ofCommand(ScenarioComparisonAnalysis.class);

	@CommandLine.Option(names = "--input-base-path", description = "File with reference data")
	private String constructorBasePath;

	public static void main(String[] args) {
		new ScenarioComparisonAnalysis().execute(args);
	}

	private static Map<String, ColumnType> getColumnTypes() {
		Map<String, ColumnType> columnTypes = new HashMap<>(Map.of("Info", ColumnType.STRING, "bike", ColumnType.DOUBLE, "car", ColumnType.DOUBLE, "ride", ColumnType.DOUBLE, "walk", ColumnType.DOUBLE, "pt", ColumnType.DOUBLE, "freight", ColumnType.DOUBLE));

		return columnTypes;
	}

	private static Map<String, ColumnType> getEmissionsColumnTypes() {
		Map<String, ColumnType> columnTypes = new HashMap<>(Map.of("Pollutant", ColumnType.STRING, "kg", ColumnType.DOUBLE));

		return columnTypes;
	}

	private static void joinCsvData(File baseFile, File policyFile, String outputFileName) {
		Table baseData = null;
		try {
			baseData = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(baseFile.toURL())).sample(false).separator(CsvOptions.detectDelimiter(baseFile.getPath())));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Table policyData = null;
		try {
			policyData = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(policyFile.toURL())).sample(false).separator(CsvOptions.detectDelimiter(policyFile.getPath())));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		String columnName;
		Map<String, Column> comparisonAllModes = new LinkedHashMap<>();

		StringColumn rowLabels = (StringColumn) policyData.column(0);

//			This is an assumption that all columns after the first one are modes of transportation.
//			Should eventually be changed to avoid errors.
		for (int i = 1; i < policyData.columnCount(); i++) {
			columnName = policyData.column(i).name();
			if (!baseData.columnNames().contains(columnName)) {
				log.warn("Column {} not found in base case trip stats, skipping", columnName);
			} else {
				DoubleColumn baseColumn = (DoubleColumn) baseData.column(columnName);
				DoubleColumn policyColumn = (DoubleColumn) policyData.column(columnName);

				comparisonAllModes.put(columnName, policyColumn);
				comparisonAllModes.put(columnName + "_base", baseColumn);
			}
		}
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath(outputFileName + ".csv")), CSVFormat.DEFAULT)) {

			List<String> modes = new ArrayList<>(comparisonAllModes.keySet());

			int rowCount = comparisonAllModes.get(modes.get(0)).size();

			printer.print("Info");
			for (String mode : comparisonAllModes.keySet()) {
				printer.print(mode);
			}
			printer.println();

			for (int i = 0; i < Math.min(rowLabels.size(), baseData.column(0).size()); i++) {
				printer.print(rowLabels.get(i));
				for (String mode : comparisonAllModes.keySet()) {
					printer.print(comparisonAllModes.get(mode).get(i));
				}
				printer.println();
			}

		} catch (IOException ex) {
			log.error(ex);
		}
	}

	@Override
	public Integer call() throws Exception {

		File baseTripsDir = new File(constructorBasePath + "/analysis/population");
		File baseTripsFile = new File(baseTripsDir + "/trip_stats.csv");
		File basePopulationTripsFile = new File(baseTripsDir + "/population_trip_stats.csv");

		log.info("Base trips file exists: {}", baseTripsFile.exists());
		log.info("Base trips file path: {}", baseTripsFile.getAbsoluteFile());

		File policyTripsFile = new File(input.getPath(TripAnalysis.class, "trip_stats.csv")).getAbsoluteFile();
		File policyPopulationTripsFile = new File(input.getPath(TripAnalysis.class, "population_trip_stats.csv")).getAbsoluteFile();

		log.info("Policy trips file exists: {}", policyTripsFile.exists());
		log.info("Policy trips absolute path: {}", policyTripsFile.getAbsolutePath());

		if (!policyTripsFile.exists() || !baseTripsFile.exists()) {
			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("trip_stats_comparison.csv").toString()), CSVFormat.DEFAULT)) {
				printer.printRecord("No trips files were found", 0, "user-group");
				log.warn("At least one trips file was not found. Please check log info to see which files are missing.");
			} catch (IOException ex) {
				log.error(ex);
			}
		} else {
			joinCsvData(baseTripsFile, policyTripsFile, "trip_stats_comparison");
		}

		if (!policyPopulationTripsFile.exists() || !basePopulationTripsFile.exists()) {
			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("population_trip_stats_comparison.csv").toString()), CSVFormat.DEFAULT)) {
				printer.printRecord("No population trips files were found", 0, "user-group");
				log.warn("At least one trips file was not found. Please check log info to see which files are missing.");
			} catch (IOException ex) {
				log.error(ex);
			}
		} else {
			joinCsvData(basePopulationTripsFile, policyPopulationTripsFile, "population_trip_stats_comparison");
		}

		return 0;
	}
}




























//		if (!policyEmissionsFile .exists() || !baseEmissionsFile.exists()) {
//			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("difference_emissions.csv").toString()), CSVFormat.DEFAULT)) {
//				printer.printRecord("No emissions files were found", 0);
//				log.warn("At least one emissions file was not found. Please check log info to see which files are missing.");
//			} catch (IOException ex) {
//				log.error(ex);
//			}
//		} else {
//			Table baseEmissionsData = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(baseEmissionsFile.toURL()))
//				.columnTypesPartial(getEmissionsColumnTypes())
//				.sample(false)
//				.separator(CsvOptions.detectDelimiter(String.valueOf(baseEmissionsFile))).build());
//
//			Table policyEmissionsData = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(policyEmissionsFile.toURL()))
//				.columnTypesPartial(getEmissionsColumnTypes())
//				.sample(false)
//				.separator(CsvOptions.detectDelimiter(String.valueOf(policyEmissionsFile.toURL()))).build());
//
/// /			This is an assumption that the emissions_total.csv has emissions categories in the first column and values in the next one.
/// /			It is then transposed to be able to utilize useful tablesaw methods that work best with columns.
/// /			Should eventually be changed to avoid errors if emissions_total.csv format changes.
//			Table baseEmissionsDataT = Table.create("TransposedBase");
//			StringColumn baseCategories = baseEmissionsData.stringColumn(0);
//
//			for (int i = 0; i < baseEmissionsData.rowCount(); i++) {
//				baseEmissionsDataT.addColumns(DoubleColumn.create(baseCategories.get(i)));
//			}
//
//			for (int colIdx = 1; colIdx < baseEmissionsData.columnCount(); colIdx++) {
//				Row newRow = baseEmissionsDataT.appendRow();
//				DoubleColumn numericCol = baseEmissionsData.doubleColumn(colIdx);
//
//				for (int rowIdx = 0; rowIdx < baseEmissionsData.rowCount(); rowIdx++) {
//					newRow.setDouble(baseCategories.get(rowIdx), numericCol.get(rowIdx));
//				}
//			}
//
//			Table policyEmissionsDataT = Table.create("TransposedBase");
//			StringColumn policyCategories = policyEmissionsData.stringColumn(0);
//
//			for (int i = 0; i < policyEmissionsData.rowCount(); i++) {
//				policyEmissionsDataT.addColumns(DoubleColumn.create(policyCategories.get(i)));
//			}
//
//			for (int colIdx = 1; colIdx < policyEmissionsData.columnCount(); colIdx++) {
//				Row newRow = policyEmissionsDataT.appendRow();
//				DoubleColumn numericCol = policyEmissionsData.doubleColumn(colIdx);
//
//				for (int rowIdx = 0; rowIdx < policyEmissionsData.rowCount(); rowIdx++) {
//					newRow.setDouble(policyCategories.get(rowIdx), numericCol.get(rowIdx));
//				}
//			}
//
//			String columnName;
//			Map<String, Double> emissionsComparisonAllTypes = new HashMap<>();
//
//
//			for (int i = 1; i < policyEmissionsDataT.columnCount(); i++) {
//
//				double baseTotalEmissionsOfType;
//				double policyTotalEmissionsOfType;
//				columnName = policyEmissionsDataT.column(i).name();
//				if (!baseEmissionsDataT.columnNames().contains(columnName)) {
//					log.warn("Column {} not found in base case trip stats, skipping", columnName);
//				} else {
//					DoubleColumn baseColumn = (DoubleColumn) baseEmissionsDataT.column(columnName);
//					DoubleColumn policyColumn = (DoubleColumn) policyEmissionsDataT.column(columnName);
//					baseTotalEmissionsOfType = baseColumn.get(0);
//					policyTotalEmissionsOfType = policyColumn.get(0);
//
//					emissionsComparisonAllTypes.put(columnName + "", policyTotalEmissionsOfType - baseTotalEmissionsOfType);
//				}
//			}
//			try (CSVPrinter printer = new CSVPrinter(
//				IOUtils.getBufferedWriter(output.getPath("difference_emissions.csv").toString()),
//				CSVFormat.DEFAULT)
//			) {
//				for (Map.Entry<String, Double> entry : emissionsComparisonAllTypes.entrySet()) {
//					String columnNameKey = entry.getKey();
//					double value = Math.round(entry.getValue()); // round if needed
//					printer.printRecord(columnNameKey, value);
//				}
//			} catch (IOException ex) {
//				log.error(ex);
//			}
//		}
