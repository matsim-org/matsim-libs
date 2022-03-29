package org.matsim.application.prepare;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateLandUseShpTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void convert() {

		Path input = Path.of(utils.getClassInputDirectory(), "andorra-latest-free.shp.zip");

		Assume.assumeTrue(Files.exists(input));

		Path output = Path.of(utils.getOutputDirectory(), "output.shp");

		new CreateLandUseShp().execute(
				input.toString(),
				"--output", output.toString()
		);

		assertThat(output)
				.exists()
				.isRegularFile();

	}
}