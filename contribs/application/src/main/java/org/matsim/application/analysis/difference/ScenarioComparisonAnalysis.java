package org.matsim.application.analysis.difference;


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
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


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
	@CommandLine.Option(names = "--input-policy-path", description = "File with reference data", required = true)
	private String constructorPolicyPath;

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

		File policyEmissionsFile = new File(input.getRunDirectory() + "/analysis/population/");
		File[] matchingEmissionsPolicyFiles = policyEmissionsFile.listFiles((dir, name) -> name.matches("emissions_total.csv"));

		File baseEmissionsFile = new File(constructorBasePath + "/analysis/emissione/");
		File[] matchingEmissionsBaseFiles = baseEmissionsFile.listFiles((dir, name) -> name.matches("emissions_total.csv"));


		if (matchingTripsPolicyFiles  == null || matchingTripsBaseFiles == null) {
			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("difference_trips.csv").toString()), CSVFormat.DEFAULT)) {
				printer.printRecord("No trips files were found", 0, "user-group");
			} catch (IOException ex) {
				log.error(ex);
			}
		} else {
			Table baseTripsData = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(matchingTripsBaseFiles [0].toURL()))
//				.columnTypesPartial(getColumnTypes())
				.sample(false)
				.separator(CsvOptions.detectDelimiter(String.valueOf(matchingTripsBaseFiles [0]))).build());

			Table policyTripsData = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(matchingTripsPolicyFiles[0].toURL()))
