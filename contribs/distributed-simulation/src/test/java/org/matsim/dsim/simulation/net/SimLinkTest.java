package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.dsim.TestUtils;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.matsim.dsim.NetworkDecomposition.PARTITION_ATTR_KEY;

class SimLinkTest {

	@Test
	public void createLocalLink() {

		var part = 0;
		var link = createLink(part, part);
		var simLink = TestUtils.createLink(link, part);

		assertInstanceOf(SimLink.LocalLink.class, simLink);
	}

	@Test
	public void createSplitOutLink() {

		var part = 0;
		var toPart = 1;
		var link = createLink(part, toPart);
		var simLink = TestUtils.createLink(link, part);

		assertInstanceOf(SimLink.SplitOutLink.class, simLink);
	}

	@Test
	public void createSplitInLink() {

		var part = 0;
		var fromPart = 1;
		var link = createLink(fromPart, part);
		var simLink = TestUtils.createLink(link, part);

		assertInstanceOf(SimLink.SplitInLink.class, simLink);
	}

	private Link createLink(int fromPart, int toPart) {
		var f = NetworkUtils.createNetwork().getFactory();
		var fromNode = f.createNode(Id.createNodeId("from"), new Coord(0, 0));
		fromNode.getAttributes().putAttribute(PARTITION_ATTR_KEY, fromPart);
		var toNode = f.createNode(Id.createNodeId("to"), new Coord(1000, 0));
		toNode.getAttributes().putAttribute(PARTITION_ATTR_KEY, toPart);
		return f.createLink(Id.createLinkId("local-link"), fromNode, toNode);
	}
}
