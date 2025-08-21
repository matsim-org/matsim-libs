package org.matsim.application.analysis.scenarioComparison;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.population.StuckAgentAnalysis;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@CommandLine.Command(name = "difference", description = "Calculates difference in amount of car hours traveled between base and policy case.")
@CommandSpec(requireRunDirectory = true,
	produces = {"difference_trips.csv", "difference_emissions.csv"}
)
public class ScenarioComparisonAnalysis implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(StuckAgentAnalysis.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(ScenarioComparisonAnalysis.class);

	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(ScenarioComparisonAnalysis.class);


	@CommandLine.Option(names = "--input-base-path", description = "File with reference data", required = true)
	private String constructorBasePath;
//	@CommandLine.Option(names = "--input-policy-path", description = "File with reference data", required = true)
//	private String constructorPolicyPath;

	public static void main(String[] args) {
		new ScenarioComparisonAnalysis().execute(args);
	}

	private static Map<String, ColumnType> getColumnTypes() {
		Map<String, ColumnType> columnTypes = new HashMap<>(Map.of("Info", ColumnType.STRING,
			"bike", ColumnType.DOUBLE, "car", ColumnType.DOUBLE, "ride", ColumnType.DOUBLE,
			"walk", ColumnType.DOUBLE, "pt", ColumnType.DOUBLE,
			"freight", ColumnType.DOUBLE));

		return columnTypes;
	}

	private static Map<String, ColumnType> getEmissionsColumnTypes() {
		Map<String, ColumnType> columnTypes = new HashMap<>(Map.of("Pollutant", ColumnType.STRING, "kg", ColumnType.DOUBLE));

		return columnTypes;
	}
	@Override
	public Integer call() throws Exception {
		File policyTripsFile = new File(input.getRunDirectory() + "/analysis/population/");
		File[] matchingTripsPolicyFiles = policyTripsFile.listFiles((dir, name) -> name.matches("trip_stats.csv"));

		File baseTripsFile = new File(constructorBasePath + "/analysis/population/");
		File[] matchingTripsBaseFiles = baseTripsFile.listFiles((dir, name) -> name.matches("trip_stats.csv"));

		File policyEmissionsFile = new File(input.getRunDirectory() + "/analysis/emissions/");
		File[] matchingEmissionsPolicyFiles = policyEmissionsFile.listFiles((dir, name) -> name.matches("emissions_total.csv"));

		File baseEmissionsFile = new File(constructorBasePath + "/analysis/emissions/");
		File[] matchingEmissionsBaseFiles = baseEmissionsFile.listFiles((dir, name) -> name.matches("emissions_total.csv"));

		if (matchingTripsPolicyFiles  == null || matchingTripsBaseFiles == null) {
			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("difference_trips.csv").toString()), CSVFormat.DEFAULT)) {
				printer.printRecord("No trips files were found", 0, "user-group");
				log.warn("At least one trips file was not found");
			} catch (IOException ex) {
				log.error(ex);
			}
		} else {
			Table baseTripsData = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(matchingTripsBaseFiles [0].toURL()))
				.sample(false)
				.separator(CsvOptions.detectDelimiter(String.valueOf(matchingTripsBaseFiles [0]))).build());

			Table policyTripsData = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(matchingTripsPolicyFiles[0].toURL()))
				.sample(false)
				.separator(CsvOptions.detectDelimiter(String.valueOf(matchingTripsPolicyFiles[0].toURL()))).build());

			String columnName;
			Map<String, Double> tripsComparisonAllModes = new HashMap<>();

