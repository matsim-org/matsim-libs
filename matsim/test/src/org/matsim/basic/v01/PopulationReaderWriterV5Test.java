/* *********************************************************************** *
 * project: org.matsim.*
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.interfaces.basic.v01.BasicHousehold;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Household;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReaderMatsimV5;
import org.matsim.population.PopulationWriterV5;
import org.matsim.population.Vehicle;
import org.matsim.population.VehicleReaderV1;
import org.matsim.population.routes.CarRoute;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;


/**
 * @author dgrether
 */
public class PopulationReaderWriterV5Test extends MatsimTestCase {

  private static final String TESTXML  = "testPopulation.xml";
  
  private static final String TESTVEHICLESXML = "testVehicles.xml";
	
  private final Id id23 = new IdImpl("23");
  private final Id id24 = new IdImpl("24");
  private final Id id42 = new IdImpl("42");
  private final Id id43 = new IdImpl("43");
  private final Id id44 = new IdImpl("44");
  private final Id id45 = new IdImpl("45");
  private final Id id666 = new IdImpl("666");
  private final Coord coord = new CoordImpl(0.0, 0.0);
  
  
	public void testBasicParser() {
		BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge>> population = new BasicPopulationImpl<BasicPerson<BasicPlan, BasicKnowledge>>();
		Map<Id, BasicHousehold> households = new HashMap<Id, BasicHousehold>();
		BasicPopulationReaderV5 reader = new BasicPopulationReaderV5(population, households);
		reader.readFile(this.getPackageInputDirectory() + TESTXML);
		checkContent(population);
		BasicHouseholdsReaderV1Test hhTest = new BasicHouseholdsReaderV1Test();
		hhTest.checkContent(households);
	}
	
	public void testParser() {
		//read vehicles
		Map<String, BasicVehicleType> vehicleTypes = new HashMap<String, BasicVehicleType>();
		Map<Id, Vehicle> vehicles = new HashMap<Id, Vehicle>();
		VehicleReaderV1 reader = new VehicleReaderV1(vehicleTypes, vehicles);
		reader.readFile(this.getPackageInputDirectory() + TESTVEHICLESXML);
		//create pop and add missing persons refered from testHouseholds.xml
		Population pop = new Population(Population.NO_STREAMING);
		pop.addPerson(new PersonImpl(id42));
		pop.addPerson(new PersonImpl(id43));
		pop.addPerson(new PersonImpl(id44));
		pop.addPerson(new PersonImpl(id45));
		//create the network layer containing the referenced links
		NetworkLayer net = new NetworkLayer();
		createNetwork(net);
		//create the household and facility data structures
		Map<Id, Household> households = new HashMap<Id, Household>();
		Facilities facilities = new Facilities();
		facilities.createFacility(id666, coord);
		//do it, do it, do it now
		PopulationReaderMatsimV5 parser = new PopulationReaderMatsimV5(net, pop, households, facilities, vehicles);
		parser.readFile(this.getPackageInputDirectory() + TESTXML);
		checkContent(pop);
		BasicHouseholdsReaderV1Test hhTest = new BasicHouseholdsReaderV1Test();
		hhTest.checkContent((Map)households);
		checkReferences(pop, facilities, net, vehicles);
	}
	
	private void checkReferences(Population pop, Facilities fac, NetworkLayer net, Map<Id, Vehicle> vehicles) {
		Person person = pop.getPerson(id23);
		assertNotNull(person);
		assertNotNull(person.getKnowledge());
		assertNotNull(person.getKnowledge().getActivities());
		for (Activity a : person.getKnowledge().getActivities()) {
			assertNotNull(a);
			assertNotNull(a.getFacility());
			assertEquals(fac.getFacilities().get(id666), a.getFacility());
		}
		Plan p = person.getPlans().get(0);
		Act a = p.getFirstActivity();
		assertNotNull(a.getCoord());
		assertNull(a.getFacility());
		assertNull(a.getFacilityId());
		assertNull(a.getLink());
		assertNull(a.getLinkId());
		Leg l = p.getNextLeg(a);
		CarRoute route = (CarRoute) l.getRoute();
		assertNotNull(route);
		assertNotNull(route.getNodes());
		assertNotNull(route.getLinkIds());
		assertNotNull(route.getLinks());
		for (Node n : route.getNodes()) {
			assertNotNull(n);
		}
		for (Link ll : route.getLinks()){
			assertNotNull(ll);
			assertTrue((ll.equals(net.getLink(id23))) || ll.equals(net.getLink(id24)));
		}
		a = p.getNextActivity(l);
		assertNotNull(a);
		assertNull(a.getCoord());
		assertNull(a.getLink());
		assertNull(a.getLinkId());
		assertNotNull(a.getFacility());
		assertNotNull(a.getFacilityId());
		assertEquals(fac.getFacilities().get(id666), a.getFacility());
		assertEquals(id666, a.getFacilityId());
		//...one could test the complete plan however this gets boring so step to the households
		Household h = person.getHousehold();
		assertNotNull(h);
		assertEquals(id23, h.getId());
		assertNotNull(h.getMembers());
		assertNotNull(h.getMemberIds());
		List<Id> memberIds = h.getMemberIds();
		Collections.sort(memberIds);
		assertEquals(id23, memberIds.get(0));
		assertEquals(id42, memberIds.get(1));
		assertEquals(id43, memberIds.get(2));
		for (Person per : h.getMembers().values()){
			assertNotNull(per);
			assertTrue(per.equals(pop.getPerson(id23)) || per.equals(pop.getPerson(id42)) || per.equals(pop.getPerson(id43)));
		}
		assertNotNull(h.getBasicLocation());
		assertEquals(fac.getFacilities().get(id666), h.getBasicLocation());
		assertNotNull(h.getVehicles());
		assertEquals(2, h.getVehicles().size());
		for (Vehicle v : h.getVehicles().values()) {
			assertNotNull(v);
			assertTrue(v.equals(vehicles.get(id23)) || v.equals(vehicles.get(id42)));
		}
	}

