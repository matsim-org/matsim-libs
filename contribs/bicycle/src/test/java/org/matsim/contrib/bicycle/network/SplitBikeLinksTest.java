package org.matsim.contrib.bicycle.network;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.network.NetworkUtils;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link SplitBikeLinks#process} on hand-built links. The split is pure
 * structure — every qualifying category is treated identically, no attribute encodes
 * whether motorized interaction applies; that decision belongs to the scoring side.
 */
class SplitBikeLinksTest {

	private static final Set<String> CAR_AND_BIKE = Set.of(TransportMode.car, TransportMode.bike);

	@Test
	void splitsALaneTaggedRoadIntoAPairTiedByAttributes() {

		Network net = NetworkUtils.createNetwork();
		Link road = link(net, "r1", CAR_AND_BIKE, "CYCLEWAY_ON_HIGHWAY_ADVISORY");
		road.getAttributes().putAttribute("surface", "asphalt");
		road.getAttributes().putAttribute(BicycleUtils.GRADIENT, 0.02);
		road.getAttributes().putAttribute("restricted_lanes", 1);
		road.getAttributes().putAttribute("speed_factor", 0.83);

		Map<Id<Link>, Id<Link>> pairs = SplitBikeLinks.process(net, TransportMode.bike, 5.56, 1500);

		assertEquals(1, pairs.size());
		Link bike = net.getLinks().get(Id.createLinkId("r1_bike"));

		// geometry and modes
		assertEquals(road.getFromNode(), bike.getFromNode());
		assertEquals(road.getToNode(), bike.getToNode());
		assertEquals(road.getLength(), bike.getLength(), 1e-9, "interaction time windows need equal length");
		assertEquals(Set.of(TransportMode.bike), bike.getAllowedModes());
		assertEquals(Set.of(TransportMode.car), road.getAllowedModes(), "the road must lose bike");
		assertEquals(5.56, bike.getFreespeed(), 1e-9);
		assertEquals(1500, bike.getCapacity(), 1e-9);

		// the pair reference, both ways, via the attributes - not the id convention
		assertEquals("r1", BicycleUtils.getCarLink(bike));
		assertEquals("r1_bike", BicycleUtils.getBikeLink(road));

		// the category moves, scoring inputs are copied, car bookkeeping stays behind
		assertEquals("CYCLEWAY_ON_HIGHWAY_ADVISORY", BicycleUtils.getBicycleInfra(bike));
		assertNull(BicycleUtils.getBicycleInfra(road), "the category must not be counted twice");
		assertEquals("asphalt", bike.getAttributes().getAttribute("surface"));
		assertEquals("asphalt", road.getAttributes().getAttribute("surface"), "shared facts stay on both");
		assertEquals(0.02, BicycleUtils.getGradient(bike));
		assertNull(bike.getAttributes().getAttribute("restricted_lanes"));
		assertNull(bike.getAttributes().getAttribute("speed_factor"));
	}

	/** Protected lanes and centerline-tagged tracks split exactly like on-carriageway lanes. */
	@Test
	void splitsProtectedAndCenterlineTrackTheSameWay() {

		Network net = NetworkUtils.createNetwork();
		link(net, "p", CAR_AND_BIKE, "CYCLEWAY_ON_HIGHWAY_PROTECTED");
		link(net, "t", CAR_AND_BIKE, "CYCLEWAY_ADJOINING");

		Map<Id<Link>, Id<Link>> pairs = SplitBikeLinks.process(net, TransportMode.bike, 5.56, 1500);

		assertEquals(2, pairs.size());
		Link protectedBike = net.getLinks().get(Id.createLinkId("p_bike"));
		Link trackBike = net.getLinks().get(Id.createLinkId("t_bike"));
		assertEquals("p", BicycleUtils.getCarLink(protectedBike),
			"structure is recorded for protected infra too - whether interaction counts is not the network's call");
		assertEquals("t", BicycleUtils.getCarLink(trackBike));
	}

	@Test
	void leavesMixedTrafficBikeOnlyAndBicycleRoadsAlone() {

		Network net = NetworkUtils.createNetwork();
		link(net, "sharrow", CAR_AND_BIKE, "SHARED_MOTOR_VEHICLE_LANE");    // no space of its own
		link(net, "none", CAR_AND_BIKE, "NONE");                            // mixed traffic
		link(net, "unclear", CAR_AND_BIKE, "NEEDS_CLARIFICATION");          // never guess
		link(net, "bikeroad", CAR_AND_BIKE, "BICYCLE_ROAD");                // carriageway IS the infra
		link(net, "path", Set.of(TransportMode.bike), "CYCLEWAY_ISOLATED"); // no car to split from

		Map<Id<Link>, Id<Link>> pairs = SplitBikeLinks.process(net, TransportMode.bike, 5.56, 1500);

		assertTrue(pairs.isEmpty());
		assertEquals(5, net.getLinks().size(), "nothing may be added");
		assertEquals(CAR_AND_BIKE, net.getLinks().get(Id.createLinkId("sharrow")).getAllowedModes(),
			"a sharrow keeps bike in the car queue - that IS the model");
	}

	@Test
	void refusesToOverwriteAnExistingBikeSuffixId() {

		Network net = NetworkUtils.createNetwork();
		link(net, "x", CAR_AND_BIKE, "CYCLEWAY_ON_HIGHWAY_ADVISORY");
		link(net, "x_bike", Set.of(TransportMode.bike), "CYCLEWAY_ISOLATED");

		assertThrows(IllegalStateException.class,
			() -> SplitBikeLinks.process(net, TransportMode.bike, 5.56, 1500),
			"an id collision must fail loudly, never overwrite");
	}

	/** Links outside the bicycle area carry no category at all and are never split. */
	@Test
	void ignoresLinksWithoutACategory() {

		Network net = NetworkUtils.createNetwork();
		Link outside = link(net, "o", CAR_AND_BIKE, null);
		outside.getAttributes().removeAttribute(BicycleUtils.BICYCLE_INFRA);

		assertTrue(SplitBikeLinks.process(net, TransportMode.bike, 5.56, 1500).isEmpty());
	}

	// ------------------------------------------------------------------------

	private static Link link(Network net, String id, Set<String> modes, String infra) {
		Node from = node(net, id + "-a");
		Node to = node(net, id + "-b");
		Link l = NetworkUtils.createAndAddLink(net, Id.createLinkId(id), from, to, 120.0, 13.9, 1000.0, 2.0);
		l.setAllowedModes(modes);
		if (infra != null) l.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRA, infra);
		return l;
	}

	private static Node node(Network net, String id) {
		Node n = net.getFactory().createNode(Id.createNodeId(id), new Coord(net.getNodes().size() * 100.0, 0));
		net.addNode(n);
		return n;
	}
}
