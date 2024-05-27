package org.matsim.modechoice.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.modechoice.TestScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class GenerateChoiceSetTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void command() throws URISyntaxException {

		Path out = Path.of(utils.getOutputDirectory());

		new GenerateChoiceSet().execute(
				"--scenario", TestScenario.class.getCanonicalName(),
				"--config", IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL("kelheim"),"config.xml").toString(),
				"--output", out.resolve("plans.xml").toString()
		);

	}
}
