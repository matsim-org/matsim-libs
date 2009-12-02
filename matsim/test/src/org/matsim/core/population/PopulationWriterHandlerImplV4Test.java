/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationWriterHandlerImplV4Test.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.population;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.testcases.MatsimTestCase;

public class PopulationWriterHandlerImplV4Test extends MatsimTestCase {

	public void testWriteGenericRoute() {
		super.loadConfig(null);
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile("test/scenarios/equil/network.xml");
		LinkImpl link1 = network.getLinks().get(new IdImpl(1));
		LinkImpl link2 = network.getLinks().get(new IdImpl(2));
		Gbl.createWorld().setNetworkLayer(network);
		
		PopulationImpl pop = new PopulationImpl();
		PopulationFactory pb = pop.getFactory();
		PersonImpl person = (PersonImpl) pb.createPerson(new IdImpl(1));
		PlanImpl plan = (PlanImpl) pb.createPlan();
		plan.setPerson(person);
		plan.addActivity(pb.createActivityFromLinkId("h", link1.getId()));
		LegImpl leg = (LegImpl) pb.createLeg(TransportMode.undefined);
		RouteWRefs route = new GenericRouteImpl(link1, link2);
		route.setTravelTime(123);
		route.setDistance(9876.54);
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(pb.createActivityFromLinkId("h", new IdImpl(1)));
		person.addPlan(plan);
		pop.addPerson(person);
		
		String filename = getOutputDirectory() + "population.xml";
		new PopulationWriter(pop).writeV4(filename);
		
		PopulationImpl pop2 = new PopulationImpl();
		new MatsimPopulationReader(pop2, network).readFile(filename);
		Person person2 = pop2.getPersons().get(new IdImpl(1));
		LegImpl leg2 = (LegImpl) person2.getPlans().get(0).getPlanElements().get(1);
		RouteWRefs route2 = leg2.getRoute();
		assertEquals(123, route2.getTravelTime(), EPSILON); // if this succeeds, we know that writing/reading the data works
		assertEquals(9876.54, route2.getDistance(), EPSILON);
	}
	
}
