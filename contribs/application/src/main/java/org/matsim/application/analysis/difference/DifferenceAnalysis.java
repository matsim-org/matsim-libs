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
import org.matsim.core.utils.misc.Time;
import picocli.CommandLine;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


@CommandLine.Command(name = "difference", description = "Calculates difference in amount of car hours traveled between base and policy case.")
@CommandSpec(
//	requires = {"trips.csv"},
	produces = {"difference.csv"}
)


public class DifferenceAnalysis implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(StuckAgentAnalysis.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(DifferenceAnalysis.class);

	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(DifferenceAnalysis.class);


	@CommandLine.Option(names = "--input-base-path", description = "File with reference data", required = true)
	private String constructorBasePath;
	@CommandLine.Option(names = "--input-policy-path", description = "File with reference data", required = true)
	private String constructorPolicyPath;

	public static void main(String[] args) {
		new DifferenceAnalysis().execute(args);
	}

	private static Map<String, ColumnType> getColumnTypes() {
		Map<String, ColumnType> columnTypes = new HashMap<>(Map.of("person", ColumnType.STRING,
			"trav_time", ColumnType.STRING, "wait_time", ColumnType.STRING, "dep_time", ColumnType.STRING,
			"longest_distance_mode", ColumnType.STRING, "main_mode", ColumnType.STRING,
			"start_activity_type", ColumnType.STRING, "end_activity_type", ColumnType.STRING,
			"first_pt_boarding_stop", ColumnType.STRING, "last_pt_egress_stop", ColumnType.STRING));

		// Map.of only has 10 argument max
		columnTypes.put("traveled_distance", ColumnType.LONG);
		columnTypes.put("euclidean_distance", ColumnType.LONG);

		return columnTypes;
	}
	@Override
	public Integer call() throws Exception {
		File test = new File(constructorPolicyPath);
		File[] matchingPolicyFiles = test.listFiles((dir, name) -> name.contains("trips.csv"));

		File testBase = new File(constructorBasePath);
		File[] matchingBaseFiles = testBase.listFiles((dir, name) -> name.contains("trips.csv"));


		Table baseTripsData = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(matchingBaseFiles[0].toURL()))
			.columnTypesPartial(getColumnTypes())
			.sample(false)
			.separator(CsvOptions.detectDelimiter(String.valueOf(matchingBaseFiles[0]))).build());

		Table policyTripsData = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(matchingPolicyFiles[0].toURL()))
			.columnTypesPartial(getColumnTypes())
			.sample(false)
			.separator(CsvOptions.detectDelimiter(String.valueOf(matchingPolicyFiles[0].toURL()))).build());

		double basePkwKm = 0.0;
		double policyPkwKm = 0.0;
		double basePkwHours = 0.0;
		double policyPkwHours = 0.0;

		for (int i = 0; i < baseTripsData.rowCount(); i++) {
			Row row = baseTripsData.row(i);
			if (Objects.equals(row.getString("main_mode"), "car")) {
				basePkwKm += row.getLong("traveled_distance") / 1000.0;
				LocalTime t = LocalTime.parse( row.getString("trav_time")) ;
				basePkwHours += t.toSecondOfDay() / 3600.0;
			}
		}

		for (int i = 0; i < policyTripsData.rowCount(); i++) {
			Row row = policyTripsData.row(i);
			if (Objects.equals(row.getString("main_mode"), "car")) {
				policyPkwKm += row.getLong("traveled_distance") / 1000.0;
				LocalTime t = LocalTime.parse( row.getString("trav_time")) ;
				policyPkwHours += t.toSecondOfDay() / 3600.0;
			}
		}

		double carKmsDiffValue = policyPkwKm - basePkwKm;
		double carHoursDiffValue = policyPkwHours - basePkwHours;

		DoubleColumn carHoursDiff = DoubleColumn.create("car_kms", carKmsDiffValue);
		DoubleColumn carKmsDiff = DoubleColumn.create("car_hours", carHoursDiffValue);

//		Table diffTable = Table.create();
//		diffTable.addColumns(carHoursDiff);
//		diffTable.addColumns(carKmsDiff);

		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("difference.csv").toString()), CSVFormat.DEFAULT)) {
			printer.printRecord("Kilometer difference", Math.round(carKmsDiffValue), "user-group");
			printer.printRecord("Hour difference", Math.round(carHoursDiffValue), "person-circle-xmark");
		} catch (IOException ex) {
			log.error(ex);
		}

//		diffTable.write().csv(output.getPath("difference.csv").toFile());


		return 0;
	}
}
