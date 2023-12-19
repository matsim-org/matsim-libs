package org.matsim.application.prepare.pt;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.nio.file.Path;

public class CreateTransitScheduleFromGtfsTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void run() {

		String input = utils.getClassInputDirectory();
		String output = utils.getOutputDirectory();

		URL network = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("berlin"), "network.xml.gz");

		// Just test for execution without exception, the given parameter may not lead to sensible results
		new CreateTransitScheduleFromGtfs().execute(
				Path.of(input).resolve("sample-feed.zip").toString(),
				"--name", "test",
				"--network", network.toString(),
				"--target-crs", "EPSG:4326",
				"--date", "2019-01-01",
				"--output", output
		);

		Assertions.assertThat(Path.of(output))
				.isDirectoryContaining(p -> p.getFileName().toString().equals("network-with-pt.xml.gz"))
				.isDirectoryContaining(p -> p.getFileName().toString().equals("test-transitSchedule.xml.gz"));

	}
}
