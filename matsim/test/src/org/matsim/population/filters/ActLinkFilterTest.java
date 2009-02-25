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

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.PersonImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

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
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(100.0, 100.0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(200.0, 200.0));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(300.0, 300.0));

		network.createLink(new IdImpl("1"), node1, node2, 1000.0, 20, 200, 2);
		network.createLink(new IdImpl("2"), node2, node3, 1000.0, 20, 200, 2);

		TreeMap<String, Person> persons = new TreeMap<String, Person>();

		String linkId = null;
		String actType = null;
		for (String personId : new String[]{"1", "2"}) {
			Person person = new PersonImpl(new IdImpl(personId));
			Plan plan = person.createPlan(true);
			if (personId.equals("1")) {
				linkId = "1";
				actType = GENERAL_HOME_ACT_TYPE;
			} else if (personId.equals("2")) {
				linkId = "2";
				actType = NINETEEN_HOUR_HOME_ACT_TYPE;
			}
			plan.createAct(actType, network.getLink(linkId));
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
