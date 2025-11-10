package org.matsim.dsim;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.dsim.simulation.SimpleAgent;
import org.matsim.dsim.simulation.SimpleVehicle;
import org.matsim.dsim.simulation.net.SimLink;
import org.matsim.dsim.simulation.net.SimNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.matsim.dsim.NetworkDecomposition.PARTITION_ATTR_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class TestUtils {

	public static SimLink createLink(Link link, int part) {
		var defaultDSimConfig = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		return createLink(link, defaultDSimConfig, part);
	}

	public static SimLink createLink(Link link, DSimConfigGroup config, int part) {
		var simNode = new SimNode(link.getToNode().getId());
		return SimLink.create(link, simNode, config, 7.5, part, x -> {
		}, x -> {
		});
	}

	public static Link createSingleLink() {
		var f = NetworkUtils.createNetwork().getFactory();
		var from = f.createNode(Id.createNodeId("from"), new Coord(0, 0));
		var to = f.createNode(Id.createNodeId("to"), new Coord(100, 0));

		return NetworkUtils.createNetwork().getFactory()
			.createLink(Id.createLinkId("id"), from, to);
	}

	public static Link createSingleLink(int fromPart, int toPart) {
		var link = createSingleLink();
		link.getFromNode().getAttributes().putAttribute(PARTITION_ATTR_KEY, fromPart);
		link.getToNode().getAttributes().putAttribute(PARTITION_ATTR_KEY, toPart);
		return link;
	}

	public static Network createLocalThreeLinkNetwork() {

		var network = NetworkUtils.createNetwork();
		var n1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(0, 0));
		n1.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
		var n2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(0, 100));
		n2.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
		var n3 = network.getFactory().createNode(Id.createNodeId("n3"), new Coord(0, 1100));
		n3.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
		var n4 = network.getFactory().createNode(Id.createNodeId("n4"), new Coord(0, 1200));
		n4.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);

		var l1 = network.getFactory().createLink(Id.createLinkId("l1"), n1, n2);
		l1.setFreespeed(27.78);
		l1.setCapacity(3600);
		l1.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
		var l2 = network.getFactory().createLink(Id.createLinkId("l2"), n2, n3);
		l2.setFreespeed(27.78);
		l2.setCapacity(3600);
		l2.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
		var l3 = network.getFactory().createLink(Id.createLinkId("l3"), n3, n4);
		l3.setFreespeed(27.78);
		l3.setCapacity(3600);
		l3.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);

		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		network.addNode(n4);
		network.addLink(l1);
		network.addLink(l2);
		network.addLink(l3);

		return network;
	}

	public static Network createDistributedThreeLinkNetwork() {
		var network = TestUtils.createLocalThreeLinkNetwork();
		network.getLinks().get(Id.createLinkId("l1"))
			.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
		network.getLinks().get(Id.createLinkId("l2"))
			.getAttributes().putAttribute(PARTITION_ATTR_KEY, 1);
		network.getLinks().get(Id.createLinkId("l3"))
			.getAttributes().putAttribute(PARTITION_ATTR_KEY, 2);

		network.getNodes().get(Id.createNodeId("n1"))
			.getAttributes().putAttribute(PARTITION_ATTR_KEY, 0);
		network.getNodes().get(Id.createNodeId("n2"))
			.getAttributes().putAttribute(PARTITION_ATTR_KEY, 1);
		network.getNodes().get(Id.createNodeId("n3"))
			.getAttributes().putAttribute(PARTITION_ATTR_KEY, 1);
		network.getNodes().get(Id.createNodeId("n4"))
			.getAttributes().putAttribute(PARTITION_ATTR_KEY, 2);
		return network;
	}

	public static SimpleAgent createAgent(String id) {
		var result = new SimpleAgent();
		result.setId(Id.createPersonId(id));
		return result;
	}

	public static SimpleVehicle createVehicle() {
		return createVehicle("vehicle", 1, 50);
	}

	public static SimpleVehicle createVehicle(String id, double pce, double maxV) {
		return createVehicle(id, createAgent("driver"), pce, maxV);
	}

	public static SimpleVehicle createVehicle(String id, SimpleAgent driver, double pce, double maxV) {

		var result = new SimpleVehicle();
		result.setId(Id.createVehicleId(id));
		result.setDriver(driver);
		result.setMaximumVelocity(maxV);
		result.setSizeInEquivalents(pce);
		return result;
	}

	public static EventsManager mockExpectingEventsManager(List<Event> expectedEvents) {
		var result = mock(EventsManager.class);
		doAnswer(call -> {
			var firstArg = call.getArgument(0);
			assertInstanceOf(Event.class, firstArg);
			var event = (Event) firstArg;
			var expectedEvent = expectedEvents.removeFirst();

			assertEquals(expectedEvent, event);
			return null;
		}).when(result).processEvent(any());
		return result;
	}
}
