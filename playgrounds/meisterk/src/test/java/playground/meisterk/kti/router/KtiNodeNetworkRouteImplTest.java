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

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.AbstractNetworkRouteTest;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.testcases.MatsimTestCase;

public class KtiNodeNetworkRouteImplTest extends AbstractNetworkRouteTest {

	private Config config = null;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.config = super.loadConfig(null);
	}

	@Override
	protected void tearDown() throws Exception {
		this.config = null;
		super.tearDown();
	}

	@Override
	protected NetworkRouteWRefs getNetworkRouteInstance(Link fromLink,
			Link toLink, NetworkLayer network) {
		return new KtiNodeNetworkRouteImpl(fromLink, toLink, this.config.planomat().getSimLegInterpretation());
	}
	
	@Override
	public void testGetDist() {
		
		HashMap<PlanomatConfigGroup.SimLegInterpretation, Double> expectedDistances = new HashMap<PlanomatConfigGroup.SimLegInterpretation, Double>();
		expectedDistances.put(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible, 5000.0);
		expectedDistances.put(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible, 6000.0);
		
		NetworkLayer network = createTestNetwork();
		Link link1 = network.getLinks().get(new IdImpl("1"));
		Link link4 = network.getLinks().get(new IdImpl("4"));
		link4.setLength(2000.0);

		for (PlanomatConfigGroup.SimLegInterpretation simLegInterpretation : expectedDistances.keySet()) {
			
			this.config.planomat().setSimLegInterpretation(simLegInterpretation);
			NetworkRouteWRefs route = getNetworkRouteInstance(link1, link4, network);
			route.setNodes(link1, NetworkUtils.getNodes(network, "2 12 13 3 4"), link4);

			assertEquals(
					"different distance calculated.", 
					expectedDistances.get(simLegInterpretation), 
					route.getDistance(), 
					MatsimTestCase.EPSILON);
			
		}

		expectedDistances.clear();
		expectedDistances.put(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible, 0.0);
		expectedDistances.put(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible, 0.0);
		
		for (PlanomatConfigGroup.SimLegInterpretation simLegInterpretation : expectedDistances.keySet()) {
			
			this.config.planomat().setSimLegInterpretation(simLegInterpretation);
			NetworkRouteWRefs route = getNetworkRouteInstance(link1, link1, network);
			route.setNodes(link1, NetworkUtils.getNodes(network, ""), link1);

			assertEquals(
					"different distance calculated.", 
					expectedDistances.get(simLegInterpretation), 
					route.getDistance(), 
					MatsimTestCase.EPSILON);
			
		}
		
	}
}
