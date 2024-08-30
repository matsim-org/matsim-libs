package org.matsim.contrib.drt.teleportation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.EuclideanDistanceBasedDrtEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;

public class DrtTeleportationWithModeChoiceTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Disabled("This test is for example purposes only")
	@Test
	void testModeChoice() {
		URL url = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(url, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.network().setInputFile("network.xml");
		config.plans().setInputFile("plans_only_drt_4.0.xml.gz");
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(3);

		config.replanning().setFractionOfIterationsToDisableInnovation(0.8);
		config.replanning().setMaxAgentPlanMemorySize(3);
		config.replanning().clearStrategySettings();

		ReplanningConfigGroup.StrategySettings changeSingleTripMode = new ReplanningConfigGroup.StrategySettings();
		changeSingleTripMode.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode);
		changeSingleTripMode.setWeight(0.1);
		config.replanning().addStrategySettings(changeSingleTripMode);

		ReplanningConfigGroup.StrategySettings changeExpBeta = new ReplanningConfigGroup.StrategySettings();
		changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
		changeExpBeta.setWeight(0.9);
		config.replanning().addStrategySettings(changeExpBeta);
		// Introduce a dummy alternative mode: bike (also teleported)
		ScoringConfigGroup.ModeParams bikeModeParams = new ScoringConfigGroup.ModeParams(TransportMode.bike);
		bikeModeParams.setConstant(1.);
		bikeModeParams.setMarginalUtilityOfTraveling(-6.);
		config.scoring().addModeParams(bikeModeParams);
		// Update change mode
		config.changeMode().setModes(new String[]{TransportMode.drt, TransportMode.bike});

		// Setting DRT config group
		DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);

		drtConfigGroup.simulationType = DrtConfigGroup.SimulationType.estimateAndTeleport;

		Controler controler = DrtControlerCreator.createControler(config, false);

		controler.addOverridingModule(new AbstractDvrpModeModule(drtConfigGroup.mode) {
			@Override
			public void install() {
				bindModal(DrtEstimator.class).toProvider(modalProvider(getter -> new
					EuclideanDistanceBasedDrtEstimator(getter.getModal(Network.class), 2.0, 0.1577493,
					103.0972273, 120, 0.3, -0.1, 0.28)));
			}
		});

		System.out.println(config);
		controler.run();
	}
}
