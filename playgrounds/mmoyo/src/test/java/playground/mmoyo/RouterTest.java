/* *********************************************************************** *
 * project: org.matsim.*
 * RouterTest.java
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

package playground.mmoyo;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.xml.sax.SAXException;

import playground.mmoyo.TransitSimulation.TransitRouteFinder;

public class RouterTest extends MatsimTestCase {

	public void testWithVerySimpleTransitSchedule() throws SAXException, ParserConfigurationException, IOException {
		/* for integration into MATSim, the following must work */
		
		// setup very simple scenario
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile("test/input/playground/mmoyo/pt/router/network.xml");
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = builder.createTransitSchedule();
		new TransitScheduleReaderV1(schedule, network).readFile("test/input/playground/mmoyo/pt/router/transitSchedule.xml");
		PopulationImpl population = new PopulationImpl();
		new MatsimPopulationReader(population, network).readFile("test/input/playground/mmoyo/pt/plans.xml");
		Person person = population.getPersons().get(new IdImpl(1));
		ActivityImpl fromAct = (ActivityImpl) person.getPlans().get(0).getPlanElements().get(0);
		ActivityImpl toAct = (ActivityImpl) person.getPlans().get(0).getPlanElements().get(2);
		
		// make sure our setup is as expected
		assertNotNull(person);
		assertNotNull(fromAct);
		assertNotNull(toAct);
		
		// in the plans file, there are no coordinates for the activities... fix that.
		fromAct.setCoord(new CoordImpl(5000.0, 5005.0));
		toAct.setCoord(new CoordImpl(44000.0, 24005.0));
		
		// now run the essential thing:
		TransitRouteFinder routeFinder = new TransitRouteFinder(schedule);
		List<Leg> legs = routeFinder.calculateRoute(fromAct, toAct, person);
		
		for (Leg leg : legs) {
			System.out.println("TransportMode: " + leg.getMode());
		}
		
		/* I would expect the following, although I could be wrong.
		 * It must be checked once the code runs up to this line.     */
		assertEquals(3, legs.size());  
		assertEquals(TransportMode.walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(TransportMode.walk, legs.get(2).getMode());
	}
	
}
