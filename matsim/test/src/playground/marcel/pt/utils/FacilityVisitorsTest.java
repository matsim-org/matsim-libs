/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityVisitorsTest.java
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

package playground.marcel.pt.utils;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

public class FacilityVisitorsTest extends MatsimTestCase {

	public void testPersonStartAct() {
		final Facilities facilities = new Facilities();
		final Facility facility = facilities.createFacility(new IdImpl("1"), new CoordImpl(0, 0));
		final NetworkLayer network = new NetworkLayer();
		final Link link = network.createLink(new IdImpl("1"), network.createNode(new IdImpl("1"), new CoordImpl(0, 0)), network.createNode(new IdImpl("2"), new CoordImpl(100, 0)), 100, 100, 3600, 1);
		final Person person = new PersonImpl(new IdImpl("1"));
		final Plan plan = person.createPlan(true);
		final Act workAct = plan.createAct("work", facility);

		final FacilityVisitors facVis = new FacilityVisitors();
		assertEquals("there should be no visitors yet.", 0, facVis.getVisitors(facility, "work").size());
		facVis.handleEvent(new ActStartEvent(7.0*3600, person, link, workAct));
		assertEquals("there should be one visitor.", 1, facVis.getVisitors(facility, "work").size());
		assertEquals(person, facVis.getVisitors(facility, "work").get(0));
	}

	public void testPersonEndAct() {
		final Facilities facilities = new Facilities();
		final Facility facility = facilities.createFacility(new IdImpl("1"), new CoordImpl(0, 0));
		final NetworkLayer network = new NetworkLayer();
		final Link link = network.createLink(new IdImpl("1"), network.createNode(new IdImpl("1"), new CoordImpl(0, 0)), network.createNode(new IdImpl("2"), new CoordImpl(100, 0)), 100, 100, 3600, 1);
		final Person person = new PersonImpl(new IdImpl("1"));
		final Plan plan = person.createPlan(true);
		final Act workAct = plan.createAct("work", facility);

		final FacilityVisitors facVis = new FacilityVisitors();
		facVis.handleEvent(new ActStartEvent(7.0*3600, person, link, workAct));
		assertEquals("there should be one visitor.", 1, facVis.getVisitors(facility, "work").size());
		assertEquals(person, facVis.getVisitors(facility, "work").get(0));

		facVis.handleEvent(new ActEndEvent(8.0*3600, person, link, workAct));
		assertEquals("there should be noone anymore.", 0, facVis.getVisitors(facility, "work").size());
	}

	public void testReset() {
		final Facilities facilities = new Facilities();
		final Facility facility = facilities.createFacility(new IdImpl("1"), new CoordImpl(0, 0));
		final NetworkLayer network = new NetworkLayer();
		final Link link = network.createLink(new IdImpl("1"), network.createNode(new IdImpl("1"), new CoordImpl(0, 0)), network.createNode(new IdImpl("2"), new CoordImpl(100, 0)), 100, 100, 3600, 1);
		final Person person = new PersonImpl(new IdImpl("1"));
		final Plan plan = person.createPlan(true);
		final Act workAct = plan.createAct("work", facility);

		final FacilityVisitors facVis = new FacilityVisitors();
		assertEquals("there should be no visitors yet.", 0, facVis.getVisitors(facility, "work").size());
		facVis.handleEvent(new ActStartEvent(7.0*3600, person, link, workAct));
		assertEquals("there should be one visitor.", 1, facVis.getVisitors(facility, "work").size());
		assertEquals(person, facVis.getVisitors(facility, "work").get(0));

		facVis.reset(1);
		assertEquals("there should be no visitors after reset().", 0, facVis.getVisitors(facility, "work").size());
	}

}