//			This is an assumption that all columns after the first one are modes of transportation.
//			Should eventually be changed to avoid errors.
			for  (int i = 1; i < policyTripsData.columnCount(); i++){
				double baseTotalTimeTraveled;
				double baseTotalDistanceTraveled;
				double policyTotalTimeTraveled;
				double policyTotalDistanceTraveled;
				columnName = policyTripsData.column(i).name();
				if ( ! baseTripsData.columnNames().contains(columnName) ) {
					log.warn("Column {} not found in base case trip stats, skipping", columnName);
				} else {
					DoubleColumn baseColumn = (DoubleColumn) baseTripsData.column(columnName);
					DoubleColumn policyColumn = (DoubleColumn) policyTripsData.column(columnName);
					baseTotalTimeTraveled = baseColumn.get(1);
					baseTotalDistanceTraveled = baseColumn.get(2);
					policyTotalTimeTraveled = policyColumn.get(1);
					policyTotalDistanceTraveled = policyColumn.get(2);

					tripsComparisonAllModes.put(columnName + "_dist", policyTotalDistanceTraveled - baseTotalDistanceTraveled);
					tripsComparisonAllModes.put(columnName + "_time", policyTotalTimeTraveled - baseTotalTimeTraveled);
				}
			}

			try (CSVPrinter printer = new CSVPrinter(
				IOUtils.getBufferedWriter(output.getPath("difference_trips.csv").toString()),
				CSVFormat.DEFAULT)
			) {
				for (Map.Entry<String, Double> entry : tripsComparisonAllModes.entrySet()) {
					String columnNameKey = entry.getKey();
					double value = Math.round(entry.getValue()); // round if needed
					printer.printRecord(columnNameKey, value);
				}
			} catch (IOException ex) {
				log.error(ex);
			}
		}


		if (matchingEmissionsPolicyFiles == null || matchingEmissionsBaseFiles == null) {
			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("difference_emissions.csv").toString()), CSVFormat.DEFAULT)) {
				printer.printRecord("No emissions files were found", 0);
				log.warn("At least one emissions file was not found");
			} catch (IOException ex) {
				log.error(ex);
			}
		} else {
			Table baseEmissionsData = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(matchingEmissionsBaseFiles[0].toURL()))
				.columnTypesPartial(getEmissionsColumnTypes())
				.sample(false)
				.separator(CsvOptions.detectDelimiter(String.valueOf(matchingEmissionsBaseFiles[0]))).build());

			Table policyEmissionsData = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(matchingEmissionsPolicyFiles[0].toURL()))
				.columnTypesPartial(getEmissionsColumnTypes())
				.sample(false)
				.separator(CsvOptions.detectDelimiter(String.valueOf(matchingEmissionsPolicyFiles[0].toURL()))).build());

//			This is an assumption that the emissions_total.csv has emissions categories in the first column and values in the next one.
//			It is then transposed to be able to utilize useful tablesaw methods that work best with columns.
//			Should eventually be changed to avoid errors if emissions_total.csv format changes.
			Table baseEmissionsDataT = Table.create("TransposedBase");
			StringColumn baseCategories = baseEmissionsData.stringColumn(0);

			for (int i = 0; i < baseEmissionsData.rowCount(); i++) {
				baseEmissionsDataT.addColumns(DoubleColumn.create(baseCategories.get(i)));
			}

			for (int colIdx = 1; colIdx < baseEmissionsData.columnCount(); colIdx++) {
				Row newRow = baseEmissionsDataT.appendRow();
				DoubleColumn numericCol = baseEmissionsData.doubleColumn(colIdx);

				for (int rowIdx = 0; rowIdx < baseEmissionsData.rowCount(); rowIdx++) {
					newRow.setDouble(baseCategories.get(rowIdx), numericCol.get(rowIdx));
				}
			}

			Table policyEmissionsDataT = Table.create("TransposedBase");
			StringColumn policyCategories = policyEmissionsData.stringColumn(0);

			for (int i = 0; i < policyEmissionsData.rowCount(); i++) {
				policyEmissionsDataT.addColumns(DoubleColumn.create(policyCategories.get(i)));
			}

			for (int colIdx = 1; colIdx < policyEmissionsData.columnCount(); colIdx++) {
				Row newRow = policyEmissionsDataT.appendRow();
				DoubleColumn numericCol = policyEmissionsData.doubleColumn(colIdx);

				for (int rowIdx = 0; rowIdx < policyEmissionsData.rowCount(); rowIdx++) {
					newRow.setDouble(policyCategories.get(rowIdx), numericCol.get(rowIdx));
				}
			}

			String columnName;
			Map<String, Double> emissionsComparisonAllTypes = new HashMap<>();


			for (int i = 1; i < policyEmissionsDataT.columnCount(); i++) {

				double baseTotalEmissionsOfType;
				double policyTotalEmissionsOfType;
				columnName = policyEmissionsDataT.column(i).name();
				if (!baseEmissionsDataT.columnNames().contains(columnName)) {
					log.warn("Column {} not found in base case trip stats, skipping", columnName);
				} else {
					DoubleColumn baseColumn = (DoubleColumn) baseEmissionsDataT.column(columnName);
					DoubleColumn policyColumn = (DoubleColumn) policyEmissionsDataT.column(columnName);
					baseTotalEmissionsOfType = baseColumn.get(0);
					policyTotalEmissionsOfType = policyColumn.get(0);

					emissionsComparisonAllTypes.put(columnName + "", policyTotalEmissionsOfType - baseTotalEmissionsOfType);
				}

				try (CSVPrinter printer = new CSVPrinter(
					IOUtils.getBufferedWriter(output.getPath("difference_emissions.csv").toString()),
					CSVFormat.DEFAULT)
				) {
					for (Map.Entry<String, Double> entry : emissionsComparisonAllTypes.entrySet()) {
						String columnNameKey = entry.getKey();
						double value = Math.round(entry.getValue()); // round if needed
						printer.printRecord(columnNameKey, value);
					}
				} catch (IOException ex) {
					log.error(ex);
				}

			}
		}

		return 0;
	}
}
