package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEndsParkingSearch;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.network.NetworkUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParkingOccupancyObserverTest {
	@Test
	void test_initial() {
		ParkingOccupancyObserver parkingOccupancyObserver = getParkingObserver();

		Id<Link> linkId = Id.createLinkId("l");
		Map<Id<Link>, ParkingCount> parkingCount = parkingOccupancyObserver.getParkingCount(0, Map.of(linkId, 1.0));
		assertEquals(Map.of(linkId, new ParkingCount(0, 2, 1.0)), parkingCount);
	}

	@Test
	void test_unpark() {
		Id<Link> linkId = Id.createLinkId("l");

		ParkingOccupancyObserver parkingOccupancyObserver = getParkingObserver();
		parkingOccupancyObserver.handleEvent(new VehicleEntersTrafficEvent(10, Id.createPersonId("p"), linkId, Id.createVehicleId("v"), "car", 0));

		Map<Id<Link>, ParkingCount> parkingCount = parkingOccupancyObserver.getParkingCount(0, Map.of(linkId, 1.0));
		assertEquals(Map.of(linkId, new ParkingCount(0, 2, 1.0)), parkingCount);
	}

	@Test
	void test_park() {
		Id<Link> linkId = Id.createLinkId("l");

		ParkingOccupancyObserver parkingOccupancyObserver = getParkingObserver();
		parkingOccupancyObserver.handleEvent(new VehicleEndsParkingSearch(10, Id.createPersonId("p"), linkId, Id.createVehicleId("v"), "car"));

		Map<Id<Link>, ParkingCount> parkingCount = parkingOccupancyObserver.getParkingCount(0, Map.of(linkId, 1.0));
		assertEquals(Map.of(linkId, new ParkingCount(1, 2, 1.0)), parkingCount);
	}

	@Test
	void test_parkUnpark() {
		Id<Link> linkId = Id.createLinkId("l");

		ParkingOccupancyObserver parkingOccupancyObserver = getParkingObserver();
		parkingOccupancyObserver.handleEvent(new VehicleEndsParkingSearch(10, Id.createPersonId("p"), linkId, Id.createVehicleId("v"), "car"));
		parkingOccupancyObserver.handleEvent(new VehicleEntersTrafficEvent(20, Id.createPersonId("p"), linkId, Id.createVehicleId("v"), "car", 0));

		Map<Id<Link>, ParkingCount> parkingCount = parkingOccupancyObserver.getParkingCount(0, Map.of(linkId, 1.0));
		assertEquals(Map.of(linkId, new ParkingCount(0, 2, 1.0)), parkingCount);
	}

	@Test
	void test_reset() {
		Id<Link> linkId = Id.createLinkId("l");

		ParkingOccupancyObserver parkingOccupancyObserver = getParkingObserver();
		parkingOccupancyObserver.handleEvent(new VehicleEndsParkingSearch(10, Id.createPersonId("p"), linkId, Id.createVehicleId("v"), "car"));
		parkingOccupancyObserver.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent(null, 10));

		{
			Map<Id<Link>, ParkingCount> parkingCount = parkingOccupancyObserver.getParkingCount(0, Map.of(linkId, 1.0));
			assertEquals(Map.of(linkId, new ParkingCount(1, 2, 1.0)), parkingCount);
		}

		parkingOccupancyObserver.notifyBeforeMobsim(null);

		{
			Map<Id<Link>, ParkingCount> parkingCount = parkingOccupancyObserver.getParkingCount(0, Map.of(linkId, 1.0));
			assertEquals(Map.of(linkId, new ParkingCount(0, 2, 1.0)), parkingCount);
		}
	}

	private static ParkingOccupancyObserver getParkingObserver() {
		Network network = NetworkUtils.createNetwork();

		Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0, 0));
		Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0, 100));

		Id<Link> linkId = Id.createLinkId("l");
		Link l = NetworkUtils.createLink(linkId, node0, node1, network, 100, 10, 10, 1);
		l.getAttributes().putAttribute("onstreet_spots", 2);

		network.addNode(node0);
		network.addNode(node1);
		network.addLink(l);

		ParkingCapacityInitializer parkingCapacityInitializer = new ZeroParkingCapacityInitializer(network);

		ParkingOccupancyObserver parkingOccupancyObserver = new ParkingOccupancyObserver(network, parkingCapacityInitializer);
		parkingOccupancyObserver.notifyBeforeMobsim(null);
		return parkingOccupancyObserver;
	}
}
