/* *********************************************************************** *
 * project: org.matsim.*
 * CompressedRouteTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.population.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.LinkIdComparator;
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.SubsequentLinksAnalyzer;


/**
 * @author mrieser
 */
public class CompressedCarRouteTest extends AbstractCarRouteTest {

	@Override
	public CarRoute getCarRouteInstance(final Link fromLink, final Link toLink) {

		NetworkLayer network = (NetworkLayer) Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
		SubsequentLinksAnalyzer subsequent = new SubsequentLinksAnalyzer(network);
		return new CompressedCarRoute(fromLink, toLink, subsequent.getSubsequentLinks());
	}

	/**
	 * Tests that setting some links results in the same links returned.
	 * This is usually not tested as it is assumed, that just storing some
	 * links and returning the same ones should not be error-prone, but it's
	 * different in this case where we do not actually store the links.
	 */
	public void testGetLinks_setLinks() {
		NetworkLayer network = createTestNetwork();
		Link link1 = network.getLink(new IdImpl("1"));
		Link link22 = network.getLink(new IdImpl("22"));
		Link link12 = network.getLink(new IdImpl("12"));
		Link link13 = network.getLink(new IdImpl("13"));
		Link linkM24 = network.getLink(new IdImpl("-24"));
		Link link4 = network.getLink(new IdImpl("4"));

		List<Link> links = new ArrayList<Link>(5);
		links.add(link22);
		links.add(link12);
		links.add(link13);
		links.add(linkM24);
		CarRoute route = getCarRouteInstance(link1, link4);
		route.setLinks(link1, links, link4);

		List<Link> links2 = route.getLinks();
		assertEquals("wrong number of links.", links.size(), links2.size());
		for (int i = 0, n = links.size(); i < n; i++) {
			assertEquals("different link at position " + i, links.get(i), links2.get(i));
		}
	}

	public void testGetLinks_onlySubsequentLinks() {
		NetworkLayer network = createTestNetwork();
		Link link0 = network.getLink(new IdImpl("0"));
		Link link1 = network.getLink(new IdImpl("1"));
		Link link2 = network.getLink(new IdImpl("2"));
		Link link3 = network.getLink(new IdImpl("3"));
		Link link4 = network.getLink(new IdImpl("4"));

		List<Link> links = new ArrayList<Link>(4);
		links.add(link1);
		links.add(link2);
		links.add(link3);

		Map<Link, Link> subsequentLinks = new TreeMap<Link, Link>(new LinkIdComparator());
		subsequentLinks.put(link0, link1);
		subsequentLinks.put(link1, link2);
		subsequentLinks.put(link2, link3);
		subsequentLinks.put(link3, link4);

		CarRoute route = new CompressedCarRoute(link0, link4, subsequentLinks);
		route.setLinks(link0, links, link4);

		List<Link> links2 = route.getLinks();
		assertEquals("wrong number of links.", links.size(), links2.size());
		for (int i = 0, n = links.size(); i < n; i++) {
			assertEquals("different link at position " + i, links.get(i), links2.get(i));
		}
	}

}
