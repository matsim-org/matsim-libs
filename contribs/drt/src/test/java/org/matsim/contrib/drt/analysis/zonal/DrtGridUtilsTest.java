package org.matsim.contrib.drt.analysis.zonal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public class DrtGridUtilsTest {

	@Test
	void test() {
		Network network = createNetwork();
		Map<String, PreparedGeometry> grid = DrtGridUtils.createGridFromNetwork(network, 100);

		assertThat(grid).hasSize(100);

		int cell = 1;
		for (int col = 0; col < 10; col++) {
			for (int row = 0; row < 10; row++) {
				Geometry geometry = grid.get(cell + "").getGeometry();

				assertThat(geometry.getCoordinates()).containsExactly(//
						new Coordinate(col * 100, row * 100),//
						new Coordinate(col * 100 + 100, row * 100),//
						new Coordinate(col * 100 + 100, row * 100 + 100),//
						new Coordinate(col * 100, row * 100 + 100),//
						new Coordinate(col * 100, row * 100));
				assertThat(geometry.getCentroid().getCoordinate()).isEqualTo(
						new Coordinate(col * 100 + 50, row * 100 + 50));

				cell++;
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
