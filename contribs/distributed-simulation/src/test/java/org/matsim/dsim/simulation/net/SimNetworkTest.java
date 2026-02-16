package org.matsim.dsim.simulation.net;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.network.NetworkPartitioning;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.dsim.NetworkDecomposition;
import org.matsim.examples.ExamplesUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.matsim.dsim.NetworkDecomposition.PARTITION_ATTR_KEY;
import static org.mockito.Mockito.mock;

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

		NetworkPartitioning partitioning = new NetworkPartitioning(ComputeNode.SINGLE_INSTANCE, network);

		var config = ConfigUtils.createConfig();
		var simNetwork = new SimNetwork(network, config, partitioning.getPartition(part),
			mock(ActiveLinks.class), mock(ActiveNodes.class));

		assertInstanceOf(SimLink.SplitInLink.class, simNetwork.getLinks().get(l1.getId()));
		assertInstanceOf(SimLink.LocalLink.class, simNetwork.getLinks().get(l2.getId()));
		assertInstanceOf(SimLink.SplitOutLink.class, simNetwork.getLinks().get(l3.getId()));
	}

	@Test
	void orderIndependentFromPartitioning() {

		var scenario = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("kelheim"), "config.xml");
		var config = ConfigUtils.loadConfig(scenario);
		var netPath = IOUtils.extendUrl(config.getContext(), config.network().getInputFile());

		var network = NetworkUtils.readNetwork(netPath.toString());
		NetworkDecomposition.scattered(network, 2);

		var localNetwork = new SimNetwork(network, config, NetworkPartition.SINGLE_INSTANCE, mock(ActiveLinks.class), mock(ActiveNodes.class));
		var simulationNode = ComputeNode.builder()
			.rank(0)
			.parts(new IntArrayList(new int[]{0, 1}))
			.cores(2)
			.build();
		var netPart = new NetworkPartitioning(simulationNode, network);
		var distNetwork = new SimNetwork(network, config, netPart.getPartition(0), mock(ActiveLinks.class), mock(ActiveNodes.class));

		assertNotEquals(localNetwork.getLinks().size(), distNetwork.getLinks().size());
		assertNotEquals(localNetwork.getNodes().size(), distNetwork.getNodes().size());

		for (var distNode : distNetwork.getNodes().values()) {
			var localNode = localNetwork.getNodes().get(distNode.getId());
			var localInIt = localNode.getInLinks().iterator();
			for (var distInLink : distNode.getInLinks()) {
				var localInLink = localInIt.next();
				assertEquals(distInLink.getId(), localInLink.getId());
			}
			assertFalse(localInIt.hasNext());

			var localOutIt = localNode.getOutLinks().values().iterator();
			for (var distOutLink : distNode.getOutLinks().values()) {
				var localOutLink = localOutIt.next();
				assertEquals(distOutLink.getId(), localOutLink.getId());
			}
			assertFalse(localOutIt.hasNext());
		}
	}
}
