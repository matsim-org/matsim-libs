package org.matsim.contrib.taxi.rides.util;

import org.locationtech.jts.util.AssertionFailedException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.Time;

import java.util.List;
import java.util.Objects;

public class Utils {
	private static List<Link> generateNetwork(Network network) {
		network.setCapacityPeriod(Time.parseTime("1:00:00"));
		var node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
		var node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(100, 0));
		var node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(850, 0));
		var node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(1600, 0));
		var node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord(1700, 0));
		var node6 = NetworkUtils.createAndAddNode(network, Id.create("6", Node.class), new Coord(1800, 0));

		var link1 = NetworkUtils.createAndAddLink(network, Id.create("1", Link.class), node1, node2, 105, 100, 3600, 1);
		var link2 = NetworkUtils.createAndAddLink(network, Id.create("2", Link.class), node2, node3, 750, 100, 3600, 1);
		var link3 = NetworkUtils.createAndAddLink(network, Id.create("3", Link.class), node3, node4, 750, 100, 3600, 1);
		var link4 = NetworkUtils.createAndAddLink(network, Id.create("4", Link.class), node4, node5, 105, 100, 360, 1);
		var link5 = NetworkUtils.createAndAddLink(network, Id.create("5", Link.class), node5, node6, 105, 100, 3600, 1);

		return List.of(link1, link2, link3, link4, link5);
	}

	public static void expectEvents(List<Event> actual, List<PartialEvent> expected) {
		if (expected.isEmpty()) {
			return;
		}
		var expectedIt = expected.iterator();
		PartialEvent partialEv = expectedIt.next();
		for (Event actualEv : actual) {
			if (Objects.equals(actualEv.getEventType(), partialEv.type)) {
				if (!partialEv.matches(actualEv)) {
					throw new AssertionFailedException("Expected: " + partialEv + ", actual: " + actualEv);
				}
				if (!expectedIt.hasNext()) {
					return;
				}
				partialEv = expectedIt.next();
			}
		}
	}
}
