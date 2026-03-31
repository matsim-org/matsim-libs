package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEndsParkingSearch;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParkingOccupancyObserverTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void test_initial() {
		ParkingOccupancyObserver parkingOccupancyObserver = getParkingObserver();

		Id<Link> linkId = Id.createLinkId("l");
		parkingOccupancyObserver.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent(null, 0));
		Map<Id<Link>, ParkingCount> parkingCount = parkingOccupancyObserver.getParkingCount(0, Map.of(linkId, 1.0));
		assertEquals(Map.of(linkId, new ParkingCount(0, 2, 1.0)), parkingCount);

		checkFile();
	}

	@Test
	void test_unpark() {
		Id<Link> linkId = Id.createLinkId("l");

		ParkingOccupancyObserver parkingOccupancyObserver = getParkingObserver();
		parkingOccupancyObserver.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent(null, 10));
		parkingOccupancyObserver.handleEvent(new VehicleEntersTrafficEvent(10, Id.createPersonId("p"), linkId, Id.createVehicleId("v"), "car", 0));

		parkingOccupancyObserver.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent(null, 11));
		Map<Id<Link>, ParkingCount> parkingCount = parkingOccupancyObserver.getParkingCount(11, Map.of(linkId, 1.0));
		assertEquals(Map.of(linkId, new ParkingCount(0, 2, 1.0)), parkingCount);

		checkFile();
	}

	@Test
	void test_park() {
		Id<Link> linkId = Id.createLinkId("l");

		ParkingOccupancyObserver parkingOccupancyObserver = getParkingObserver();
		parkingOccupancyObserver.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent(null, 10));
		parkingOccupancyObserver.handleEvent(new VehicleEndsParkingSearch(10, Id.createPersonId("p"), linkId, Id.createVehicleId("v"), "car"));
		parkingOccupancyObserver.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent(null, 11));

		Map<Id<Link>, ParkingCount> parkingCount = parkingOccupancyObserver.getParkingCount(11, Map.of(linkId, 1.0));
		assertEquals(Map.of(linkId, new ParkingCount(1, 2, 1.0)), parkingCount);

		checkFile();
	}

	@Test
	void test_parkUnpark() {
		Id<Link> linkId = Id.createLinkId("l");

		ParkingOccupancyObserver parkingOccupancyObserver = getParkingObserver();
		parkingOccupancyObserver.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent(null, 10));
		parkingOccupancyObserver.handleEvent(new VehicleEndsParkingSearch(10, Id.createPersonId("p"), linkId, Id.createVehicleId("v"), "car"));
		parkingOccupancyObserver.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent(null, 20));
		parkingOccupancyObserver.handleEvent(new VehicleEntersTrafficEvent(20, Id.createPersonId("p"), linkId, Id.createVehicleId("v"), "car", 0));

		parkingOccupancyObserver.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent(null, 21));
		Map<Id<Link>, ParkingCount> parkingCount = parkingOccupancyObserver.getParkingCount(21, Map.of(linkId, 1.0));
		assertEquals(Map.of(linkId, new ParkingCount(0, 2, 1.0)), parkingCount);

		checkFile();
	}

	@Test
	void test_reset() {
		Id<Link> linkId = Id.createLinkId("l");

		ParkingOccupancyObserver parkingOccupancyObserver = getParkingObserver();
		parkingOccupancyObserver.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent(null, 10));
		parkingOccupancyObserver.handleEvent(new VehicleEndsParkingSearch(10, Id.createPersonId("p"), linkId, Id.createVehicleId("v"), "car"));

		{
			parkingOccupancyObserver.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent(null, 11));
			Map<Id<Link>, ParkingCount> parkingCount = parkingOccupancyObserver.getParkingCount(11, Map.of(linkId, 1.0));
			assertEquals(Map.of(linkId, new ParkingCount(1, 2, 1.0)), parkingCount);
		}
		checkFile(0);

		parkingOccupancyObserver.notifyBeforeMobsim(new BeforeMobsimEvent(null, 1, true));

		{
			parkingOccupancyObserver.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent(null, 0));
			Map<Id<Link>, ParkingCount> parkingCount = parkingOccupancyObserver.getParkingCount(0, Map.of(linkId, 1.0));
			assertEquals(Map.of(linkId, new ParkingCount(0, 2, 1.0)), parkingCount);
		}

		checkFile(1);
	}

	private ParkingOccupancyObserver getParkingObserver() {
		Network network = NetworkUtils.createNetwork();

		Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0, 0));
		Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0, 100));

		Id<Link> linkId = Id.createLinkId("l");
		Link l = NetworkUtils.createLink(linkId, node0, node1, network, 100, 10, 10, 1);
		l.getAttributes().putAttribute("onstreet_spots", 2);

		network.addNode(node0);
		network.addNode(node1);
		network.addLink(l);

		ParkingCapacityInitializer parkingCapacityInitializer = new ZeroParkingCapacityInitializer(network, ConfigUtils.createConfig());

		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		OutputDirectoryHierarchy outputDirectoryHierarchy = new OutputDirectoryHierarchy(config);
		outputDirectoryHierarchy.createIterationDirectory(0);
		outputDirectoryHierarchy.createIterationDirectory(1);

		ParkingOccupancyObserver parkingOccupancyObserver = new ParkingOccupancyObserver(network, parkingCapacityInitializer, config, outputDirectoryHierarchy);
		parkingOccupancyObserver.notifyBeforeMobsim(new BeforeMobsimEvent(null, 0, false));
		return parkingOccupancyObserver;
	}

	private void checkFile() {
		checkFile(0);
	}

	private void checkFile(int iteration) {
		BufferedReader bufferedReader = IOUtils.getBufferedReader(utils.getOutputDirectory() + "ITERS/it." + iteration + "/" + iteration + ".parking_initial_occupancy.csv");
		try {
			assertEquals("linkId;capacity;occupancy", bufferedReader.readLine());
			assertEquals("l;2;0", bufferedReader.readLine());
			assertEquals(null, bufferedReader.readLine());
		} catch (IOException e) {
			Assertions.fail();
		}
	}
}
