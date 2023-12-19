/* *********************************************************************** *
 * project: org.matsim.*
 * LinkRouteTest.java
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.testcases.fakes.FakeLink;

/**
 * @author mrieser
 */
public class LinkNetworkRouteTest extends AbstractNetworkRouteTest {

	@Override
	public NetworkRoute getNetworkRouteInstance(final Id<Link> fromLinkId, final Id<Link> toLinkId, final Network network) {
		return RouteUtils.createLinkNetworkRouteImpl(fromLinkId, toLinkId);
	}

	@Test
	void testClone() {
		Id<Link> id1 = Id.create(1, Link.class);
		Id<Link> id2 = Id.create(2, Link.class);
		Id<Link> id3 = Id.create(3, Link.class);
		Id<Link> id4 = Id.create(4, Link.class);
		Id<Link> id5 = Id.create(5, Link.class);
		Link startLink = new FakeLink(id1);
		Link endLink = new FakeLink(id2);
		Link link3 = new FakeLink(id3);
		Link link4 = new FakeLink(id4);
		Link link5 = new FakeLink(id5);
		NetworkRoute route1 = RouteUtils.createLinkNetworkRouteImpl(startLink.getId(), endLink.getId());
		ArrayList<Id<Link>> srcRoute = new ArrayList<Id<Link>>();
		srcRoute.add(link3.getId());
		srcRoute.add(link4.getId());
		route1.setLinkIds(startLink.getId(), srcRoute, endLink.getId());
		Assertions.assertEquals(2, route1.getLinkIds().size());

		NetworkRoute route2 = (NetworkRoute) route1.clone();

		srcRoute.add(link5.getId());
		route1.setLinkIds(startLink.getId(), srcRoute, endLink.getId());

		Assertions.assertEquals(3, route1.getLinkIds().size());
		Assertions.assertEquals(2, route2.getLinkIds().size());
	}

}
