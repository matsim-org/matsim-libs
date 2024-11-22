package org.matsim.contrib.discrete_mode_choice;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.ModeAvailabilityModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;

public class DeterminismTest {
	private static void runConfig(URL configUrl, String outputDirectory) {
		Config config = ConfigUtils.loadConfig(configUrl, new DiscreteModeChoiceConfigGroup());
		config.controller().setLastIteration(2);
		config.controller().setOutputDirectory(outputDirectory);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DiscreteModeChoiceModule());
		controler.addOverridingModule(new ModeAvailabilityModule());
		controler.run();
	}


	@Test
	public void testSimulationDeterminism() {
		Logger logger = LogManager.getLogger(DeterminismTest.class);
		logger.info("Testing simulation determinism");
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("siouxfalls-2014"), "config_default.xml");
		int samples = 10;
		for(int i=0; i<samples; i++) {
			String outputDirectory = "test_determinism/output_"+i;
			MatsimRandom.reset();
			runConfig(configUrl, outputDirectory);
			String referencePlans = "test_determinism/output_0/output_plans.xml.gz";
			String comparedPlans = outputDirectory + "/output_plans.xml.gz";
			String referenceEvents = "test_determinism/output_0/output_events.xml.gz";
			String comparedEvents = outputDirectory + "/output_events.xml.gz";
			assert i == 0 || CRCChecksum.getCRCFromFile(referencePlans) == CRCChecksum.getCRCFromFile(comparedPlans);
			assert i == 0 || CRCChecksum.getCRCFromFile(referenceEvents) == CRCChecksum.getCRCFromFile(comparedEvents);
		}
	}
}
