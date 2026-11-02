package org.matsim.contrib.msp.traveltimematrix;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.core.network.NetworkUtils;

import java.util.Set;

/**
 * The networks the tests of this package are built on. They are deliberately tiny and their travel times
 * are round numbers, so that an expected value can be written down by hand rather than copied from a
 * previous run.
 */
final class TestNetworks {

	/**
	 * The node spacing of {@link #corridor()} [m]. Also the cell size that puts every node of it into a
	 * zone of its own.
	 */
	static final double SPACING = 1000;

	private TestNetworks() {
	}

	/**
	 * A corridor of four nodes in a row, 1 km apart, where the middle section is a motorway and the two
	 * ends are residential streets:
	 * <pre>
	 *   n1 --residential-- n2 --motorway-- n3 --residential-- n4
	 *      1000 m, 10 m/s     1000 m, 50 m/s   1000 m, 10 m/s
	 * </pre>
	 * Every link exists in both directions, so the network is strongly connected and survives cleaning.
	 * At a 1 s time step, {@code QSimFreeSpeedTravelTime} gives a residential link 101 s and the
	 * motorway link 21 s, so n1 to n4 takes 223 s of which 101 + 101 = 202 s are residential: the road
	 * class shares of that pair are 202/223 residential and 21/223 motorway.
	 */
	static Network corridor() {

		Network network = NetworkUtils.createNetwork();

		Node[] nodes = new Node[4];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = NetworkUtils.createAndAddNode(network, Id.createNodeId("n" + (i + 1)),
				new Coord(i * SPACING, 0));
		}

		link(network, "1-2", nodes[0], nodes[1], 10, "residential");
		link(network, "2-1", nodes[1], nodes[0], 10, "residential");
		link(network, "2-3", nodes[1], nodes[2], 50, "motorway");
		link(network, "3-2", nodes[2], nodes[1], 50, "motorway");
		link(network, "3-4", nodes[2], nodes[3], 10, "residential");
		link(network, "4-3", nodes[3], nodes[2], 10, "residential");

		return network;
	}

	/**
	 * A link of 1 km, open to car, with its OSM highway type set.
	 */
	static Link link(Network network, String id, Node from, Node to, double freeSpeed, String highwayType) {
		Link link = NetworkUtils.createAndAddLink(network, Id.createLinkId(id), from, to, SPACING, freeSpeed, 1800, 1);
		link.setAllowedModes(Set.of(TransportMode.car));
		NetworkUtils.setType(link, highwayType);
		return link;
	}

	/**
	 * A square grid zone system over the network.
	 */
	static ZoneSystem zoneSystem(Network network, double cellSize) {
		SquareGridZoneSystemParams params = new SquareGridZoneSystemParams();
		params.setCellSize(cellSize);
		return ZoneSystemUtils.createZoneSystem(network, params);
	}

	/**
	 * A zone system that puts <b>every node in a zone of its own</b>, which is what a test that controls
	 * a single origin-destination pair needs: the matrix and the shares are addressed per zone, and the
	 * path they describe runs between the <em>central nodes</em> of the two zones, so a zone holding two
	 * nodes silently answers a query about a different pair.
	 * <p>
	 * It checks that it managed to, rather than trusting a cell size to be small enough - where the grid
	 * puts its cell boundaries is not this test's business, and a grid that pairs two nodes up would make
	 * a test fail with a plausible wrong number instead of saying what went wrong.
	 */
	static ZoneSystem oneZonePerNode(Network network) {
		ZoneSystem zoneSystem = zoneSystem(network, SPACING / 2.5);
		if (zoneSystem.getZones().size() != network.getNodes().size()) {
			throw new AssertionError("Expected one zone per node, but " + network.getNodes().size() + " nodes ended "
				+ "up in " + zoneSystem.getZones().size() + " zones. Pick a smaller cell size.");
		}
		return zoneSystem;
	}
}
