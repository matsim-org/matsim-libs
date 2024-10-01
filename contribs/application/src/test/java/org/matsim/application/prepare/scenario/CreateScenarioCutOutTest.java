package org.matsim.application.prepare.scenario;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;

class CreateScenarioCutOutTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Test the cutout without facilities and without events.
	 */
	@Test
	void testBasicCutout() {

	}

	/**
	 * Test the cutout with facilities but without events.
	 */
	@Test
	void testFacilitiesCutout() {

	}

	/**
	 * Test the cutout without facilities but with events.
	 */
	@Test
	void testEventCutout() {

	}

	/**
	 * Test the cutout with both.
	 */
	//@Test //TODO Add this test in when finished
	void testFullCutOut() {
		// TODO Use smaller scenario like chessboard
		// TODO, change file paths
		new CreateScenarioCutOut().execute(
			"--buffer", "100",
			"--input", "../data/010.output_plans.xml.gz",
			"--network", "../data/berlin-v6.0-network.xml.gz",
			"--events", "../data/010.output_events.xml.gz",
			"--facilities", "../data/berlin-v6.0-facilities.xml.gz",
			"--output-network", "../data/cut-network.xml",
			"--output-population", "../data/cut-population.xml",
			"--output-network-change-events", "../data/cut-change-events.xml",
			"--input-crs=EPSG:25832",
			"--target-crs=EPSG:25832",
			"--shp=../data/gartenfeld-shape.shp"
		);

		try{
			Assertions.assertTrue(FileUtils.contentEquals(new File("../data/cut-network.xml"), new File("../data/test/cut-network.xml")));
			Assertions.assertTrue(FileUtils.contentEquals(new File("../data/cut-population.xml"), new File("../data/test/cut-population.xml")));
			Assertions.assertTrue(FileUtils.contentEquals(new File("../data/cut-change-events.xml"), new File("../data/test/cut-change-events2.xml")));
		} catch (Exception e){
			Assertions.fail(e);
		}
	}
}
