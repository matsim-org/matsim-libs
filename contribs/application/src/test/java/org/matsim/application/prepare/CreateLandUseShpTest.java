package org.matsim.application.prepare;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateLandUseShpTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void convert() {

		Path input = Path.of(utils.getClassInputDirectory(), "andorra-latest-free.shp.zip");

		Assumptions.assumeTrue(Files.exists(input));

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
