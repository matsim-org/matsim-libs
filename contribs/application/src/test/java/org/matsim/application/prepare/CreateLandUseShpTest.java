package org.matsim.application.prepare;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public class CreateLandUseShpTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void convert() {

		// use the pbf of the osm reader
		Path input = Path.of(utils.getClassInputDirectory(), "andorra-latest-free.shp.zip");

		Assume.assumeTrue(Files.exists(input));

		Path output = Path.of(utils.getOutputDirectory(), "output.shp");

		new CreateLandUseShp().execute(
				input.toString(),
				"--output", output.toString()
		);

	}
}