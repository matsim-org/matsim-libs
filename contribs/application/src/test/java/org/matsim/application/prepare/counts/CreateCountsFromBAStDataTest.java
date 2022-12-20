package org.matsim.application.prepare.counts;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

public class CreateCountsFromBAStDataTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	String carOutput = "car-test-counts.xml.gz";
	String freightOutput = "freight-test-counts.xml.gz";

	String ignoredCounts = "ignored.csv";
	String manualMatchedCounts = "manual.csv";
	String wrongManualMatchedCounts = "wrong_manual.csv";

	String network = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("berlin"), "network.xml.gz").toString();
	String motorwayData = "2021_A_S_test_data.txt";
	String primaryData = "2021_B_S_test_data.txt.gz";
	String stationData = "Jawe2021_test_data.csv";
	String shp = "Bezirke-Berlin-shp.zip";

	String networkCrs = "EPSG:31468";
	String shpCrs = "EPSG:3857";

	@Test
	public void testCreateCountsFromBAStData(){

		String version = "normal-";
		String car = utils.getOutputDirectory() + version + carOutput;
		String freight = utils.getOutputDirectory() + version + freightOutput;

		String[] args = new String[]{
				"--station-data=" + utils.getPackageInputDirectory() + stationData,
				"--network=" + network,
				"--input-crs=" + networkCrs,
				"--motorway-data=" + utils.getPackageInputDirectory() + motorwayData,
				"--primary-data=" + utils.getPackageInputDirectory() + primaryData,
				"--shp=" + utils.getPackageInputDirectory() + shp,
				"--shp-crs=" + shpCrs,
				"--year=2021",
				"--car-output=" + car,
				"--freight-output=" + freight
		};

		new CreateCountsFromBAStData().execute(args);

		Counts<Link> counts = new Counts<>();
		new MatsimCountsReader(counts).readFile(car);

		Integer size = counts.getCounts().size();

		assertThat(size).isGreaterThan(0);
	}

	@Test
	public void testWithIgnoredStations(){

		String version = "with-ignored-";
		String car1 = utils.getOutputDirectory() + version + carOutput;
		String freight1 = utils.getOutputDirectory() + version + freightOutput;

		String[] args1 = new String[]{
				"--station-data=" + utils.getPackageInputDirectory() + stationData,
				"--network=" + network,
				"--input-crs=" + networkCrs,
				"--motorway-data=" + utils.getPackageInputDirectory() + motorwayData,
				"--primary-data=" + utils.getPackageInputDirectory() + primaryData,
				"--shp=" + utils.getPackageInputDirectory() + shp,
				"--shp-crs=" + shpCrs,
				"--year=2021",
				"--car-output=" + car1,
				"--freight-output=" + freight1
		};

		new CreateCountsFromBAStData().execute(args1);

		String car2 = utils.getOutputDirectory() + "without-ignored-" + carOutput;
		String freight2 = utils.getOutputDirectory() + "without-ignored-" + freightOutput;

		String[] args2 = new String[]{
				"--station-data=" + utils.getPackageInputDirectory() + stationData,
				"--network=" + network,
				"--input-crs=" + networkCrs,
				"--motorway-data=" + utils.getPackageInputDirectory() + motorwayData,
				"--primary-data=" + utils.getPackageInputDirectory() + primaryData,
				"--shp=" + utils.getPackageInputDirectory() + shp,
				"--shp-crs=" + shpCrs,
				"--year=2021",
				"--car-output=" + car2,
				"--freight-output=" + freight2,
				"--ignored-counts=" + utils.getPackageInputDirectory() + ignoredCounts,
		};

		new CreateCountsFromBAStData().execute(args2);

		Counts<Link> countsComplete = new Counts<>();
		Counts<Link> countsWithoutIgnored = new Counts<>();

		new MatsimCountsReader(countsComplete).readFile(car1);
		new MatsimCountsReader(countsWithoutIgnored).readFile(car2);

		int completeSize = countsComplete.getCounts().size();
		int ignoredSize = countsWithoutIgnored.getCounts().size();

		assertThat(completeSize).isGreaterThan(ignoredSize);
	}

	@Test
	public void testManualMatchedCounts(){

		String car = utils.getOutputDirectory() + "manual-matched-" + carOutput;
		String freight = utils.getOutputDirectory() + "manual-matched-" + freightOutput;

		//Map contains supposed matching from manual.csv
		Map<Id<Link>, String> manual = Map.of(Id.createLinkId("4205"), "Neukölln", Id.createLinkId("4219"), "Neukölln");

		String[] args = new String[]{
				"--station-data=" + utils.getPackageInputDirectory() + stationData,
				"--network=" + network,
				"--input-crs=" + networkCrs,
				"--motorway-data=" + utils.getPackageInputDirectory() + motorwayData,
				"--primary-data=" + utils.getPackageInputDirectory() + primaryData,
				"--shp=" + utils.getPackageInputDirectory() + shp,
				"--shp-crs=" + shpCrs,
				"--year=2021",
				"--car-output=" + car,
				"--freight-output=" + freight,
				"--manual-matched-counts=" + utils.getPackageInputDirectory() + manualMatchedCounts,
		};

		new CreateCountsFromBAStData().execute(args);

		Counts<Link> countsWithManualMatched = new Counts<>();

		new CountsReaderMatsimV1(countsWithManualMatched).readFile(car);

		var map = countsWithManualMatched.getCounts();

		for(var entry: manual.entrySet()){

			Id<Link> supposed = entry.getKey();
			String station = entry.getValue();

			Count<Link> actual = map.get(supposed);
			String actualStation = actual.getCsLabel();

			Assert.assertEquals(station, actualStation);
		}
	}

	@Test
	public void testManualMatchingWithWrongInput(){

		String car = utils.getOutputDirectory() + "manual-matched-" + carOutput;
		String freight = utils.getOutputDirectory() + "manual-matched-" + freightOutput;

		String[] args = new String[]{
				"--station-data=" + utils.getPackageInputDirectory() + stationData,
				"--network=" + network,
				"--input-crs=" + networkCrs,
				"--motorway-data=" + utils.getPackageInputDirectory() + motorwayData,
				"--primary-data=" + utils.getPackageInputDirectory() + primaryData,
				"--shp=" + utils.getPackageInputDirectory() + shp,
				"--shp-crs=" + shpCrs,
				"--year=2021",
				"--car-output=" + car,
				"--freight-output=" + freight,
				"--manual-matched-counts=" + utils.getPackageInputDirectory() + wrongManualMatchedCounts,
		};

		Assert.assertThrows(RuntimeException.class, () -> new CreateCountsFromBAStData().execute(args));
	}
}
