/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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

package org.matsim.basic.v01;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.facilities.Facilities;
import org.matsim.interfaces.basic.v01.BasicHousehold;
import org.matsim.interfaces.population.Household;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Population;
import org.matsim.population.PopulationReaderMatsimV5;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

/**
 * @author dgrether
 */
public class BasicPopulationReaderV5Test extends MatsimTestCase {

  private static final String TESTXML  = "testPopulation.xml";
	
  private final Id id23 = new IdImpl("23");
  private final Id id24 = new IdImpl("24");
//  private final Id id42 = new IdImpl("42");
//  private final Id id43 = new IdImpl("43");
//  private final Id id44 = new IdImpl("44");
//  private final Id id45 = new IdImpl("45");
  private final Id id666 = new IdImpl("666");
  private final Coord coord = new CoordImpl(0.0, 0.0);
  
	public void testBasicParser() {
		BasicPopulation<BasicPerson<BasicPlan>> population = new BasicPopulationImpl<BasicPerson<BasicPlan>>();
		List<BasicHousehold> households = new ArrayList<BasicHousehold>();
		BasicPopulationReaderMatsimV5 reader = new BasicPopulationReaderMatsimV5(population, households);
		reader.readFile(this.getPackageInputDirectory() + TESTXML);
		checkContent(population);
		BasicHouseholdsReaderV100Test hhTest = new BasicHouseholdsReaderV100Test();
		hhTest.checkContent(households);
	}
	
	public void testParser() {
		Population pop = new Population(Population.NO_STREAMING);
		NetworkLayer net = new NetworkLayer();
		createNetwork(net);
		List<Household> households = new ArrayList<Household>();
		Facilities fac = new Facilities();
		fac.createFacility(id666, coord);
		PopulationReaderMatsimV5 parser = new PopulationReaderMatsimV5(net, pop, households, fac);
		parser.readFile(this.getPackageInputDirectory() + TESTXML);
		checkContent(pop);
		BasicHouseholdsReaderV100Test hhTest = new BasicHouseholdsReaderV100Test();
		hhTest.checkContent(households);
	}
	
	private void createNetwork(NetworkLayer n) {
		Node n23 = n.createNode(id23, this.coord);
		Node n24 = n.createNode(id24, coord);
		Node n666  = n.createNode(id666, coord);
		n.createLink(id23, n23, n24, 0.0, 0.0, 1, 1);
		n.createLink(id24, n24, n666, 0.0, 0.0, 1, 1);
	}

	private void checkContent(BasicPopulation population) {
		assertNotNull(population);
		BasicPerson pp = population.getPerson(id23);
		assertNotNull(pp);
		assertEquals(1, pp.getPlans().size());
		BasicPlan p = (BasicPlan) pp.getPlans().get(0);
		assertNotNull(p);
		assertTrue(p.isSelected());
		assertTrue(p.hasUndefinedScore());
		int i = 0;
		for (ActIterator it = p.getIteratorAct(); it.hasNext();) {
			BasicAct act = it.next();
			assertNotNull(act);
			if(i == 0) {
				assertEquals("h", act.getType());
				assertNull(act.getFacilityId());
				assertNull(act.getLinkId());
				assertNotNull(act.getCoord());
				assertEquals(48.28d, act.getCoord().getX(), EPSILON);
				assertEquals(7.56d, act.getCoord().getY(), EPSILON);
				assertEquals(6.0*3600.0d, act.getEndTime(), EPSILON);
			}
			else if (i == 1) {
				assertEquals("w", act.getType());
				assertNotNull(act.getFacilityId());
				assertNull(act.getLinkId());
				assertNull(act.getCoord());
				assertEquals(12.0*3600.0d + 10.0 * 60.0d, act.getEndTime(), EPSILON);
			}
			else if (i == 2) {
				assertEquals("w", act.getType());
				assertNull(act.getLinkId());
				assertNull(act.getCoord());
				assertNotNull(act.getFacilityId());
				assertEquals(id666, act.getFacilityId());
			}
			else if(i == 3) {
				assertEquals("h", act.getType());
				assertNull(act.getFacilityId());
				assertNull(act.getLinkId());
				assertNotNull(act.getCoord());
				assertEquals(48.28d, act.getCoord().getX(), EPSILON);
				assertEquals(7.56d, act.getCoord().getY(), EPSILON);
			}
			i++;
		}
		assertEquals(4, i);
		
		i = 0;
		for (LegIterator it = p.getIteratorLeg(); it.hasNext();) {
			BasicLeg leg = it.next();
			assertNotNull(leg);
			BasicRoute route;
			if (i == 0) {
				assertEquals(BasicLeg.Mode.car, leg.getMode());
				assertNotNull(leg.getRoute());
				route = leg.getRoute();
				assertEquals(45.5d, route.getDist(), EPSILON);
				assertEquals(24.3d, route.getTravTime(), EPSILON);
				assertEquals(2, route.getLinkIds().size());
				assertEquals(id23, route.getLinkIds().get(0));
				assertEquals(id24, route.getLinkIds().get(1));
			}
			else if (i == 1) {
				assertEquals(BasicLeg.Mode.car, leg.getMode());
				assertNotNull(leg.getRoute());
				route = leg.getRoute();
				assertEquals(1, route.getLinkIds().size());
				assertEquals(id23, route.getLinkIds().get(0));
			}
			else if (i == 2) {
				assertEquals(BasicLeg.Mode.pt, leg.getMode());
				assertNull(leg.getRoute());
			}
			i++;
		}
		assertEquals(3, i);
	}
	
}
