package org.matsim.contrib.drt.teleportation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.PessimisticDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.RealisticDrtEstimator;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;

class DrtTeleportationTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testPessimisticEstimator() {
		URL url = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(url, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.network().setInputFile("network.xml");
		config.plans().setInputFile("plans_only_drt_1.0.xml.gz");
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(2);

		Controler controler = DrtControlerCreator.createControler(config, false);
		DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);
		drtConfigGroup.maxTravelTimeAlpha = 1.2;
		drtConfigGroup.maxTravelTimeBeta = 600;
		drtConfigGroup.maxWaitTime = 300;
		DrtFareParams fareParams = new DrtFareParams();
		fareParams.baseFare = 1.0;
		fareParams.distanceFare_m = 0.001;
		drtConfigGroup.addParameterSet(fareParams);

		// Setup to enable estimator and teleportation
		drtConfigGroup.simulationType = DrtConfigGroup.SimulationType.estimateAndTeleport;
		controler.addOverridingModule(new AbstractDvrpModeModule(drtConfigGroup.mode) {
			@Override
			public void install() {
				bindModal(DrtEstimator.class).toInstance(new PessimisticDrtEstimator(drtConfigGroup));
			}
		});

		controler.run();
		// TODO add a check
	}

	@Test
	void testRealisticEstimator() {
		URL url = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(url, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.network().setInputFile("network.xml");
		config.plans().setInputFile("plans_only_drt_1.0.xml.gz");
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(2);

		// install the drt routing stuff, but not the mobsim stuff!
		Controler controler = DrtControlerCreator.createControler(config, false);
		DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);
		drtConfigGroup.maxTravelTimeAlpha = 1.2;
		drtConfigGroup.maxTravelTimeBeta = 600;
		drtConfigGroup.maxWaitTime = 300;
		DrtFareParams fareParams = new DrtFareParams();
		fareParams.baseFare = 1.0;
		fareParams.distanceFare_m = 0.001;
		drtConfigGroup.addParameterSet(fareParams);

		// Setup to enable estimator and teleportation
		drtConfigGroup.simulationType = DrtConfigGroup.SimulationType.estimateAndTeleport;
		controler.addOverridingModule(new AbstractDvrpModeModule(drtConfigGroup.mode) {
			@Override
			public void install() {
				bindModal(DrtEstimator.class).toInstance(new RealisticDrtEstimator(
					new RealisticDrtEstimator.DistributionGenerator(1.2, 150, 0.1, 180, 0.2)));
			}
		});

		controler.run();
		// TODO add a check
	}
}
