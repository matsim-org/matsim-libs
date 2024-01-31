/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.SimpleDisposition;
import ch.sbb.matsim.contrib.railsim.qsimengine.router.TrainRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.util.function.Consumer;

public class RailsimEngineTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private EventsManager eventsManager;
	private RailsimTestUtils.EventCollector collector;

	@BeforeEach
	public void setUp() {
		eventsManager = EventsUtils.createEventsManager();
		collector = new RailsimTestUtils.EventCollector();

		eventsManager.addHandler(collector);
		eventsManager.initProcessing();
	}

	private RailsimTestUtils.Holder getTestEngine(String network, @Nullable Consumer<Link> f) {
		Network net = NetworkUtils.readNetwork(new File(utils.getPackageInputDirectory(), network).toString());
		RailsimConfigGroup config = new RailsimConfigGroup();

		collector.clear();

		if (f != null) {
			for (Link link : net.getLinks().values()) {
				f.accept(link);
			}
		}
		RailResourceManager res = new RailResourceManager(eventsManager, config, net);
		TrainRouter router = new TrainRouter(net, res);

		return new RailsimTestUtils.Holder(new RailsimEngine(eventsManager, config, res, new SimpleDisposition(res, router)), net);
	}

	private RailsimTestUtils.Holder getTestEngine(String network) {
		return getTestEngine(network, null);
	}

	@Test
	void testSimple() {

		RailsimTestUtils.Holder test = getTestEngine("networkMicroBi.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "train", 0, "l1-2", "l5-6");

		test.doSimStepUntil(400);

		RailsimTestUtils.assertThat(collector)
			.hasSizeGreaterThan(5)
			.hasTrainState("train", 144, 0, 44)
			.hasTrainState("train", 234, 2000, 0);

		test = getTestEngine("networkMicroBi.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "train", 0, "l1-2", "l5-6");

		test.doStateUpdatesUntil(400, 1);

		RailsimTestUtils.assertThat(collector)
			.hasSizeGreaterThan(5)
			.hasTrainState("train", 144, 0, 44)
			.hasTrainState("train", 234, 2000, 0);

	}

	@Test
	void testCongested() {

		RailsimTestUtils.Holder test = getTestEngine("networkMicroBi.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo", 0, "l1-2", "l5-6");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 60, "l1-2", "l5-6");

		test.doSimStepUntil(600);

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("cargo", 359, 2000, 0)
			.hasTrainState("regio", 474, 2000, 0);

	}

	@Test
	void testCongestedWithHeadway() {

		RailsimTestUtils.Holder test = getTestEngine("networkMicroBi.xml", l -> RailsimUtils.setMinimumHeadwayTime(l, 60));

		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo", 0, "l1-2", "l5-6");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 60, "l1-2", "l5-6");

		test.doSimStepUntil(600);

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("cargo", 359, 2000, 0)
			.hasTrainState("regio", 485, 2000, 0);

	}


	@Test
	void testOpposite() {

		RailsimTestUtils.Holder test = getTestEngine("networkMicroBi.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio1", 0, "l1-2", "l7-8");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2", 0, "l8-7", "l2-1");

		test.doSimStepUntil(600);

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio1", 293, 600, 0)
			.hasTrainState("regio2", 358, 1000, 0);


		test = getTestEngine("networkMicroBi.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio1", 0, "l1-2", "l7-8");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2", 0, "l8-7", "l2-1");

		test.doStateUpdatesUntil(600, 1);

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio1", 293, 600, 0)
			.hasTrainState("regio2", 358, 1000, 0);

	}

	@Test
	void testVaryingSpeedOne() {

		RailsimTestUtils.Holder test = getTestEngine("networkMesoUni.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "t1_IN-t1_OUT", "t3_IN-t3_OUT");

		test.doSimStepUntil(10000);

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio", 7599, 0, 2.7777777)
			.hasTrainState("regio", 7674, 200, 0);

		test = getTestEngine("networkMesoUni.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "t1_IN-t1_OUT", "t3_IN-t3_OUT");

		test.doStateUpdatesUntil(10000, 1);

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio", 7599, 0, 2.7777777)
			.hasTrainState("regio", 7674, 200, 0);

	}

	@Test
	void testVaryingSpeedMany() {

		RailsimTestUtils.Holder test = getTestEngine("networkMesoUni.xml");

		for (int i = 0; i < 10; i++) {
			RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio" + i, 60 * i, "t1_IN-t1_OUT", "t3_IN-t3_OUT");
		}

		test.doSimStepUntil(30000);

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio0", 7599, 0, 2.7777777)
			.hasTrainState("regio0", 7674, 200, 0)
			.hasTrainState("regio1", 7734, 200, 0)
			.hasTrainState("regio9", 23107, 200, 0);

		test = getTestEngine("networkMesoUni.xml");

		for (int i = 0; i < 10; i++) {
			RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio" + i, 60 * i, "t1_IN-t1_OUT", "t3_IN-t3_OUT");
		}

		test.doStateUpdatesUntil(30000, 1);

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio0", 7599, 0, 2.7777777)
			.hasTrainState("regio0", 7674, 200, 0)
			.hasTrainState("regio1", 7734, 200, 0)
			.hasTrainState("regio9", 23107, 200, 0);

	}

	@Test
	void testTrainFollowing() {

		RailsimTestUtils.Holder test = getTestEngine("networkMicroUni.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio1", 0, "1-2", "20-21");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2", 0, "1-2", "20-21");

		test.doSimStepUntil(5000);

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio1", 1138, 1000, 0)
			.hasTrainState("regio2", 1517, 1000, 0);

		test = getTestEngine("networkMicroUni.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio1", 0, "1-2", "20-21");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2", 0, "1-2", "20-21");

		test.doStateUpdatesUntil(5000, 1);

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio1", 1138, 1000, 0)
			.hasTrainState("regio2", 1517, 1000, 0);
	}

	@Test
	void testMicroTrainFollowingVaryingSpeed() {

		RailsimTestUtils.Holder test = getTestEngine("networkMicroVaryingSpeed.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo1", 0, "1-2", "20-21");
		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo2", 15, "1-2", "20-21");

		test.doSimStepUntil(3000);
//		test.debugFiles(collector, "microVarying");

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("cargo1", 1278, 1000, 0)
			.hasTrainState("cargo2", 2033, 1000, 0);

		// Same test with state updates
		test = getTestEngine("networkMicroVaryingSpeed.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo1", 0, "1-2", "20-21");
		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo2", 15, "1-2", "20-21");
		test.doStateUpdatesUntil(3000, 1);
//		test.debugFiles(collector, "microVarying_detailed");

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("cargo1", 1278, 1000, 0)
			.hasTrainState("cargo2", 2033, 1000, 0);


	}
}
