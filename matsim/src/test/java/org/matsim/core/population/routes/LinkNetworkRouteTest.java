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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.testcases.fakes.FakeLink;

/**
 * @author mrieser
 */
public class LinkNetworkRouteTest extends AbstractNetworkRouteTest {

	@Override
	public NetworkRoute getNetworkRouteInstance(final Id fromLinkId, final Id toLinkId, final NetworkLayer network) {
		return new LinkNetworkRouteImpl(fromLinkId, toLinkId, network);
	}

	@Test
	public void testClone() {
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		Id id4 = new IdImpl(4);
		Id id5 = new IdImpl(5);
		Link startLink = new FakeLink(id1);
		Link endLink = new FakeLink(id2);
		Link link3 = new FakeLink(id3);
		Link link4 = new FakeLink(id4);
		Link link5 = new FakeLink(id5);
		LinkNetworkRouteImpl route1 = new LinkNetworkRouteImpl(startLink.getId(), endLink.getId(), null);
		ArrayList<Id> srcRoute = new ArrayList<Id>();
		srcRoute.add(link3.getId());
		srcRoute.add(link4.getId());
		route1.setLinkIds(startLink.getId(), srcRoute, endLink.getId());
		Assert.assertEquals(2, route1.getLinkIds().size());

		LinkNetworkRouteImpl route2 = route1.clone();

		srcRoute.add(link5.getId());
		route1.setLinkIds(startLink.getId(), srcRoute, endLink.getId());

		Assert.assertEquals(3, route1.getLinkIds().size());
		Assert.assertEquals(2, route2.getLinkIds().size());
	}

}
