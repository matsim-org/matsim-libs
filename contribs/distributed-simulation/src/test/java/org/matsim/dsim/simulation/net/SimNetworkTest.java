package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.matsim.dsim.NetworkDecomposition.PARTITION_ATTR_KEY;

class SimNetworkTest {

	@Test
	public void initSimNetwork() {

		var part = 1;
		var network = NetworkUtils.createNetwork();
		var n1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(0, 0));
		n1.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
		var n2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(100, 0));
		n2.getAttributes().putAttribute(PARTITION_ATTR_KEY, part);
		var n3 = network.getFactory().createNode(Id.createNodeId("n3"), new Coord(1100, 0));
		n3.getAttributes().putAttribute(PARTITION_ATTR_KEY, part);
		var n4 = network.getFactory().createNode(Id.createNodeId("n4"), new Coord(1200, 0));
		n4.getAttributes().putAttribute(PARTITION_ATTR_KEY, 2);
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		network.addNode(n4);

		var l1 = network.getFactory().createLink(Id.createLinkId("l1"), n1, n2);
		var l2 = network.getFactory().createLink(Id.createLinkId("l2"), n2, n3);
		var l3 = network.getFactory().createLink(Id.createLinkId("l3"), n3, n4);
		network.addLink(l1);
		network.addLink(l2);
		network.addLink(l3);

		// TODO, do we want to pass that handler here?
		var simNetwork = new SimNetwork(network, ConfigUtils.createConfig(), part, SimLink.OnLeaveQueue.defaultHandler(), _ -> {}, _ -> {});

		assertInstanceOf(SimLink.SplitInLink.class, simNetwork.getLinks().get(l1.getId()));
		assertInstanceOf(SimLink.LocalLink.class, simNetwork.getLinks().get(l2.getId()));
		assertInstanceOf(SimLink.SplitOutLink.class, simNetwork.getLinks().get(l3.getId()));
	}
}
