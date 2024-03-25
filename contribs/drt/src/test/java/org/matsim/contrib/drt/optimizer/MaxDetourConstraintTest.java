package org.matsim.contrib.drt.optimizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;

public class MaxDetourConstraintTest {
	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMaxDetourConstraint() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
			new OTFVisConfigGroup());
		DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);

		// Max wait time
		drtConfigGroup.maxWaitTime = 300;

		// Turn on the max detour constraint
		drtConfigGroup.maxDetourAlpha = 1.5;
		drtConfigGroup.maxDetourBeta = 300;
		drtConfigGroup.maxAllowedPickupDelay = 180;
		drtConfigGroup.maxAbsoluteDetour = 1200;

		// Make the max total travel time constraints very loose (i.e., make it not active)
		drtConfigGroup.maxTravelTimeAlpha = 10;
		drtConfigGroup.maxTravelTimeBeta = 7200;

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Controler controler = DrtControlerCreator.createControler(config, false);

		controler.run();
	}
}
