/* *********************************************************************** *
 * project: org.matsim.*
 * KtiNodeNetworkRouteImpl.java
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

package playground.meisterk.kti.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.AbstractNetworkRouteTest;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.testcases.MatsimTestCase;

public class KtiNodeNetworkRouteImplTest extends AbstractNetworkRouteTest {

	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected NetworkRouteWRefs getNetworkRouteInstance(Link fromLink,
			Link toLink, NetworkLayer network) {
		return new KtiNodeNetworkRouteImpl(fromLink, toLink);
	}

	@Override
	public void testGetDist() {
		NetworkLayer network = createTestNetwork();
		Link link1 = network.getLink(new IdImpl("1"));
		Link link4 = network.getLink(new IdImpl("4"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link4, network);
		route.setNodes(link1, NetworkUtils.getNodes(network, "2 12 13 3 4"), link4);

		assertEquals("different distance calculated.", 5000.0, route.getDistance(), MatsimTestCase.EPSILON);
	}
	
}
