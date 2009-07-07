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

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.population.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

import playground.marcel.pt.transitSchedule.TransitScheduleBuilderImpl;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;
import playground.marcel.pt.transitSchedule.api.TransitSchedule;
import playground.marcel.pt.transitSchedule.api.TransitScheduleBuilder;
import playground.mmoyo.PTRouter.PTOb;
import playground.mmoyo.TransitSimulation.TransitRouteFinder;

public class RouterTest extends MatsimTestCase {

	private static final String path = "../shared-svn/studies/schweiz-ivtch/pt-experimental/"; 

	private static final String CONFIG=  path  + "config.xml";
	private static final String ZURICHPTN= path + "network.xml";
	private static final String ZURICHPTTIMETABLE= path + "PTTimetable.xml";
	private static final String ZURICHPTPLANS= path + "plans.xml";
	private static final String OUTPUTPLANS= path + "output_plans.xml";

	public void test1() {
		PTOb pt= new PTOb(CONFIG, ZURICHPTN, ZURICHPTTIMETABLE,ZURICHPTPLANS, OUTPUTPLANS); 
		pt.readPTNet(ZURICHPTN);

		// searches and shows a PT path between two coordinates 
		Coord coord1 = new CoordImpl(747420, 262794);   
		Coord coord2 = new CoordImpl(685862, 254136);
		Path path2 = pt.getPtRouter2().findPTPath (coord1, coord2, 24372, 300);
		System.out.println(path2.links.size());
		for (LinkImpl link : path2.links){
			System.out.println(link.getId()+ ": " + link.getFromNode().getId() + " " + link.getType() + link.getToNode().getId() );
		}

		assertEquals( 31, path2.links.size() ) ;
		assertEquals( "1311" , path2.links.get(10).getId().toString() ) ;
		assertEquals( "250" , path2.links.get(20).getId().toString() ) ;
	}
	
	
	public void testWithVerySimpleTransitSchedule() throws SAXException, ParserConfigurationException, IOException {
		/* for integration into MATSim, the following must work */
		
		// setup very simple scenario
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile("test/input/playground/marcel/pt/transitSchedule/network.xml");
		TransitScheduleBuilder builder = new TransitScheduleBuilderImpl();
		TransitSchedule schedule = builder.createTransitSchedule();
		new TransitScheduleReaderV1(schedule, network).readFile("test/input/playground/marcel/pt/transitSchedule/transitSchedule.xml");
		PopulationImpl population = new PopulationImpl();
		new MatsimPopulationReader(population, network).readFile("test/input/playground/marcel/pt/plans.xml");
		PersonImpl person = population.getPersons().get(new IdImpl(1));
		ActivityImpl fromAct = (ActivityImpl) person.getPlans().get(0).getPlanElements().get(0);
		ActivityImpl toAct = (ActivityImpl) person.getPlans().get(0).getPlanElements().get(2);
		
		// make sure our setup is as expected
		assertNotNull(person);
		assertNotNull(fromAct);
		assertNotNull(toAct);
		
		// in the plans file, there are no coordinates for the activities... fix that.
		fromAct.setCoord(new CoordImpl(400.0, 399.0));
		toAct.setCoord(new CoordImpl(4700.0, 700.0));
		
		// now run the essential thing:
		TransitRouteFinder routeFinder = new TransitRouteFinder(schedule);
		List<LegImpl> legs = routeFinder.calculateRoute(fromAct, toAct, person);
		
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
