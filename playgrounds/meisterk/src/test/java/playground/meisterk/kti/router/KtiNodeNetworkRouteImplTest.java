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

import java.util.HashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.AbstractNetworkRouteTest;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.testcases.MatsimTestCase;

import playground.meisterk.org.matsim.config.PlanomatConfigGroup;

public class KtiNodeNetworkRouteImplTest extends AbstractNetworkRouteTest {

	private PlanomatConfigGroup config = new PlanomatConfigGroup();

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
		this.config = null;
	}

	@Override
	@Test
	public void testGetDistance() {

		HashMap<PlanomatConfigGroup.SimLegInterpretation, Double> expectedDistances = new HashMap<PlanomatConfigGroup.SimLegInterpretation, Double>();
		expectedDistances.put(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible, 5000.0);
		expectedDistances.put(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible, 6000.0);

		NetworkImpl network = createTestNetwork();
		Link link1 = network.getLinks().get(Id.create("1", Link.class));
		Link link4 = network.getLinks().get(Id.create("4", Link.class));
		link4.setLength(2000.0);

		for (PlanomatConfigGroup.SimLegInterpretation simLegInterpretation : expectedDistances.keySet()) {

			this.config.setSimLegInterpretation(simLegInterpretation);
			NetworkRoute route = getNetworkRouteInstance(link1.getId(), link4.getId(), network);
			route.setLinkIds(link1.getId(), NetworkUtils.getLinkIds("22 12 -23 3"), link4.getId());

			Assert.assertEquals(
					"different distance calculated.",
					expectedDistances.get(simLegInterpretation),
					route.getDistance(),
					MatsimTestCase.EPSILON);

		}

		expectedDistances.clear();
		expectedDistances.put(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible, 0.0);
		expectedDistances.put(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible, 0.0);

		for (PlanomatConfigGroup.SimLegInterpretation simLegInterpretation : expectedDistances.keySet()) {

			this.config.setSimLegInterpretation(simLegInterpretation);
			NetworkRoute route = getNetworkRouteInstance(link1.getId(), link1.getId(), network);
			route.setLinkIds(link1.getId(), NetworkUtils.getLinkIds(""), link1.getId());

			Assert.assertEquals(
					"different distance calculated.",
					expectedDistances.get(simLegInterpretation),
					route.getDistance(),
					MatsimTestCase.EPSILON);

		}

	}

	@Override
	protected NetworkRoute getNetworkRouteInstance(Id<Link> fromLinkId, Id<Link> toLinkId,
			NetworkImpl network) {
		return new KtiLinkNetworkRouteImpl(fromLinkId, toLinkId, network, this.config.getSimLegInterpretation());

	}
}