//				.columnTypesPartial(getColumnTypes())
				.sample(false)
				.separator(CsvOptions.detectDelimiter(String.valueOf(matchingTripsPolicyFiles[0].toURL()))).build());

			double baseCarKm = 0.0, baseBikeKm = 0.0, baseRideKm = 0.0, baseWalkKm = 0.0, basePtKm = 0.0, baseFreightKm = 0.0;
			double policyCarKm = 0.0, policyBikeKm = 0.0, policyRideKm = 0.0, policyWalkKm = 0.0, policyPtKm = 0.0, policyFreightKm = 0.0;
			double baseCarHours = 0.0, baseBikeHours = 0.0, baseRideHours = 0.0, baseWalkHours = 0.0, basePtHours = 0.0, baseFreightHours = 0.0;
			double policyCarHours = 0.0, policyBikeHours = 0.0, policyRideHours = 0.0, policyWalkHours = 0.0, policyPtHours = 0.0, policyFreightHours = 0.0;

			String columnName;
			for  (int i = 1; i < policyTripsData.columnCount() - 1; i++){
				int baseTotalTimeTraveled = 0;
				int baseTotalDistanceTraveled = 0;
				columnName = policyTripsData.column(i).name();
				if ( ! baseTripsData.columnNames().contains(columnName) ) {
					log.warn("Column {} not found in base case trip stats, skipping", columnName);
				} else {
//					baseTotalTimeTraveled = baseTripsData.column(baseTripsData.columnIndex(columnName))
				}
			}
			for (int i = 0; i < baseTripsData.rowCount(); i++) {
				Row row = baseTripsData.row(i);
//				for (int i = 0; i < row.columnCount(); i++) {
//					columnName = row.columnNames().get(i);
//				}
				if (Objects.equals(row.getString("Info"), "Total distance traveled [km]")) {
					baseCarKm += row.getDouble("car");
					baseBikeKm += row.getDouble("bike");
					baseRideKm += row.getDouble("ride");
					baseWalkKm += row.getDouble("walk");
					basePtKm += row.getDouble("pt");
					baseFreightKm += row.getDouble("freight");
				}
				if (Objects.equals(row.getString("Info"), "Total time traveled [h]")) {
					baseCarHours += row.getDouble("car");
					baseBikeHours += row.getDouble("bike");
					baseRideHours += row.getDouble("ride");
					baseWalkHours += row.getDouble("walk");
					basePtHours += row.getDouble("pt");
					baseFreightHours += row.getDouble("freight");
				}}

			for (int i = 0; i < policyTripsData.rowCount(); i++) {
				Row row = policyTripsData.row(i);
				if (Objects.equals(row.getString("Info"), "Total distance traveled [km]")) {
					policyCarKm += row.getDouble("car");
					policyBikeKm += row.getDouble("bike");
					policyRideKm += row.getDouble("ride");
					policyWalkKm += row.getDouble("walk");
					policyPtKm += row.getDouble("pt");
					policyFreightKm += row.getDouble("freight");
				}
				if (Objects.equals(row.getString("Info"), "Total time traveled [h]")) {
					policyCarHours += row.getDouble("car");
					policyBikeHours += row.getDouble("bike");
					policyRideHours += row.getDouble("ride");
					policyWalkHours += row.getDouble("walk");
					policyPtHours += row.getDouble("pt");
					policyFreightHours += row.getDouble("freight");
				}}

			double carKmsDiffValue = policyCarKm - baseCarKm;
			double bikeKmsDiffValue = policyBikeKm - baseBikeKm;
			double rideKmsDiffValue = policyRideKm - baseRideKm;
			double walkKmsDiffValue = policyWalkKm - baseWalkKm;
			double ptKmsDiffValue = policyPtKm - basePtKm;
			double freightKmsDiffValue = policyFreightKm - baseFreightKm;

			double carHoursDiffValue = policyCarHours - baseCarHours;
			double bikeHoursDiffValue = policyBikeHours - baseBikeHours;
			double rideHoursDiffValue = policyRideHours - baseRideHours;
			double walkHoursDiffValue = policyWalkHours - baseWalkHours;
			double ptHoursDiffValue = policyPtHours - basePtHours;
			double freightHoursDiffValue = policyFreightHours - baseFreightHours;
			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("difference_trips.csv").toString()), CSVFormat.DEFAULT)) {
				printer.printRecord("Car Kilometer difference", Math.round(carKmsDiffValue), "user-group");
				printer.printRecord("Car Hour difference", Math.round(carHoursDiffValue), "person-circle-xmark");
				printer.printRecord("Bike Kilometer difference", Math.round(bikeKmsDiffValue), "user-group");
				printer.printRecord("Bike Hour difference", Math.round(bikeHoursDiffValue), "person-circle-xmark");
				printer.printRecord("Ride Kilometer difference", Math.round(rideKmsDiffValue), "user-group");
				printer.printRecord("Ride Hour difference", Math.round(rideHoursDiffValue), "person-circle-xmark");
				printer.printRecord("Walk Kilometer difference", Math.round(walkKmsDiffValue), "user-group");
				printer.printRecord("Walk difference", Math.round(walkHoursDiffValue), "person-circle-xmark");
				printer.printRecord("Pt Kilometer difference", Math.round(ptKmsDiffValue), "user-group");
				printer.printRecord("Pt Hour difference", Math.round(ptHoursDiffValue), "person-circle-xmark");
				printer.printRecord("Freight Kilometer difference", Math.round(freightKmsDiffValue), "user-group");
				printer.printRecord("Freight Hour difference", Math.round(freightHoursDiffValue), "person-circle-xmark");
			} catch (IOException ex) {
				log.error(ex);
			}
		}


		if (matchingEmissionsPolicyFiles == null || matchingEmissionsBaseFiles == null) {
			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("difference_emissions.csv").toString()), CSVFormat.DEFAULT)) {
				printer.printRecord("No emissions files were found", 0, "user-group");
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

			double baseCO = 0.0, baseCO2 = 0.0, baseHC = 0.0, baseNOx = 0.0, basePM = 0.0, baseSO2 = 0.0;
			double policyCO = 0.0, policyCO2 = 0.0, policyHC = 0.0, policyNOx = 0.0, policyPM = 0.0, policySO2 = 0.0;


			for  (int i = 0; i < policyEmissionsData.rowCount(); i++) {
				Row row = policyEmissionsData.row(i);
				if (Objects.equals(row.getString("Pollutant"), "CO")) {
					policyCO += row.getDouble("kg");
				}
				if (Objects.equals(row.getString("Pollutant"), "CO2")) {
					policyCO2 += row.getDouble("kg");
				}
				if (Objects.equals(row.getString("Pollutant"), "HC")) {
					policyHC += row.getDouble("kg");
				}
				if (Objects.equals(row.getString("Pollutant"), "NOx")) {
					policyNOx += row.getDouble("kg");
				}
				if (Objects.equals(row.getString("Pollutant"), "PM")) {
					policyPM += row.getDouble("kg");
				}
				if (Objects.equals(row.getString("Pollutant"), "SO2")) {
					policySO2 += row.getDouble("kg");
				}
			}

			for  (int i = 0; i < baseEmissionsData.rowCount(); i++) {
				Row row = baseEmissionsData.row(i);
				if (Objects.equals(row.getString("Pollutant"), "CO")) {
					baseCO += row.getDouble("kg");
				}
				if (Objects.equals(row.getString("Pollutant"), "CO2")) {
					baseCO2 += row.getDouble("kg");
				}
				if (Objects.equals(row.getString("Pollutant"), "HC")) {
					baseHC += row.getDouble("kg");
				}
				if (Objects.equals(row.getString("Pollutant"), "NOx")) {
					baseNOx += row.getDouble("kg");
				}
				if (Objects.equals(row.getString("Pollutant"), "PM")) {
					basePM += row.getDouble("kg");
				}
				if (Objects.equals(row.getString("Pollutant"), "SO2")) {
					baseSO2 += row.getDouble("kg");
				}
			}

			double CO2DiffValue = policyCO2 - baseCO2;
			double CODiffValue = policyCO - baseCO;
			double NOxDiffValue = policyNOx - baseNOx;
			double PMDiffValue = policyPM - basePM;
			double SO2DiffValue = policySO2 - baseSO2;
			double HCDiffValue = policyHC - baseHC;

			try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("difference_emissions.csv").toString()), CSVFormat.DEFAULT)) {
				printer.printRecord("Carbon monoxide emissions (CO) difference", Math.round(CODiffValue), "user-group");
				printer.printRecord("Carbon dioxide emissions (CO2) difference", Math.round(CO2DiffValue), "person-circle-xmark");
				printer.printRecord("Hydrocarbon emissions (HC) difference", Math.round(HCDiffValue), "user-group");
				printer.printRecord("Nitrogen oxide emissions (NOx) difference", Math.round(NOxDiffValue), "person-circle-xmark");
				printer.printRecord("Particulate matter emissions (PM) difference", Math.round(PMDiffValue), "user-group");
				printer.printRecord("Sulfur dioxide emissions (SO2) difference", Math.round(SO2DiffValue), "person-circle-xmark");
			} catch (IOException ex) {
				log.error(ex);
			}
		}

		return 0;
	}
}
