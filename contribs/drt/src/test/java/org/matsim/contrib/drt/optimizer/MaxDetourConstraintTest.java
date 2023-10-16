package org.matsim.contrib.drt.optimizer;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.optimizer.insertion.DefaultInsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.MaxDetourInsertionCostCalculator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
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
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMaxDetourConstraint() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
			new OTFVisConfigGroup());
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = MultiModeDrtConfigGroup.get(config);
		DrtConfigGroup drtConfigGroup = multiModeDrtConfigGroup.getModalElements().iterator().next();

		drtConfigGroup.maxDetourAlpha = 1.5;
		drtConfigGroup.maxDetourBeta = 300;

		drtConfigGroup.maxTravelTimeAlpha = 2;
		drtConfigGroup.maxTravelTimeBeta = 600;
		drtConfigGroup.maxWaitTime = 300;

		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		Controler controler = DrtControlerCreator.createControler(config, false);

		for (DrtConfigGroup drtCfg: multiModeDrtConfigGroup.getModalElements()) {
			controler.addOverridingModule(new AbstractDvrpModeModule(drtCfg.mode) {
				@Override
				public void install() {
					bindModal(InsertionCostCalculator.class).toProvider(modalProvider(
							getter -> new MaxDetourInsertionCostCalculator(new DefaultInsertionCostCalculator(getter.getModal(CostCalculationStrategy.class)))));
				}
			});
		}

		controler.run();
	}
}
