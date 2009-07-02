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

package org.matsim.core.population.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.LinkIdComparator;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.algorithms.SubsequentLinksAnalyzer;

/**
 * @author mrieser
 */
public class CompressedNetworkRouteTest extends AbstractNetworkRouteTest {

	@Override
	public NetworkRoute getCarRouteInstance(final LinkImpl fromLink, final LinkImpl toLink, NetworkLayer network) {
		SubsequentLinksAnalyzer subsequent = new SubsequentLinksAnalyzer(network);
		return new CompressedNetworkRoute(fromLink, toLink, subsequent.getSubsequentLinks());
	}

	/**
	 * Tests that setting some links results in the same links returned.
	 * This is usually not tested as it is assumed, that just storing some
	 * links and returning the same ones should not be error-prone, but it's
	 * different in this case where we do not actually store the links.
	 */
	public void testGetLinks_setLinks() {
		NetworkLayer network = createTestNetwork();
		LinkImpl link1 = network.getLink(new IdImpl("1"));
		LinkImpl link22 = network.getLink(new IdImpl("22"));
		LinkImpl link12 = network.getLink(new IdImpl("12"));
		LinkImpl link13 = network.getLink(new IdImpl("13"));
		LinkImpl linkM24 = network.getLink(new IdImpl("-24"));
		LinkImpl link4 = network.getLink(new IdImpl("4"));

		List<LinkImpl> links = new ArrayList<LinkImpl>(5);
		links.add(link22);
		links.add(link12);
		links.add(link13);
		links.add(linkM24);
		NetworkRoute route = getCarRouteInstance(link1, link4, network);
		route.setLinks(link1, links, link4);

		List<LinkImpl> links2 = route.getLinks();
		assertEquals("wrong number of links.", links.size(), links2.size());
		for (int i = 0, n = links.size(); i < n; i++) {
			assertEquals("different link at position " + i, links.get(i), links2.get(i));
		}
	}

	public void testGetLinks_onlySubsequentLinks() {
		NetworkLayer network = createTestNetwork();
		LinkImpl link0 = network.getLink(new IdImpl("0"));
		LinkImpl link1 = network.getLink(new IdImpl("1"));
		LinkImpl link2 = network.getLink(new IdImpl("2"));
		LinkImpl link3 = network.getLink(new IdImpl("3"));
		LinkImpl link4 = network.getLink(new IdImpl("4"));

		List<LinkImpl> links = new ArrayList<LinkImpl>(4);
		links.add(link1);
		links.add(link2);
		links.add(link3);

		Map<LinkImpl, LinkImpl> subsequentLinks = new TreeMap<LinkImpl, LinkImpl>(new LinkIdComparator());
		subsequentLinks.put(link0, link1);
		subsequentLinks.put(link1, link2);
		subsequentLinks.put(link2, link3);
		subsequentLinks.put(link3, link4);

		NetworkRoute route = new CompressedNetworkRoute(link0, link4, subsequentLinks);
		route.setLinks(link0, links, link4);

		List<LinkImpl> links2 = route.getLinks();
		assertEquals("wrong number of links.", links.size(), links2.size());
		for (int i = 0, n = links.size(); i < n; i++) {
			assertEquals("different link at position " + i, links.get(i), links2.get(i));
		}
	}

	/**
	 * Tests that {@link CompressedNetworkRoute#getLinks()} doesn't crash or
	 * hang when a route object is not correctly initialized.  
	 */
	public void testGetLinks_incompleteInitialization() {
		NetworkLayer network = createTestNetwork();
		LinkImpl link0 = network.getLink(new IdImpl("0"));
		LinkImpl link1 = network.getLink(new IdImpl("1"));
		LinkImpl link2 = network.getLink(new IdImpl("2"));
		LinkImpl link3 = network.getLink(new IdImpl("3"));
		LinkImpl link4 = network.getLink(new IdImpl("4"));

		Map<LinkImpl, LinkImpl> subsequentLinks = new TreeMap<LinkImpl, LinkImpl>(new LinkIdComparator());
		subsequentLinks.put(link0, link1);
		subsequentLinks.put(link1, link2);
		subsequentLinks.put(link2, link3);
		subsequentLinks.put(link3, link4);

		NetworkRoute route = new CompressedNetworkRoute(link0, link4, subsequentLinks);
		// NO route.setLinks() here!

		assertEquals("expected 0 links.", 0, route.getLinks().size());
		assertEquals("expected 0 link ids.", 0, route.getLinkIds().size());
	}

	/**
	 * Tests that {@link CompressedNetworkRoute#getNodes()} doesn't crash or
	 * hang when a route object is not correctly initialized.  
	 */
	public void testGetNodes_incompleteInitialization() {
		NetworkLayer network = createTestNetwork();
		LinkImpl link0 = network.getLink(new IdImpl("0"));
		LinkImpl link1 = network.getLink(new IdImpl("1"));
		LinkImpl link2 = network.getLink(new IdImpl("2"));
		LinkImpl link3 = network.getLink(new IdImpl("3"));
		LinkImpl link4 = network.getLink(new IdImpl("4"));
		
		Map<LinkImpl, LinkImpl> subsequentLinks = new TreeMap<LinkImpl, LinkImpl>(new LinkIdComparator());
		subsequentLinks.put(link0, link1);
		subsequentLinks.put(link1, link2);
		subsequentLinks.put(link2, link3);
		subsequentLinks.put(link3, link4);
		
		NetworkRoute route = new CompressedNetworkRoute(link0, link4, subsequentLinks);
		// NO route.setLinks() here!
		
		assertEquals("expected 0 links.", 0, route.getNodes().size());
	}
	
}
