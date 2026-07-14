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

import ch.sbb.matsim.contrib.railsim.events.RailsimTrainLeavesLinkEvent;
import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.NoDeadlockAvoidance;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.MaxSpeedProfile;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.SimpleDisposition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManagerImpl;
import ch.sbb.matsim.contrib.railsim.qsimengine.router.TrainRouter;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import static ch.sbb.matsim.contrib.railsim.qsimengine.RailsimTestUtils.createTrainTimeDistanceHandler;
import static org.assertj.core.api.Assertions.assertThat;

public class RailsimEngineTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

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
		TrainManager trains = new TrainManager();
		RailResourceManager res = new RailResourceManagerImpl(eventsManager, config, net, new NoDeadlockAvoidance(net), trains);
		MaxSpeedProfile speed = new MaxSpeedProfile();
		TrainRouter router = new TrainRouter(net, res);
		TrainTimeDistanceHandler ttd = createTrainTimeDistanceHandler();

		return new RailsimTestUtils.Holder(new RailsimEngine(eventsManager, config, res, trains, new SimpleDisposition(res, speed, router), ttd), net);
	}

	private RailsimTestUtils.Holder getTestEngine(String network) {
		return getTestEngine(network, null);
	}

	private void invokeReverseTrain(RailsimEngine engine, UpdateEvent event, RailLink nextLink,
									Id<Link> previousHeadLink, double previousHeadPosition,
									Id<Link> previousTailLink, double previousTailPosition,
									int nextRouteIdx) throws ReflectiveOperationException {
		Method reverseTrain = RailsimEngine.class.getDeclaredMethod("reverseTrain", TrainState.class, RailLink.class, Id.class, double.class, Id.class, double.class, int.class);
		reverseTrain.setAccessible(true);
		reverseTrain.invoke(engine, event.state, nextLink, previousHeadLink, previousHeadPosition, previousTailLink, previousTailPosition, nextRouteIdx);
	}

	private void invokeUpdateDeparture(RailsimEngine engine, double time, UpdateEvent event) throws ReflectiveOperationException {
		Method updateDeparture = RailsimEngine.class.getDeclaredMethod("updateDeparture", double.class, UpdateEvent.class);
		updateDeparture.setAccessible(true);
		updateDeparture.invoke(engine, time, event);
	}

	private void invokeEnterLink(RailsimEngine engine, double time, UpdateEvent event) throws ReflectiveOperationException {
		Method enterLink = RailsimEngine.class.getDeclaredMethod("enterLink", double.class, UpdateEvent.class);
		enterLink.setAccessible(true);
		enterLink.invoke(engine, time, event);
	}

	private void invokeLeaveLink(RailsimEngine engine, double time, UpdateEvent event) throws ReflectiveOperationException {
		Method leaveLink = RailsimEngine.class.getDeclaredMethod("leaveLink", double.class, UpdateEvent.class);
		leaveLink.setAccessible(true);
		leaveLink.invoke(engine, time, event);
	}

	@Test
	void testSimple() {

		RailsimTestUtils.Holder test = getTestEngine("networkMicroBi.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "train", 0, "l1-2", "l5-6");

		test.doSimStepUntil(400);
		test.debugFiles(collector, utils.getOutputDirectory() + "/simple");

		RailsimTestUtils.assertThat(collector)
			.hasSizeGreaterThan(5)
			.hasTrainState("train", 144, 0, 44)
			.hasTrainState("train", 234, 2000, 0);

		test = getTestEngine("networkMicroBi.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "train", 0, "l1-2", "l5-6");

		test.doStateUpdatesUntil(400, 1);
		test.debugFiles(collector, utils.getOutputDirectory() + "/simple_detailed");

		RailsimTestUtils.assertThat(collector)
			.hasSizeGreaterThan(5)
			.hasTrainState("train", 144, 0, 44)
			.hasTrainState("train", 234, 2000, 0);

	}

	@Test
	void testReverseTrainSingleLinkKeepsExactState() throws ReflectiveOperationException {
		Network net = NetworkUtils.readNetwork(new File(utils.getPackageInputDirectory(), "networkMicroBi.xml").toString());
		RailResourceManager resources = new RailResourceManagerImpl(eventsManager, new RailsimConfigGroup(), net, new NoDeadlockAvoidance(net), new TrainManager());
		RailLink forward = resources.getLink(Id.createLinkId("l1-2"));
		RailLink reverse = resources.getLink(Id.createLinkId("l2-1"));

		TrainState state = new TrainState(null, new TrainInfo(Id.create("reversible", VehicleType.class), 200, 10, 1, 1, 1, 60),
			0, forward.getLinkId(), new ArrayList<>(List.of(forward, reverse)));
		state.headLink = forward.getLinkId();
		state.headPosition = 1000;
		state.tailLink = forward.getLinkId();
		state.tailPosition = 800;
		state.cumulativeDistance = 1234;
		state.routeIdx = 1;

		UpdateEvent event = new UpdateEvent(state, UpdateEvent.Type.REVERSE_TRAIN);
		invokeReverseTrain(new RailsimEngine(null, null, null, null, null, null), event, reverse, forward.getLinkId(), 1000, forward.getLinkId(), 800, 2);

		assertThat(state.headLink).isEqualTo(reverse.getLinkId());
		assertThat(state.headPosition).isEqualTo(200);
		assertThat(state.tailLink).isEqualTo(forward.getLinkId());
		assertThat(state.tailPosition).isEqualTo(1000);
		assertThat(state.cumulativeDistance).isEqualTo(1434);
		assertThat(state.routeIdx).isEqualTo(2);
		assertThat(RailsimCalc.checkTrainLength(state)).isTrue();
	}

	@Test
	void testUpdateDepartureKeepsExistingGeometryAcrossCirculationBoundary() throws ReflectiveOperationException {
		File integrationDir = new File(new File(utils.getPackageInputDirectory()).getParentFile(), "integration/shuttleMicro");
		Network net = NetworkUtils.readNetwork(new File(integrationDir, "trainNetwork.xml").toString());
		RailsimConfigGroup config = new RailsimConfigGroup();
		TrainManager trains = new TrainManager();
		RailResourceManager resources = new RailResourceManagerImpl(eventsManager, config, net, new NoDeadlockAvoidance(net), trains);
		MaxSpeedProfile speed = new MaxSpeedProfile();
		TrainRouter router = new TrainRouter(net, resources);
		RailsimEngine engine = new RailsimEngine(eventsManager, config, resources, trains, new SimpleDisposition(resources, speed, router),
			createTrainTimeDistanceHandler());

		RailLink aToStop1 = resources.getLink(Id.createLinkId("link_A_to_stop_1"));
		RailLink aToStop2 = resources.getLink(Id.createLinkId("link_A_to_stop_2"));
		RailLink aToStop3 = resources.getLink(Id.createLinkId("link_A_to_stop_3"));
		RailLink am1 = resources.getLink(Id.createLinkId("link_AM_1"));
		RailLink am2 = resources.getLink(Id.createLinkId("link_AM_2"));

		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("train_type", VehicleType.class));
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("train_1"), vehicleType);
		MobsimVehicle mobsimVehicle = Mockito.mock(MobsimVehicle.class, Answers.RETURNS_MOCKS);
		RailsimTransitDriverAgent driver = Mockito.mock(RailsimTransitDriverAgent.class, Answers.RETURNS_MOCKS);
		TransitStopFacility stop = Mockito.mock(TransitStopFacility.class);
		TransitStopFacility nextStop = Mockito.mock(TransitStopFacility.class);

		Mockito.when(mobsimVehicle.getVehicle()).thenReturn(vehicle);
		Mockito.when(mobsimVehicle.getId()).thenReturn(vehicle.getId());
		Mockito.when(driver.getVehicle()).thenReturn(mobsimVehicle);
		Mockito.when(driver.getId()).thenReturn(Id.createPersonId("driver_train_1"));
		Mockito.when(driver.getMode()).thenReturn("rail");
		Mockito.when(driver.getNextTransitStop()).thenReturn(stop);
		Mockito.when(stop.getLinkId()).thenReturn(aToStop3.getLinkId());
		Mockito.when(driver.handleTransitStop(stop, 1335.0)).thenReturn(60.0);

		TrainState state = new TrainState(driver, new TrainInfo(Id.create("train_type", VehicleType.class), 150, 20, 1, 1, 1, 60),
			1335, aToStop3.getLinkId(), new ArrayList<>(List.of(aToStop3, am1, am2)));
		state.headLink = aToStop3.getLinkId();
		state.headPosition = aToStop3.length;
		state.tailLink = aToStop1.getLinkId();
		state.tailPosition = 0;
		state.previousRoute.addAll(List.of(aToStop1, aToStop2, aToStop3));

		assertThat(RailsimCalc.checkTrainLength(state)).isTrue();

		UpdateEvent event = new UpdateEvent(state, UpdateEvent.Type.DEPARTURE);
		invokeUpdateDeparture(engine, 1335, event);

		assertThat(state.headLink).isEqualTo(aToStop3.getLinkId());
		assertThat(state.headPosition).isEqualTo(aToStop3.length);
		assertThat(state.tailLink).isEqualTo(aToStop1.getLinkId());
		assertThat(state.tailPosition).isZero();
		assertThat(state.routeIdx).isEqualTo(1);
		assertThat(RailsimCalc.checkTrainLength(state)).isTrue();
		assertThat(event.type).isEqualTo(UpdateEvent.Type.ENTER_LINK);
		assertThat(event.plannedTime).isEqualTo(1395);
	}

	@Test
	void testReverseStopUsesScheduledDwellOnlyOnce() throws ReflectiveOperationException {
		Network net = NetworkUtils.readNetwork(new File(utils.getPackageInputDirectory(), "networkMicroBi.xml").toString());
		RailsimConfigGroup config = new RailsimConfigGroup();
		TrainManager trains = new TrainManager();
		RailResourceManager resources = new RailResourceManagerImpl(eventsManager, config, net, new NoDeadlockAvoidance(net), trains);
		MaxSpeedProfile speed = new MaxSpeedProfile();
		TrainRouter router = new TrainRouter(net, resources);
		RailsimEngine engine = new RailsimEngine(eventsManager, config, resources, trains, new SimpleDisposition(resources, speed, router),
			createTrainTimeDistanceHandler());

		RailLink forward = resources.getLink(Id.createLinkId("l1-2"));
		RailLink reverse = resources.getLink(Id.createLinkId("l2-1"));

		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("reversible", VehicleType.class));
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("train_reverse_stop"), vehicleType);
		MobsimVehicle mobsimVehicle = Mockito.mock(MobsimVehicle.class, Answers.RETURNS_MOCKS);
		RailsimTransitDriverAgent driver = Mockito.mock(RailsimTransitDriverAgent.class, Answers.RETURNS_MOCKS);
		TransitStopFacility stop = Mockito.mock(TransitStopFacility.class);
		TransitStopFacility nextStop = Mockito.mock(TransitStopFacility.class);

		Mockito.when(mobsimVehicle.getVehicle()).thenReturn(vehicle);
		Mockito.when(mobsimVehicle.getId()).thenReturn(vehicle.getId());
		Mockito.when(driver.getVehicle()).thenReturn(mobsimVehicle);
		Mockito.when(driver.getId()).thenReturn(Id.createPersonId("driver_train_reverse_stop"));
		Mockito.when(driver.getMode()).thenReturn("rail");
		Mockito.when(driver.getNextTransitStop()).thenReturn(stop, nextStop);
		Mockito.when(stop.getLinkId()).thenReturn(forward.getLinkId());
		Mockito.when(nextStop.getLinkId()).thenReturn(reverse.getLinkId());
		Mockito.when(driver.handleTransitStop(stop, 300.0)).thenReturn(60.0);
		Mockito.when(driver.handleTransitStop(stop, 360.0)).thenReturn(10.0);
		Mockito.when(driver.handleTransitStop(stop, 370.0)).thenReturn(0.0);

		TrainState state = new TrainState(driver, new TrainInfo(vehicleType.getId(), 200, 20, 1, 1, 1, 60),
			300, forward.getLinkId(), new ArrayList<>(List.of(forward, reverse)));
		state.headLink = forward.getLinkId();
		state.headPosition = forward.length;
		state.tailLink = forward.getLinkId();
		state.tailPosition = forward.length - state.train.length();
		state.routeIdx = 1;
		state.speed = 0;
		state.acceleration = 0;

		UpdateEvent event = new UpdateEvent(state, UpdateEvent.Type.ENTER_LINK);

		invokeEnterLink(engine, 300, event);

		assertThat(event.plannedTime).isEqualTo(360);
		assertThat(event.lastArrivalTime).isEqualTo(300);
		assertThat(event.type).isEqualTo(UpdateEvent.Type.ENTER_LINK);

		invokeEnterLink(engine, 360, event);

		assertThat(event.plannedTime).isEqualTo(370);
		assertThat(event.lastArrivalTime).isEqualTo(300);
		assertThat(event.type).isEqualTo(UpdateEvent.Type.ENTER_LINK);

		invokeEnterLink(engine, 370, event);

		assertThat(event.plannedTime).isEqualTo(370);
		assertThat(event.lastArrivalTime).isEqualTo(300);
		assertThat(event.type).isEqualTo(UpdateEvent.Type.ENTER_LINK);

		invokeEnterLink(engine, 370, event);

		assertThat(event.type).isEqualTo(UpdateEvent.Type.REVERSE_TRAIN);
		assertThat(event.plannedTime).isEqualTo(370);
		assertThat(event.lastArrivalTime).isEqualTo(300);
	}

	@Test
	void testLeaveLinkDoesNotAdvanceTailBeforeBoundary() throws ReflectiveOperationException {
		Network net = NetworkUtils.readNetwork(new File(utils.getPackageInputDirectory(), "networkMixedTypes.xml").toString());
		RailsimConfigGroup config = new RailsimConfigGroup();
		TrainManager trains = new TrainManager();
		RailResourceManager resources = new RailResourceManagerImpl(eventsManager, config, net, new NoDeadlockAvoidance(net), trains);
		MaxSpeedProfile speed = new MaxSpeedProfile();
		TrainRouter router = new TrainRouter(net, resources);
		RailsimEngine engine = new RailsimEngine(eventsManager, config, resources, trains, new SimpleDisposition(resources, speed, router),
			createTrainTimeDistanceHandler());

		RailLink link12 = resources.getLink(Id.createLinkId("1-2"));
		RailLink link23 = resources.getLink(Id.createLinkId("2-3"));
		RailLink link34 = resources.getLink(Id.createLinkId("3-4"));

		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("sprinter"), RailsimTestUtils.vehicles.get(TestVehicle.Sprinter));
		MobsimVehicle mobsimVehicle = Mockito.mock(MobsimVehicle.class, Answers.RETURNS_MOCKS);
		RailsimTransitDriverAgent driver = Mockito.mock(RailsimTransitDriverAgent.class, Answers.RETURNS_MOCKS);

		Mockito.when(mobsimVehicle.getVehicle()).thenReturn(vehicle);
		Mockito.when(mobsimVehicle.getId()).thenReturn(vehicle.getId());
		Mockito.when(driver.getVehicle()).thenReturn(mobsimVehicle);
		Mockito.when(driver.getId()).thenReturn(Id.createPersonId("driver_sprinter"));
		Mockito.when(driver.getMode()).thenReturn("rail");

		TrainState state = new TrainState(driver,
			new TrainInfo(vehicle.getType().getId(), vehicle.getType().getLength(), 90, 0.7, 0.7, 0.7, -1),
			707.298, link23.getLinkId(), new ArrayList<>(List.of(link12, link23, link34)));
		state.headLink = link23.getLinkId();
		state.headPosition = 7.561;
		state.tailLink = link12.getLinkId();
		state.tailPosition = 807.561;
		state.routeIdx = 2;
		state.speed = 0;
		state.targetSpeed = 0;
		state.acceleration = 0;
		state.approvedDist = 0;
		state.approvedSpeed = 0;

		assertThat(RailsimCalc.checkTrainLength(state)).isTrue();

		UpdateEvent event = new UpdateEvent(state, UpdateEvent.Type.LEAVE_LINK);
		event.plannedTime = 708.0;

		invokeLeaveLink(engine, 708.0, event);

		assertThat(state.tailLink).isEqualTo(link12.getLinkId());
		assertThat(state.tailPosition).isEqualTo(807.561);
		assertThat(RailsimCalc.checkTrainLength(state)).isTrue();
		assertThat(event.type).isNotEqualTo(UpdateEvent.Type.LEAVE_LINK);
		assertThat(collector.events).noneMatch(RailsimTrainLeavesLinkEvent.class::isInstance);
	}

	@Test
	void testCongested() {

		RailsimTestUtils.Holder test = getTestEngine("networkMicroBi.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo", 0, "l1-2", "l5-6");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 60, "l1-2", "l5-6");

		test.doSimStepUntil(600);
		test.debugFiles(collector, utils.getOutputDirectory() + "/congested");

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
		test.debugFiles(collector, utils.getOutputDirectory() + "/congestedWithHeadway");

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
		test.debugFiles(collector, utils.getOutputDirectory() + "/opposite");

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio1", 293, 600, 0)
			.hasTrainState("regio2", 358, 1000, 0);


		test = getTestEngine("networkMicroBi.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio1", 0, "l1-2", "l7-8");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2", 0, "l8-7", "l2-1");

		test.doStateUpdatesUntil(600, 1);
		test.debugFiles(collector, utils.getOutputDirectory() + "/opposite_detailed");

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio1", 293, 600, 0)
			.hasTrainState("regio2", 358, 1000, 0);

	}

	@Test
	void testVaryingSpeedOne() {

		RailsimTestUtils.Holder test = getTestEngine("networkMesoUni.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "t1_IN-t1_OUT", "t3_IN-t3_OUT");

		test.doSimStepUntil(10000);
		test.debugFiles(collector, utils.getOutputDirectory() + "/varyingSpeedOne");

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio", 7599, 0, 2.7777777)
			.hasTrainState("regio", 7674, 200, 0);

		test = getTestEngine("networkMesoUni.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "t1_IN-t1_OUT", "t3_IN-t3_OUT");

		test.doStateUpdatesUntil(10000, 1);
		test.debugFiles(collector, utils.getOutputDirectory() + "/varyingSpeedOne_detailed");

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
		test.debugFiles(collector, utils.getOutputDirectory() + "/varyingSpeedMany");

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
		test.debugFiles(collector, utils.getOutputDirectory() + "/varyingSpeedMany_detailed");

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
		test.debugFiles(collector, utils.getOutputDirectory() + "/trainFollowing");

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio1", 1138, 1000, 0)
			.hasTrainState("regio2", 1517, 1000, 0);

		test = getTestEngine("networkMicroUni.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio1", 0, "1-2", "20-21");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2", 0, "1-2", "20-21");

		test.doStateUpdatesUntil(5000, 1);
		test.debugFiles(collector, utils.getOutputDirectory() + "/trainFollowing_detailed");

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
		test.debugFiles(collector, utils.getOutputDirectory() + "/microVarying");

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("cargo1", 1278, 1000, 0)
			.hasTrainState("cargo2", 2033, 1000, 0);

		// Same test with state updates
		test = getTestEngine("networkMicroVaryingSpeed.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo1", 0, "1-2", "20-21");
		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo2", 15, "1-2", "20-21");
		test.doStateUpdatesUntil(3000, 1);
		test.debugFiles(collector, utils.getOutputDirectory() + "/microVarying_detailed");

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("cargo1", 1278, 1000, 0)
			.hasTrainState("cargo2", 2033, 1000, 0);


	}
}
