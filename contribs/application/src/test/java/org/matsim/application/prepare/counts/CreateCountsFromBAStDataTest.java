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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateCountsFromBAStDataTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	String countsOutput = "test-counts.xml.gz";

	String mapping = "mapping.csv";
	String wrongManualMatchedCounts = "wrong_manual.csv";

	String network = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("berlin"), "network.xml.gz").toString();
	String motorwayData = "2021_A_S_test_data.txt";
	String primaryData = "2021_B_S_test_data.txt.gz";
	String stationData = "Jawe2021_test_data.csv";
	String shp = "Bezirke-Berlin-shp.zip";

	String networkCrs = "EPSG:31468";
	String shpCrs = "EPSG:3857";

	@Test
	void testCreateCountsFromBAStData() {

		String version = "normal-";
		String out = utils.getOutputDirectory() + version + countsOutput;

		String[] args = new String[]{
			"--station-data=" + utils.getPackageInputDirectory() + stationData,
			"--network=" + network,
			"--input-crs=" + networkCrs,
			"--motorway-data=" + utils.getPackageInputDirectory() + motorwayData,
			"--primary-data=" + utils.getPackageInputDirectory() + primaryData,
			"--shp=" + utils.getPackageInputDirectory() + shp,
			"--shp-crs=" + shpCrs,
			"--year=2021",
			"--output=" + out,
		};

		new CreateCountsFromBAStData().execute(args);

		Counts<Link> counts = new Counts<>();
		new MatsimCountsReader(counts).readFile(out);

		assertThat(counts.getMeasureLocations())
			.hasSize(24);

		for (Map.Entry<Id<Link>, MeasurementLocation<Link>> e : counts.getMeasureLocations().entrySet()) {
			assertThat(e.getValue().hasMeasurableForMode(Measurable.VOLUMES, TransportMode.car))
				.isTrue();

			assertThat(e.getValue().hasMeasurableForMode(Measurable.VOLUMES, TransportMode.truck))
				.isTrue();
		}
	}

	@Test
	void testWithIgnoredStations() {

		String version = "with-ignored-";
		String out1 = utils.getOutputDirectory() + version + countsOutput;

		String[] args1 = new String[]{
			"--station-data=" + utils.getPackageInputDirectory() + stationData,
			"--network=" + network,
			"--input-crs=" + networkCrs,
			"--motorway-data=" + utils.getPackageInputDirectory() + motorwayData,
			"--primary-data=" + utils.getPackageInputDirectory() + primaryData,
			"--shp=" + utils.getPackageInputDirectory() + shp,
			"--shp-crs=" + shpCrs,
			"--year=2021",
			"--output=" + out1,
		};

		new CreateCountsFromBAStData().execute(args1);

		String out2 = utils.getOutputDirectory() + "without-ignored-" + countsOutput;

		String[] args2 = new String[]{
			"--station-data=" + utils.getPackageInputDirectory() + stationData,
			"--network=" + network,
			"--input-crs=" + networkCrs,
			"--motorway-data=" + utils.getPackageInputDirectory() + motorwayData,
			"--primary-data=" + utils.getPackageInputDirectory() + primaryData,
			"--shp=" + utils.getPackageInputDirectory() + shp,
			"--shp-crs=" + shpCrs,
			"--year=2021",
			"--output=" + out2,
			"--counts-mapping=" + utils.getPackageInputDirectory() + mapping,
		};

		new CreateCountsFromBAStData().execute(args2);

		Counts<Link> countsComplete = new Counts<>();
		Counts<Link> countsWithoutIgnored = new Counts<>();

		new MatsimCountsReader(countsComplete).readFile(out1);
		new MatsimCountsReader(countsWithoutIgnored).readFile(out2);

		int completeSize = countsComplete.getMeasureLocations().size();
		int ignoredSize = countsWithoutIgnored.getMeasureLocations().size();

		assertThat(completeSize).isGreaterThan(ignoredSize);
	}

	@Test
	void testManualMatchedCounts() {

		String out = utils.getOutputDirectory() + "manual-matched-" + countsOutput;

		//Map contains supposed matching from manual.csv
		Map<Id<Link>, String> manual = Map.of(Id.createLinkId("4205"), "Neukölln_N", Id.createLinkId("4219"), "Neukölln_S");

		String[] args = new String[]{
			"--station-data=" + utils.getPackageInputDirectory() + stationData,
			"--network=" + network,
			"--input-crs=" + networkCrs,
			"--motorway-data=" + utils.getPackageInputDirectory() + motorwayData,
			"--primary-data=" + utils.getPackageInputDirectory() + primaryData,
			"--shp=" + utils.getPackageInputDirectory() + shp,
			"--shp-crs=" + shpCrs,
			"--year=2021",
			"--output=" + out,
			"--counts-mapping=" + utils.getPackageInputDirectory() + mapping,
		};

		new CreateCountsFromBAStData().execute(args);

		Counts<Link> countsWithManualMatched = new Counts<>();

		new MatsimCountsReader(countsWithManualMatched).readFile(out);

		var map = countsWithManualMatched.getMeasureLocations();

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

		String out = utils.getOutputDirectory() + "manual-matched-" + countsOutput;

		String[] args = new String[]{
			"--station-data=" + utils.getPackageInputDirectory() + stationData,
			"--network=" + network,
			"--input-crs=" + networkCrs,
			"--motorway-data=" + utils.getPackageInputDirectory() + motorwayData,
			"--primary-data=" + utils.getPackageInputDirectory() + primaryData,
			"--shp=" + utils.getPackageInputDirectory() + shp,
			"--shp-crs=" + shpCrs,
			"--year=2021",
			"--output=" + out,
			"--counts-mapping=" + utils.getPackageInputDirectory() + wrongManualMatchedCounts,
		};

		Assertions.assertThrows(RuntimeException.class, () -> new CreateCountsFromBAStData().execute(args));
	}
}
