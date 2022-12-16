package org.matsim.modechoice.commands;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.modechoice.TestScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class GenerateChoiceSetTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void command() throws URISyntaxException {

		Path out = Path.of(utils.getOutputDirectory());

		new GenerateChoiceSet().execute(
				"--scenario", TestScenario.class.getCanonicalName(),
				"--config", IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL("kelheim"),"config.xml").toString(),
				"--output", out.resolve("plans.xml").toString()
		);

	}
}
