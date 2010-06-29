/* *********************************************************************** *
 * project: org.matsim.*
 * MyPlansProcessorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class MyPlansProcessorTest extends MatsimTestCase{
	
	private Logger log = Logger.getLogger(MyPlansProcessorTest.class);
	private Scenario scenario;
	private List<MyZone> zones;
	
	public void testMyPlansProcessorConstructor(){
		setupTest();
		MyPlansProcessor mpp = new MyPlansProcessor(scenario, zones);
		assertNotNull("Did not create MyPlansProcessor.", mpp);
		assertNotNull("The Scenario object is null.", mpp.getScenario());
		assertNotNull("The zones list is null.", mpp.getZones());
		assertNotNull("The DenseDoubleMatrix2D is null.", mpp.getOdMatrix());
		assertEquals("Matrix has wrong number of rows.", zones.size(), mpp.getOdMatrix().rows());
		assertEquals("Matrix has wrong number of columns.", zones.size(), mpp.getOdMatrix().columns());
	}
	
	public void testProcessPlans(){
		setupTest();
		MyPlansProcessor mpp = new MyPlansProcessor(scenario, zones);
		mpp.processPlans();
		assertEquals("Wrong travel time from zone 1 to 1.", 0.0, mpp.getOdMatrix().get(0, 0));
		assertEquals("Wrong travel time from zone 1 to 2.", 0.0, mpp.getOdMatrix().get(0, 1));
		assertEquals("Wrong travel time from zone 1 to 3.", 0.0, mpp.getOdMatrix().get(0, 2));
		assertEquals("Wrong travel time from zone 1 to 4.", 30.0, mpp.getOdMatrix().get(0, 3));
		assertEquals("Wrong travel time from zone 2 to 1.", 0.0, mpp.getOdMatrix().get(1, 0));
		assertEquals("Wrong travel time from zone 2 to 2.", 0.0, mpp.getOdMatrix().get(1, 1));
		assertEquals("Wrong travel time from zone 2 to 3.", 0.0, mpp.getOdMatrix().get(1, 2));
		assertEquals("Wrong travel time from zone 2 to 4.", 0.0, mpp.getOdMatrix().get(1, 3));
		assertEquals("Wrong travel time from zone 3 to 1.", 0.0, mpp.getOdMatrix().get(2, 0));
		assertEquals("Wrong travel time from zone 3 to 2.", 0.0, mpp.getOdMatrix().get(2, 1));
		assertEquals("Wrong travel time from zone 3 to 3.", 0.0, mpp.getOdMatrix().get(2, 2));
		assertEquals("Wrong travel time from zone 3 to 4.", 0.0, mpp.getOdMatrix().get(2, 3));
		assertEquals("Wrong travel time from zone 4 to 1.", 10.0, mpp.getOdMatrix().get(3, 0));
		assertEquals("Wrong travel time from zone 4 to 2.", 0.0, mpp.getOdMatrix().get(3, 1));
		assertEquals("Wrong travel time from zone 4 to 3.", 0.0, mpp.getOdMatrix().get(3, 2));
		assertEquals("Wrong travel time from zone 4 to 4.", 0.0, mpp.getOdMatrix().get(3, 3));
	}
	
	

	/**
	 * 
	 */
	private void setupTest() {
		// Set up zones.
		File folder = new File(getInputDirectory());
		String shapefile = folder.getParent() + "/zones.shp";
		MyZoneReader mzr = new MyZoneReader(shapefile);
		mzr.readZones(1);
		zones = (List<MyZone>) mzr.getZones();
		
		// Set up scenario.
		scenario = new ScenarioImpl();
		
		//=====================================================================
		// Network.
		//---------------------------------------------------------------------
		Network n = scenario.getNetwork();
		NetworkFactory nf = n.getFactory();

		Node n1 = nf.createNode(new IdImpl("1"), new CoordImpl(2,3));
		Node n2 = nf.createNode(new IdImpl("2"), new CoordImpl(8,3));
		Node n3 = nf.createNode(new IdImpl("3"), new CoordImpl(7,7));
		Node n4 = nf.createNode(new IdImpl("4"), new CoordImpl(3,7));
		
		n.addNode(n1); n.addNode(n2); n.addNode(n3); n.addNode(n4);

		n.addLink(nf.createLink(new IdImpl("12"), n1.getId(), n2.getId()));
		n.addLink(nf.createLink(new IdImpl("13"), n1.getId(), n3.getId()));
		n.addLink(nf.createLink(new IdImpl("14"), n1.getId(), n4.getId()));
		n.addLink(nf.createLink(new IdImpl("21"), n2.getId(), n1.getId()));
		n.addLink(nf.createLink(new IdImpl("23"), n2.getId(), n3.getId()));
		n.addLink(nf.createLink(new IdImpl("24"), n2.getId(), n4.getId()));
		n.addLink(nf.createLink(new IdImpl("31"), n3.getId(), n1.getId()));
		n.addLink(nf.createLink(new IdImpl("32"), n3.getId(), n2.getId()));
		n.addLink(nf.createLink(new IdImpl("34"), n3.getId(), n4.getId()));
		n.addLink(nf.createLink(new IdImpl("41"), n4.getId(), n1.getId()));
		n.addLink(nf.createLink(new IdImpl("42"), n4.getId(), n2.getId()));
		n.addLink(nf.createLink(new IdImpl("43"), n4.getId(), n3.getId()));
		
		NetworkWriter nw = new NetworkWriter(n);
		nw.write(getOutputDirectory() + "/networkTest.xml");
		//=====================================================================
		
		
		//=====================================================================
		// Plans.
		//---------------------------------------------------------------------
		Population p = scenario.getPopulation();
		PopulationFactory pf = p.getFactory();
		
		// Person 1.
		//---------------------------------------------------------------------
		Person p1 = pf.createPerson(new IdImpl("0"));
		Plan plan = pf.createPlan();
		// Home.
		Activity a1 = new ActivityImpl("home", new CoordImpl(1.0, 1.0)); a1.setEndTime(6*3600);
		plan.addActivity(a1);
		// Home -> work.
		Leg l1 = new LegImpl(TransportMode.car);
		Link homeLink = n.getLinks().get(new IdImpl("12"));
		Link workLink = n.getLinks().get(new IdImpl("43"));
		List<Id> hwLinks = new ArrayList<Id>();
		hwLinks.add(n.getLinks().get(new IdImpl("24")).getId());
		NetworkRoute nr1 = new LinkNetworkRouteImpl(homeLink.getId(), workLink.getId());
		nr1.setLinkIds(homeLink.getId(), hwLinks, workLink.getId());
		l1.setRoute(nr1);
		l1.setTravelTime(30.0*60.0);
		plan.addLeg(l1);
		// TODO Try dijkstra here.	
		// Work.
		Activity a2 = new ActivityImpl("work", new CoordImpl(9.0, 9.0)); 
		a2.setStartTime(7*3600);
		a2.setEndTime(16*3600);
		plan.addActivity(a2);
		// Work -> home.
		Leg l2 = new LegImpl(TransportMode.car);
		NetworkRoute nr2 = new LinkNetworkRouteImpl(workLink.getId(), homeLink.getId());
		l2.setRoute(nr2);
		l2.setTravelTime(10.0*60.0);
		plan.addLeg(l2);
		// Home.
		Activity a3 = new ActivityImpl("home", new CoordImpl(1.0, 1.0));
		a3.setStartTime(17*3600);
		plan.addActivity(a3);
		//---------------------------------------------------------------------
		p1.addPlan(plan);
		plan.setSelected(true);
		p.addPerson(p1);

		PopulationWriter pw = new PopulationWriter(p, n);
		pw.writeFileV4(getOutputDirectory() + "/populationTest.xml");
		//=====================================================================
		log.info("Wrote population.");
		
		
			
	}

}

