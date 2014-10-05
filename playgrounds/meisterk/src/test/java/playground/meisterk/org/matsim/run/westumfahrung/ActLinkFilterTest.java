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

package playground.meisterk.org.matsim.run.westumfahrung;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
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
		NetworkImpl network = NetworkImpl.createNetwork();
		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new CoordImpl(100.0, 100.0));
		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new CoordImpl(200.0, 200.0));
		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new CoordImpl(300.0, 300.0));

		network.createAndAddLink(Id.create("1", Link.class), node1, node2, 1000.0, 20, 200, 2);
		network.createAndAddLink(Id.create("2", Link.class), node2, node3, 1000.0, 20, 200, 2);

		TreeMap<String, PersonImpl> persons = new TreeMap<String, PersonImpl>();

		String linkId = null;
		String actType = null;
		for (String personId : new String[]{"1", "2"}) {
			PersonImpl person = new PersonImpl(Id.create(personId, Person.class));
			PlanImpl plan = person.createAndAddPlan(true);
			if (personId.equals("1")) {
				linkId = "1";
				actType = GENERAL_HOME_ACT_TYPE;
			} else if (personId.equals("2")) {
				linkId = "2";
				actType = NINETEEN_HOUR_HOME_ACT_TYPE;
			}
			plan.createAndAddActivity(actType, Id.create(linkId, Link.class));
			persons.put(personId, person);
		}


		ActLinkFilter allHomeFilter = new ActLinkFilter(".*" + GENERAL_HOME_ACT_TYPE + ".*", null);
		allHomeFilter.addLink(Id.create("1", Link.class));
		assertTrue(allHomeFilter.judge(persons.get("1").getPlans().get(0)));
		assertFalse(allHomeFilter.judge(persons.get("2").getPlans().get(0)));
		allHomeFilter.addLink(Id.create("2", Link.class));
		assertTrue(allHomeFilter.judge(persons.get("1").getPlans().get(0)));
		assertTrue(allHomeFilter.judge(persons.get("2").getPlans().get(0)));

		ActLinkFilter home19Filter = new ActLinkFilter(NINETEEN_HOUR_HOME_ACT_TYPE, null);
		home19Filter.addLink(Id.create("1", Link.class));
		assertFalse(home19Filter.judge(persons.get("1").getPlans().get(0)));
		assertFalse(home19Filter.judge(persons.get("2").getPlans().get(0)));
		home19Filter.addLink(Id.create("2", Link.class));
		System.out.println();
		assertFalse(home19Filter.judge(persons.get("1").getPlans().get(0)));
		assertTrue(home19Filter.judge(persons.get("2").getPlans().get(0)));

	}

}
