package org.matsim.contrib.drt.teleportation;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.estimator.DrtEstimatorParams;
import org.matsim.contrib.drt.routing.*;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;
import java.util.*;

class Test2 {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@org.junit.jupiter.api.Test
	void test1() {

		URL url = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(url, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());

		config.network().setInputFile("network.xml");
		config.plans().setInputFile("plans_only_drt_1.0.xml.gz");

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);

		// install the drt routing stuff, but not the mobsim stuff!
		Controler controler = DrtControlerCreator.createControler(config, false);


		DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);

		drtConfigGroup.simulationType = DrtConfigGroup.SimulationType.estimateAndTeleport;

		DrtEstimatorParams params = new DrtEstimatorParams();
		drtConfigGroup.addParameterSet(params);

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
