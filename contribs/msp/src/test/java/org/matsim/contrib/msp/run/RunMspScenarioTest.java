package org.matsim.contrib.msp.run;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for {@link RunMspScenario}: makes sure the run script starts up and produces output.
 * It uses the equil example scenario, so that the test does not depend on any external input.
 */
public class RunMspScenarioTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void runsAndWritesOutput() {

		URL context = ExamplesUtils.getTestScenarioURL("equil");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"));

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(1);
		// fix the compression type, so that the file names asserted below do not depend on the default
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.gzip);

		Controler controler = MATSimApplication.prepare(new RunMspScenario(config), config);
		controler.run();

		Path output = Path.of(utils.getOutputDirectory());

		assertTrue(Files.exists(output.resolve("output_plans.xml.gz")), "output plans should have been written");
		assertTrue(Files.exists(output.resolve("output_network.xml.gz")), "output network should have been written");
		assertTrue(Files.exists(output.resolve("output_events.xml.gz")), "output events should have been written");
	}
}
