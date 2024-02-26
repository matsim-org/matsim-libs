package org.matsim.contrib.drt.teleportation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.DrtEstimatorParams;
import org.matsim.contrib.drt.estimator.impl.PessimisticDrtEstimator;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
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
		config.controller().setLastIteration(3);

		// install the drt routing stuff, but not the mobsim stuff!
		Controler controler = DrtControlerCreator.createControler(config, false);
		DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);
		drtConfigGroup.maxTravelTimeAlpha = 1.1;
		drtConfigGroup.simulationType = DrtConfigGroup.SimulationType.estimateAndTeleport;

//		DrtEstimatorParams params = new DrtEstimatorParams();
//		drtConfigGroup.addParameterSet(params);

		DrtFareParams fareParams = new DrtFareParams();
		fareParams.baseFare = 1.0;
		fareParams.distanceFare_m = 0.001;
		drtConfigGroup.addParameterSet(fareParams);

		controler.addOverridingModule(new AbstractDvrpModeModule(drtConfigGroup.mode) {
			@Override
			public void install() {
				bindModal(DrtEstimator.class).toInstance(new PessimisticDrtEstimator(drtConfigGroup));
			}
		});


		System.out.println(config);

		// TODO
		// We want to use DRT infrastructure (routing) so we need to integrate into drt teleportation
		// Write our own TeleportingPassengerEngine
		// this engine can either calc estimates beforehand or during departure (using information of drt router)


		// alternative: implement our own router
		// do nothing drt specific -> calculate travel time information during routing
		// can use standard teleportation engines given route information
		// we need to update routes ourself, we have no drt access egress, no waiting times, no drt output or requests
		// this would be more general, could be useful for other use cases?
		// but we only need it for DRT for now?

		controler.run();

	}
}
