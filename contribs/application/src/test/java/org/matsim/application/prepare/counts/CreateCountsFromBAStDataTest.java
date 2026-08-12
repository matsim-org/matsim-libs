package org.matsim.application.prepare.counts;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.*;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateCountsFromBAStDataTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Aggregation the command uses if --aggregate is not given.
	 */
	String defaultAggregation = "midweek";
	String countsOutput = "counts-from-bast.xml.gz";

	String mapping = "mapping.csv";
	String wrongManualMatchedCounts = "wrong_manual.csv";

	String network = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("berlin"), "network.xml.gz").toString();
	String motorwayData = "2021_A_S_test_data.txt";
	String primaryData = "2021_B_S_test_data.txt.gz";
	String stationData = "Jawe2021_test_data.csv";
	String shp = "Bezirke-Berlin-shp.zip";

	String networkCrs = "EPSG:31468";
	String shpCrs = "EPSG:3857";

	/**
	 * Base arguments writing to {@code <output>/<version>}, plus whatever the test adds.
	 */
	private String[] args(String version, String... additional) {

		List<String> args = new ArrayList<>(List.of(
			"--station-data=" + utils.getPackageInputDirectory() + stationData,
			"--network=" + network,
			"--input-crs=" + networkCrs,
			"--motorway-data=" + utils.getPackageInputDirectory() + motorwayData,
			"--primary-data=" + utils.getPackageInputDirectory() + primaryData,
			"--shp=" + utils.getPackageInputDirectory() + shp,
			"--shp-crs=" + shpCrs,
			"--year=2021",
			"--output=" + output(version)
		));

		args.addAll(List.of(additional));

		return args.toArray(new String[0]);
	}

	private Path output(String version) {
		return Path.of(utils.getOutputDirectory(), version);
	}

	private Path countsFile(String version, String aggregation) {
		return output(version).resolve(aggregation).resolve(countsOutput);
	}

	private Counts<Link> read(Path path) {

		assertThat(path).exists();

		Counts<Link> counts = new Counts<>();
		new MatsimCountsReader(counts).readFile(path.toString());

		return counts;
	}

	@Test
	void testCreateCountsFromBAStData() {

		String version = "normal";

		new CreateCountsFromBAStData().execute(args(version));

		Counts<Link> counts = read(countsFile(version, defaultAggregation));

		assertThat(counts.getMeasureLocations())
			.isNotEmpty();

		for (Map.Entry<Id<Link>, MeasurementLocation<Link>> e : counts.getMeasureLocations().entrySet()) {
			assertThat(e.getValue().hasMeasurableForMode(Measurable.VOLUMES, TransportMode.car))
				.isTrue();

			assertThat(e.getValue().hasMeasurableForMode(Measurable.VOLUMES, TransportMode.truck))
				.isTrue();

			// BASt hour 01 covers 00:00 to 01:00, so a profile runs from hour 0 to hour 23
			Measurable volumes = e.getValue().getVolumesForMode(TransportMode.car);
			assertThat(volumes.getAtHour(0)).isPresent();
			assertThat(volumes.getAtHour(23)).isPresent();
			assertThat(volumes.size()).isEqualTo(24);
		}
	}

	@Test
	void testWithIgnoredStations() {

		new CreateCountsFromBAStData().execute(args("with-ignored"));
		new CreateCountsFromBAStData().execute(args("without-ignored",
			"--counts-mapping=" + utils.getPackageInputDirectory() + mapping));

		Counts<Link> countsComplete = read(countsFile("with-ignored", defaultAggregation));
		Counts<Link> countsWithoutIgnored = read(countsFile("without-ignored", defaultAggregation));

		int completeSize = countsComplete.getMeasureLocations().size();
		int ignoredSize = countsWithoutIgnored.getMeasureLocations().size();

		assertThat(completeSize).isGreaterThan(ignoredSize);
	}

	@Test
	void testManualMatchedCounts() {

		String version = "manual-matched";

		//Map contains supposed matching from manual.csv
		Map<Id<Link>, String> manual = Map.of(Id.createLinkId("4205"), "Neukölln_N, 2012_1_R1", Id.createLinkId("4219"), "Neukölln_S, 2012_2_R2");

		new CreateCountsFromBAStData().execute(args(version,
			"--counts-mapping=" + utils.getPackageInputDirectory() + mapping));

		var map = read(countsFile(version, defaultAggregation)).getMeasureLocations();

		for (var entry : manual.entrySet()) {

			Id<Link> supposed = entry.getKey();
			String station = entry.getValue();

			MeasurementLocation<Link> actual = map.get(supposed);
			String actualStation = actual.getStationName();

			Assertions.assertEquals(station, actualStation);
		}
	}

	@Test
	void testManualMatchingWithWrongInput() {

		String[] args = args("wrong-manual",
			"--counts-mapping=" + utils.getPackageInputDirectory() + wrongManualMatchedCounts);

		Assertions.assertThrows(RuntimeException.class, () -> new CreateCountsFromBAStData().execute(args));
	}

	@Test
	void testMultipleAggregations() {

		String version = "aggregations";

		new CreateCountsFromBAStData().execute(args(version,
			"--aggregate=midweek=TUESDAY,WEDNESDAY,THURSDAY",
			"--aggregate=sat=SATURDAY"));

		Counts<Link> midweek = read(countsFile(version, "midweek"));
		Counts<Link> saturday = read(countsFile(version, "sat"));

		assertThat(midweek.getMeasureLocations()).isNotEmpty();
		assertThat(saturday.getMeasureLocations()).hasSameSizeAs(midweek.getMeasureLocations());

		//The same days would produce the same values, different days must not
		assertThat(hourlyValues(midweek)).isNotEqualTo(hourlyValues(saturday));
	}

	@Test
	void testDateRanges() {

		String version = "ranges";

		new CreateCountsFromBAStData().execute(args(version,
			"--aggregate=january=TUESDAY,WEDNESDAY,THURSDAY@2021-01-01:2021-01-31",
			"--aggregate=whole-year=TUESDAY,WEDNESDAY,THURSDAY",
			//The test data covers 2021 only, so this range selects no day at all
			"--aggregate=empty=TUESDAY,WEDNESDAY,THURSDAY@2019-01-01:2019-12-31"));

		Counts<Link> january = read(countsFile(version, "january"));
		Counts<Link> wholeYear = read(countsFile(version, "whole-year"));

		assertThat(january.getMeasureLocations()).isNotEmpty();

		//A range restricting the days has to change the aggregated values
		assertThat(hourlyValues(january)).isNotEqualTo(hourlyValues(wholeYear));

		//An aggregation covering no day writes no file
		assertThat(countsFile(version, "empty")).doesNotExist();
	}

	@Test
	void testMedian() {

		String version = "median";

		new CreateCountsFromBAStData().execute(args(version, "--statistic=MEDIAN"));
		new CreateCountsFromBAStData().execute(args("mean", "--statistic=MEAN"));

		Counts<Link> median = read(countsFile(version, defaultAggregation));
		Counts<Link> mean = read(countsFile("mean", defaultAggregation));

		assertThat(median.getMeasureLocations()).hasSameSizeAs(mean.getMeasureLocations());
		assertThat(hourlyValues(median)).isNotEqualTo(hourlyValues(mean));
	}

	@Test
	void testDailyCounts() throws Exception {

		String version = "daily";

		new CreateCountsFromBAStData().execute(args(version));

		//The motorway test data starts at the first of January
		Path day = output(version).resolve("days").resolve("01").resolve("01.xml.gz");
		Counts<Link> counts = read(day);

		assertThat(counts.getMeasureLocations()).isNotEmpty();
		assertThat(counts.getYear()).isEqualTo(2021);

		for (MeasurementLocation<Link> location : counts.getMeasureLocations().values()) {
			Measurable volumes = location.getVolumesForMode(TransportMode.car);
			//A station is only written if it covers the whole day
			assertThat(volumes.size()).isEqualTo(24);
			assertThat(volumes.getAtHour(0)).isPresent();
		}

		//Daily counts are the measured values of a single day, not an aggregate over several
		assertThat(hourlyValues(counts)).isNotEqualTo(hourlyValues(read(countsFile(version, defaultAggregation))));

		try (var files = Files.list(output(version).resolve("days"))) {
			//One directory per month the data covers
			assertThat(files).isNotEmpty();
		}
	}

	@Test
	void testWithoutDailyCounts() {

		String version = "no-daily";

		new CreateCountsFromBAStData().execute(args(version, "--skip-daily-counts"));

		assertThat(countsFile(version, defaultAggregation)).exists();
		assertThat(output(version).resolve("days")).doesNotExist();
	}

	/**
	 * All hourly car volumes of a counts file, keyed by link and hour, to compare two aggregations by their values.
	 */
	private Map<String, Double> hourlyValues(Counts<Link> counts) {

		Map<String, Double> values = new TreeMap<>();

		for (Map.Entry<Id<Link>, MeasurementLocation<Link>> entry : counts.getMeasureLocations().entrySet()) {
			Measurable volumes = entry.getValue().getVolumesForMode(TransportMode.car);

			for (int hour = 0; hour < 24; hour++) {
				String key = entry.getKey() + "_" + hour;
				volumes.getAtHour(hour).ifPresent(value -> values.put(key, value));
			}
		}

		return values;
	}
}
