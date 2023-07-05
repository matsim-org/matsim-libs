package org.matsim.contrib.bicycle;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;

import static org.junit.Assert.*;

public class BicycleUtilityUtilsTest {

	@Test
	public void getGradientNoFromZ() {
		var link = createLink(new Coord(0, 0), new Coord(100, 0, 100));
		assertEquals(0., BicycleUtilityUtils.getGradient(link), 0.00001);
	}

	@Test
	public void getGradientNoToZ() {
		var link = createLink(new Coord(0, 0, 100), new Coord(100, 0));
		assertEquals(0., BicycleUtilityUtils.getGradient(link), 0.00001);
	}

	@Test
	public void getGradientFlat() {
		var link = createLink(new Coord(0, 0, 100), new Coord(100, 0, 100));
		assertEquals(0., BicycleUtilityUtils.getGradient(link), 0.00001);
	}

	@Test
	public void getGradientUphill() {
		var link = createLink(new Coord(0, 0, 0), new Coord(100, 0, 100));
		assertEquals(1., BicycleUtilityUtils.getGradient(link), 0.00001);
	}

	@Test
	public void getGradientDownhill() {
		var link = createLink(new Coord(0, 0, 100), new Coord(100, 0, 0));
		assertEquals(0., BicycleUtilityUtils.getGradient(link), 0.00001);
	}

	private static Link createLink(Coord from, Coord to) {

		var net = NetworkUtils.createNetwork();
		var fromNode = net.getFactory().createNode(Id.createNodeId("from"), from);
		var toNode = net.getFactory().createNode(Id.createNodeId("to"), to);
		var link = net.getFactory().createLink(Id.createLinkId("link"), fromNode, toNode);
		link.setLength(100);
		return link;
	}
}
