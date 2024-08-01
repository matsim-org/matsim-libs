package org.matsim.contrib.drt.analysis.zonal;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystem;
import org.matsim.core.network.NetworkUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class DrtGridUtilsTest {

	@Test
	void test() {
		Network network = createNetwork();
		SquareGridZoneSystem squareGridZoneSystem = new SquareGridZoneSystem(network, 100, false, z -> true);

		assertThat(squareGridZoneSystem.getZones()).hasSize(100);

		for (int col = 0; col < 10; col++) {
			for (int row = 0; row < 10; row++) {
				Optional<Zone> zoneForCoord = squareGridZoneSystem.getZoneForCoord(new Coord(col * 100, row * 100));

				assertThat(zoneForCoord).isPresent();
				assertThat(zoneForCoord.get().getPreparedGeometry().getGeometry().getCoordinates()).containsExactly(//
						new Coordinate(col * 100, row * 100),//
						new Coordinate(col * 100 + 100, row * 100),//
						new Coordinate(col * 100 + 100, row * 100 + 100),//
						new Coordinate(col * 100, row * 100 + 100),//
						new Coordinate(col * 100, row * 100));
				assertThat(zoneForCoord.get().getPreparedGeometry().getGeometry().getCentroid().getCoordinate()).isEqualTo(
						new Coordinate(col * 100 + 50, row * 100 + 50));
			}
		}
	}

	static Network createNetwork() {
		Network network = NetworkUtils.createNetwork();
		Node a = network.getFactory().createNode(Id.createNodeId("a"), new Coord(0, 0));
		Node b = network.getFactory().createNode(Id.createNodeId("b"), new Coord(0, 1000));
		Node c = network.getFactory().createNode(Id.createNodeId("c"), new Coord(1000, 1000));
		Node d = network.getFactory().createNode(Id.createNodeId("d"), new Coord(1000, 0));
		network.addNode(a);
		network.addNode(b);
		network.addNode(c);
		network.addNode(d);

		Link ab = network.getFactory().createLink(Id.createLinkId("ab"), a, b);
		Link bc = network.getFactory().createLink(Id.createLinkId("bc"), b, c);
		Link cd = network.getFactory().createLink(Id.createLinkId("cd"), c, d);
		Link da = network.getFactory().createLink(Id.createLinkId("da"), d, a);
		network.addLink(ab);
		network.addLink(bc);
		network.addLink(cd);
		network.addLink(da);
		return network;
	}
}
