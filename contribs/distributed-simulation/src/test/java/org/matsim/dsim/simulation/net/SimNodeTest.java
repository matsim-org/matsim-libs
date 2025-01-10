package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.dsim.TestUtils;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimNodeTest {

	@Test
	public void init() {
		var network = TestUtils.createLocalThreeLinkNetwork();
		var simLinks = network.getLinks().values().stream()
			.map(link -> TestUtils.createLink(link, 0))
			.collect(Collectors.toMap(SimLink::getId, link -> link));
		var node = network.getNodes().get(Id.createNodeId("n3"));
		var simNode = new SimNode(node.getId());
		simNode.addInLink(simLinks.get(Id.createLinkId("l2")));
		simNode.addOutLink(simLinks.get(Id.createLinkId("l3")));

		assertEquals(node.getId(), simNode.getId());
		assertEquals(1, simNode.getInLinks().size());
		assertEquals(Id.createLinkId("l2"), simNode.getInLinks().getFirst().getId());
		assertEquals(1, simNode.getOutLinks().size());
		assertTrue(simNode.getOutLinks().containsKey(Id.createLinkId("l3")));
	}
}