	public void testWriter() throws FileNotFoundException, IOException {
		//read the file
		BasicPopulation<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> population = new BasicPopulationImpl<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>>();
		Map<Id, BasicHousehold> households = new HashMap<Id, BasicHousehold>();
		BasicPopulationReaderV5 reader = new BasicPopulationReaderV5(population, households);
		reader.readFile(this.getPackageInputDirectory() + TESTXML);
		//write it
		PopulationWriterV5 writer = new PopulationWriterV5(population, households);
		writer.setPrettyPrint(true);
		writer.useCompression(false);
		writer.writeFile(this.getOutputDirectory() + "testPopulationOutput.xml");
		//read it again and check the content
		population = new BasicPopulationImpl<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>();
		households = new HashMap<Id, BasicHousehold>();
		reader = new BasicPopulationReaderV5(population, households);
		reader.readFile(this.getOutputDirectory() + "testPopulationOutput.xml");
		this.checkContent(population);
		BasicHouseholdsReaderV1Test hhTest = new BasicHouseholdsReaderV1Test();
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
		assertEquals(id23, pp.getId());
		assertEquals("m", pp.getSex());
		assertEquals(23, pp.getAge());
		assertTrue(pp.hasLicense());
		assertEquals("sometimes", pp.getCarAvail());
		assertTrue(pp.isEmployed());
		assertEquals("ch-HT-2y", pp.getTravelcards().first());
		assertEquals(id23, pp.getFiscalHouseholdId());
		//check knowledge
		BasicKnowledge<BasicActivity> knowledge = pp.getKnowledge();
		assertNotNull(knowledge);
		assertNotNull(knowledge.getDescription());
		assertNotNull(knowledge.getActivities());
		assertEquals(2, knowledge.getActivities().size());
		BasicActivity activity = knowledge.getActivities().get(0);
		assertNotNull(activity);
		assertEquals(Integer.valueOf(4), activity.getFrequency());
		assertNotNull(activity.getLocation());
		assertEquals(id666, activity.getLocation().getId());
		assertEquals(LocationType.FACILITY, activity.getLocation().getLocationType());
		assertEquals(new Integer(40), activity.getCapacity());
		assertNotNull(activity.getOpeningTime(DayType.wk));
		assertEquals(8.0 * 3600.0, activity.getOpeningTime(DayType.wk).first().getStartTime(), EPSILON);
		assertEquals(17.0 * 3600.0, activity.getOpeningTime(DayType.wk).first().getEndTime(), EPSILON);

		activity = knowledge.getActivities().get(1);
		assertNotNull(activity);
		assertNull(activity.getFrequency());
		assertNotNull(activity.getLocation());
		assertNotNull(activity.getLocation().getId());
		assertEquals(id666, activity.getLocation().getId());
		//here we have to branch the test, as the basic classes don't need a coordinate for a location
		//the derived classes however do -> there is still an artificial null value of 0.0, 0.0
		if (activity.getLocation().getCenter() == null) {
			assertTrue(true);
		}
		else {
			assertEquals(new CoordImpl(0.0d, 0.0d), activity.getLocation().getCenter());
		}
		assertNull(activity.getCapacity());
		assertNull(activity.getOpeningTime(DayType.wk));

		
		//now check the contents of plans
		assertEquals(1, pp.getPlans().size());
		BasicPlan p = (BasicPlan) pp.getPlans().get(0);
		assertNotNull(p);
		assertTrue(p.isSelected());
		assertEquals(42.42d, p.getScore(), EPSILON);
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
				assertEquals(23.0d*60.0d, act.getDuration(), EPSILON);
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
				assertEquals(6.0d * 3600.0d + 5.0d * 60.0d, leg.getDepartureTime(), EPSILON);
				assertEquals(25.0d * 60.0d, leg.getTravelTime(), EPSILON);
				assertEquals(6.0d * 3600.0d + 30.0d * 60.0d, leg.getArrivalTime(), EPSILON);
				assertNotNull(leg.getRoute());
				route = leg.getRoute();
				assertEquals(45.5d, route.getDist(), EPSILON);
				assertEquals(23.0d * 60.0d + 23.0d, route.getTravelTime(), EPSILON);
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
