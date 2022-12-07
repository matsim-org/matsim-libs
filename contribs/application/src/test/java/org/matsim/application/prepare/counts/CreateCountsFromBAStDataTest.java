package org.matsim.application.prepare.counts;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.testcases.MatsimTestUtils;

public class CreateCountsFromBAStDataTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	String carOutput = "car-test-counts.xml.gz";
	String freightOutput = "freight-test-counts.xml.gz";
	String ignoredCounts = "C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/ignored.csv";
	String manualMatchedCounts = "C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/manual.csv";

	@Test
	public void testCreateCountsFromBAStData(){

		String version = "normal-";
		String car = utils.getOutputDirectory() + version + carOutput;
		String freight = utils.getOutputDirectory() + version + freightOutput;

		String[] args = new String[]{
				"--station-data=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/Jawe2021_test_data.csv",
				"--network=C:\\Users\\ACER\\IdeaProjects\\matsim-libs\\examples\\scenarios\\berlin\\network.xml.gz",
				"--motorway-data=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/2021_A_S_test_data.zip",
				"--primary-data=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/2021_B_S_test_data.txt",
				"--year=2021",
				"--car-output=" + car,
				"--freight-output=" + freight
		};

		new CreateCountsFromBAStData().execute(args);

		Counts<Link> counts = new Counts<>();
		new MatsimCountsReader(counts).readFile(car);

		int size = counts.getCounts().size();

		Assert.assertTrue(size > 0);
	}

	@Test
	public void testWithIgnoredStations(){

		String version = "with-ignored-";
		String car1 = utils.getOutputDirectory() + version + carOutput;
		String freight1 = utils.getOutputDirectory() + version + freightOutput;

		String[] args1 = new String[]{
				"--station-data=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/Jawe2021_test_data.csv",
				"--network=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/009.output_network.xml.gz",
				"--motorway-data=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/2021_A_S_test_data.txt",
				"--primary-data=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/2021_B_S_test_data.txt",
				"--shp=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Shape/ruhrgebiet_boundary.shp",
				"--shp-crs=EPSG:25832",
				"--input-crs=EPSG:25832",
				"--year=2021",
				"--car-output=" + car1,
				"--freight-output=" + freight1
		};

		new CreateCountsFromBAStData().execute(args1);

		String car2 = utils.getOutputDirectory() + "without-ignored-" + carOutput;
		String freight2 = utils.getOutputDirectory() + "without-ignored-" + freightOutput;

		String[] args2 = new String[]{
				"--station-data=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/Jawe2021_test_data.csv",
				"--network=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/009.output_network.xml.gz",
				"--motorway-data=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/2021_A_S_test_data.txt",
				"--primary-data=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/2021_B_S_test_data.txt",
				"--shp=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Shape/ruhrgebiet_boundary.shp",
				"--shp-crs=EPSG:25832",
				"--input-crs=EPSG:25832",
				"--year=2021",
				"--car-output=" + car2,
				"--freight-output=" + freight2,
				"--ignored-counts=" + ignoredCounts,
		};

		new CreateCountsFromBAStData().execute(args2);

		Counts<Link> countsComplete = new Counts<>();
		Counts<Link> countsWithoutIgnored = new Counts<>();

		new MatsimCountsReader(countsComplete).readFile(car1);
		new MatsimCountsReader(countsWithoutIgnored).readFile(car2);

		Assert.assertTrue(countsComplete.getCounts().size() > countsWithoutIgnored.getCounts().size());
	}

	@Test
	public void testManualMatchedCounts(){

		String car2 = utils.getOutputDirectory() + "manual-matched-" + carOutput;
		String freight2 = utils.getOutputDirectory() + "manual-matched-" + freightOutput;

		String[] args2 = new String[]{
				"--station-data=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/Jawe2021_test_data.csv",
				"--network=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/009.output_network.xml.gz",
				"--motorway-data=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/2021_A_S_test_data.txt",
				"--primary-data=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Testdaten/2021_B_S_test_data.txt",
				"--shp=C:/Users/ACER/Desktop/Uni/VSP/Ruhrgebiet/Shape/ruhrgebiet_boundary.shp",
				"--shp-crs=EPSG:25832",
				"--input-crs=EPSG:25832",
				"--year=2021",
				"--car-output=" + car2,
				"--freight-output=" + freight2,
				"--manual-matched-counts=" + manualMatchedCounts,
		};

		new CreateCountsFromBAStData().execute(args2);

		Counts<Link> countsWithManualMatched = new Counts<>();

		new MatsimCountsReader(countsWithManualMatched).readFile(car2);

		Id<Link> target = Id.createLinkId("3506230770006f"); //Link is matched to station Bochum regular

		Assert.assertFalse(countsWithManualMatched.getCounts().containsKey(target));
	}
}
