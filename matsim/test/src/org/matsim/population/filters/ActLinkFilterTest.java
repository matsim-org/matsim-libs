/* *********************************************************************** *
 * project: org.matsim.*
 * ActLinkFilterTest.java
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

package org.matsim.population.filters;

import java.util.TreeMap;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class ActLinkFilterTest extends MatsimTestCase {

	/**
	 * @author komeiste
	 * @throws Exception
	 */
	public void testActLinkFilter() throws Exception {

		final String GENERAL_HOME_ACT_TYPE = "h";
		final String NINETEEN_HOUR_HOME_ACT_TYPE = "h19";

		// fixture: 2 links and 2 persons
		NetworkLayer network = new NetworkLayer();
		NodeImpl node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(100.0, 100.0));
		NodeImpl node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(200.0, 200.0));
		NodeImpl node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(300.0, 300.0));

		network.createAndAddLink(new IdImpl("1"), node1, node2, 1000.0, 20, 200, 2);
		network.createAndAddLink(new IdImpl("2"), node2, node3, 1000.0, 20, 200, 2);

		TreeMap<String, PersonImpl> persons = new TreeMap<String, PersonImpl>();

		String linkId = null;
		String actType = null;
		for (String personId : new String[]{"1", "2"}) {
			PersonImpl person = new PersonImpl(new IdImpl(personId));
			PlanImpl plan = person.createAndAddPlan(true);
			if (personId.equals("1")) {
				linkId = "1";
				actType = GENERAL_HOME_ACT_TYPE;
			} else if (personId.equals("2")) {
				linkId = "2";
				actType = NINETEEN_HOUR_HOME_ACT_TYPE;
			}
			plan.createAndAddActivity(actType, network.getLink(linkId));
			persons.put(personId, person);
		}


		ActLinkFilter allHomeFilter = new ActLinkFilter(".*" + GENERAL_HOME_ACT_TYPE + ".*", null);
		allHomeFilter.addLink(new IdImpl("1"));
		assertTrue(allHomeFilter.judge(persons.get("1").getPlans().get(0)));
		assertFalse(allHomeFilter.judge(persons.get("2").getPlans().get(0)));
		allHomeFilter.addLink(new IdImpl("2"));
		assertTrue(allHomeFilter.judge(persons.get("1").getPlans().get(0)));
		assertTrue(allHomeFilter.judge(persons.get("2").getPlans().get(0)));

		ActLinkFilter home19Filter = new ActLinkFilter(NINETEEN_HOUR_HOME_ACT_TYPE, null);
		home19Filter.addLink(new IdImpl("1"));
		assertFalse(home19Filter.judge(persons.get("1").getPlans().get(0)));
		assertFalse(home19Filter.judge(persons.get("2").getPlans().get(0)));
		home19Filter.addLink(new IdImpl("2"));
		System.out.println();
		assertFalse(home19Filter.judge(persons.get("1").getPlans().get(0)));
		assertTrue(home19Filter.judge(persons.get("2").getPlans().get(0)));

	}

}
