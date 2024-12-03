package org.matsim.application.analysis.population;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

class TripAnalysisTest {
	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();
	private final CsvOptions csv = new CsvOptions(CSVFormat.Predefined.Default);

	@Test
	void defaultParametersTest() throws IOException {

		writeInputCsvFiles();

		new TripAnalysis().execute("--input-trips", Path.of(utils.getInputDirectory(), "trips.csv").toString(),
			"--input-persons", Path.of(utils.getInputDirectory(), "persons.csv").toString(),
			"--output-mode-share", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_share.csv").toString(),
			"--output-mode-share-per-dist", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_share_per_dist.csv").toString(),
			"--output-mode-users", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_users.csv").toString(),
			"--output-trip-stats", Path.of(utils.getOutputDirectory(), "analysis", "population", "trip_stats.csv").toString(),
			"--output-population-trip-stats", Path.of(utils.getOutputDirectory(), "analysis", "population", "population_trip_stats.csv").toString(),
			"--output-trip-purposes-by-hour", Path.of(utils.getOutputDirectory(), "analysis", "population", "trip_purposes_by_hour.csv").toString(),
			"--output-mode-share-distance-distribution", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_share_distance_distribution.csv").toString(),
			"--output-mode-choices", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_choices.csv").toString(),
			"--output-mode-choice-evaluation", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_choice_evaluation.csv").toString(),
			"--output-mode-choice-evaluation-per-mode", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_choice_evaluation_per_mode.csv").toString(),
			"--output-mode-confusion-matrix", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_confusion_matrix.csv").toString(),
			"--output-mode-prediction-error", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_prediction_error.csv").toString());

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**trip_stats.csv")
			.isDirectoryContaining("glob:**mode_share.csv")
			.isDirectoryContaining("glob:**mode_share_per_dist.csv")
			.isDirectoryContaining("glob:**mode_users.csv")
			.isDirectoryContaining("glob:**population_trip_stats.csv")
			.isDirectoryContaining("glob:**trip_purposes_by_hour.csv")
			.isDirectoryContaining("glob:**mode_share_distance_distribution.csv");

		Path.of(utils.getInputDirectory()).toFile().delete();
	}

	@Test
	void personFilterTest() throws IOException {

		writeInputCsvFiles();

		new TripAnalysis().execute("--person-filter", "subpopulation=person",
			"--input-trips", Path.of(utils.getInputDirectory(), "trips.csv").toString(),
			"--input-persons", Path.of(utils.getInputDirectory(), "persons.csv").toString(),
			"--output-mode-share", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_share.csv").toString(),
			"--output-mode-share-per-dist", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_share_per_dist.csv").toString(),
			"--output-mode-users", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_users.csv").toString(),
			"--output-trip-stats", Path.of(utils.getOutputDirectory(), "analysis", "population", "trip_stats.csv").toString(),
			"--output-population-trip-stats", Path.of(utils.getOutputDirectory(), "analysis", "population", "population_trip_stats.csv").toString(),
			"--output-trip-purposes-by-hour", Path.of(utils.getOutputDirectory(), "analysis", "population", "trip_purposes_by_hour.csv").toString(),
			"--output-mode-share-distance-distribution", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_share_distance_distribution.csv").toString(),
			"--output-mode-choices", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_choices.csv").toString(),
			"--output-mode-choice-evaluation", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_choice_evaluation.csv").toString(),
			"--output-mode-choice-evaluation-per-mode", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_choice_evaluation_per_mode.csv").toString(),
			"--output-mode-confusion-matrix", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_confusion_matrix.csv").toString(),
			"--output-mode-prediction-error", Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_prediction_error.csv").toString());

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**trip_stats.csv")
			.isDirectoryContaining("glob:**mode_share.csv")
			.isDirectoryContaining("glob:**mode_share_per_dist.csv")
			.isDirectoryContaining("glob:**mode_users.csv")
			.isDirectoryContaining("glob:**population_trip_stats.csv")
			.isDirectoryContaining("glob:**trip_purposes_by_hour.csv")
			.isDirectoryContaining("glob:**mode_share_distance_distribution.csv");

		Table modeShare = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_share.csv").toString()))
			.columnTypesPartial(Map.of("person", ColumnType.TEXT))
			.sample(false)
			.separator(CsvOptions.detectDelimiter(Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_share.csv").toString())).build());

		StringColumn mainMode = modeShare.stringColumn("main_mode");

//		only 1 row with values
		Assertions.assertThat(modeShare.rowCount()).isEqualTo(1);
		//		only mode car, no mode goods in mode share stats
		Assertions.assertThat(mainMode.get(0)).isEqualTo("car");

		Path.of(utils.getInputDirectory()).toFile().delete();
	}

	private void writeInputCsvFiles() throws IOException {
		Path persons = Path.of(utils.getInputDirectory()).resolve("persons.csv");
		Files.createDirectories(persons.getParent());
		CSVPrinter printer = csv.createPrinter(persons);

//		print dummy persons
		printer.printRecord("person", "executed_score", "first_act_x", "first_act_y", "first_act_type", "age", "carAvail", "home_x", "home_y", "householdIncome", "householdSize",
			"income", "sex", "sim_ptAbo", "sim_regionType", "subpopulation", "purpose", "tourStartArea", "vehicleTypes");
		printer.printRecord("100", "-130.69951448065348", "369956.19", "5776578.61", "home_49200", "50", "always", "369956.19", "5776578.61", "5", "2", "1159.0", "f", "none", "124", "person", "", "", "");
		printer.printRecord("100_goodsTraffic", "-130.69951448065348", "369956.19", "5776578.61", "home_49200", "50", "always", "369956.19", "5776578.61", "5", "2", "1159.0", "f", "none", "124", "goodsTraffic", "", "", "");
		printer.close();

//		print dummy trips
		printer = csv.createPrinter(Path.of(utils.getInputDirectory(), "trips.csv"));
		printer.printRecord("person", "trip_number", "trip_id", "dep_time", "trav_time", "wait_time", "traveled_distance", "euclidean_distance", "main_mode", "longest_distance_mode",
			"modes", "start_activity_type", "end_activity_type", "start_facility_id", "start_link", "start_x", "start_y", "end_facility_id", "end_link", "end_x", "end_y", "first_pt_boarding_stop", "last_pt_egress_stop");
		printer.printRecord("100", "1", "100_1", "13:57:42", "00:34:07", "00:00:00", "53025", "34976", "car", "car", "walk-car-walk", "home_49200", "errands_3600", "null", "-199781090",
			"369956.19", "5776578.61", "null", "152273276", "401592.51", "5761661.71", "", "");
		printer.printRecord("100", "2", "100_2", "15:42:50", "01:01:52", "00:00:00", "78917", "54643", "car", "car", "walk-car-walk", "errands_3600", "errands_4200", "null", "152273276",
			"401592.51", "5761661.71", "null", "-366338372", "455444.34", "5752394.08", "", "");
		printer.printRecord("100_goodsTraffic", "1", "100_1", "13:57:42", "00:34:07", "00:00:00", "53025", "34976", "goods", "goods", "walk-goods-walk", "home_49200", "errands_3600", "null", "-199781090",
			"369956.19", "5776578.61", "null", "152273276", "401592.51", "5761661.71", "", "");
		printer.close();
	}
}
