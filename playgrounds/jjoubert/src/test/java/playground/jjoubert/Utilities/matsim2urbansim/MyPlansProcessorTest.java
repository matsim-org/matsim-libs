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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class MyPlansProcessorTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private Logger log = Logger.getLogger(MyPlansProcessorTest.class);
	private Scenario scenario;
	private List<MyZone> zones;

	@Test
	public void testMyPlansProcessorConstructor(){
		setupTest();
		MyPlansProcessor mpp = new MyPlansProcessor(scenario, zones);
		Assert.assertNotNull("Did not create MyPlansProcessor.", mpp);
		Assert.assertNotNull("The Scenario object is null.", mpp.getScenario());
		Assert.assertNotNull("The zones list is null.", mpp.getZones());
		Assert.assertNotNull("The DenseDoubleMatrix2D is null.", mpp.getOdMatrix());
		Assert.assertEquals("Matrix has wrong number of rows.", zones.size(), mpp.getOdMatrix().rows());
		Assert.assertEquals("Matrix has wrong number of columns.", zones.size(), mpp.getOdMatrix().columns());
	}

	@Test
	public void testProcessPlans(){
		setupTest();
		MyPlansProcessor mpp = new MyPlansProcessor(scenario, zones);
		mpp.processPlans();
		Assert.assertEquals("Wrong travel time from zone 1 to 4.", ((30.0 + 40.0)*60.0)/2.0, mpp.getAvgOdTravelTime(0, 3), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time from zone 4 to 1.", ((10.0 + 20.0)*60.0)/2.0, mpp.getAvgOdTravelTime(3, 0), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testWriteOdMatrixToDbf(){
		setupTest();
		MyPlansProcessor mpp = new MyPlansProcessor(scenario, zones);
		mpp.processPlans();
		mpp.writeOdMatrixToDbf(utils.getOutputDirectory() + "testDbf.dbf");
		Assert.assertTrue("Dbf table file does not exist", ((new File(utils.getOutputDirectory() + "testDbf.dbf")).exists()));
	}


	/**
	 *
	 */
	private void setupTest() {
		// Set up zones.
		File folder = new File(utils.getInputDirectory());
		String shapefile = folder.getParent() + "/zones.shp";
		MyZoneReader mzr = new MyZoneReader(shapefile);
		mzr.readZones(1);
		zones = mzr.getZoneList();

		// Set up scenario.
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		//=====================================================================
		// Network.
		//---------------------------------------------------------------------
		Network n = scenario.getNetwork();
		NetworkFactory nf = n.getFactory();

		Node n1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 2, (double) 3));
		Node n2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 8, (double) 3));
		Node n3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 7, (double) 7));
		Node n4 = nf.createNode(Id.create("4", Node.class), new Coord((double) 3, (double) 7));

		n.addNode(n1); n.addNode(n2); n.addNode(n3); n.addNode(n4);

		n.addLink(nf.createLink(Id.create("12", Link.class), n1, n2));
		n.addLink(nf.createLink(Id.create("13", Link.class), n1, n3));
		n.addLink(nf.createLink(Id.create("14", Link.class), n1, n4));
		n.addLink(nf.createLink(Id.create("21", Link.class), n2, n1));
		n.addLink(nf.createLink(Id.create("23", Link.class), n2, n3));
		n.addLink(nf.createLink(Id.create("24", Link.class), n2, n4));
		n.addLink(nf.createLink(Id.create("31", Link.class), n3, n1));
		n.addLink(nf.createLink(Id.create("32", Link.class), n3, n2));
		n.addLink(nf.createLink(Id.create("34", Link.class), n3, n4));
		n.addLink(nf.createLink(Id.create("41", Link.class), n4, n1));
		n.addLink(nf.createLink(Id.create("42", Link.class), n4, n2));
		n.addLink(nf.createLink(Id.create("43", Link.class), n4, n3));

		NetworkWriter nw = new NetworkWriter(n);
		nw.write(utils.getOutputDirectory() + "/networkTest.xml");
		//=====================================================================


		//=====================================================================
		// Plans.
		//---------------------------------------------------------------------
		Population p = scenario.getPopulation();
		PopulationFactory pf = p.getFactory();
		//---------------------------------------------------------------------
		// Person 1.
		//---------------------------------------------------------------------
		Person p1 = pf.createPerson(Id.create("0", Person.class));
		Plan plan = pf.createPlan();
		// Home.
		Activity a1 = new ActivityImpl("home", new Coord(1.0, 1.0)); a1.setEndTime(6*3600);
		plan.addActivity(a1);
		// Home -> work.
		Leg l1 = new LegImpl(TransportMode.car);
		Link homeLink = n.getLinks().get(Id.create("12", Link.class));
		Link workLink = n.getLinks().get(Id.create("43", Link.class));
		List<Id<Link>> hwLinks = new ArrayList<Id<Link>>();
		hwLinks.add(n.getLinks().get(Id.create("24", Link.class)).getId());
		NetworkRoute nr1 = new LinkNetworkRouteImpl(homeLink.getId(), workLink.getId());
		nr1.setLinkIds(homeLink.getId(), hwLinks, workLink.getId());
		l1.setRoute(nr1);
		l1.setTravelTime(30.0*60.0);
		plan.addLeg(l1);
		// TODO Try dijkstra here.
		// Work.
		Activity a2 = new ActivityImpl("work", new Coord(9.0, 9.0));
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
		Activity a3 = new ActivityImpl("home", new Coord(1.0, 1.0));
		a3.setStartTime(17*3600);
		plan.addActivity(a3);
		//---------------------------------------------------------------------
		p1.addPlan(plan);
		p.addPerson(p1);
		//---------------------------------------------------------------------
		// Person 2. Same characteristics as Person 1, but he (or SHE, rather)
		// travels a bit slower ;-)
		//---------------------------------------------------------------------
		Person p2 = pf.createPerson(Id.create("1", Person.class));
		plan = pf.createPlan();
		// Home.
		a1 = new ActivityImpl("home", new Coord(1.0, 1.0)); a1.setEndTime(6*3600);
		plan.addActivity(a1);
		// Home -> work.
		l1 = new LegImpl(TransportMode.car);
		homeLink = n.getLinks().get(Id.create("12", Link.class));
		workLink = n.getLinks().get(Id.create("43", Link.class));
		hwLinks = new ArrayList<Id<Link>>();
		hwLinks.add(n.getLinks().get(Id.create("24", Link.class)).getId());
		nr1 = new LinkNetworkRouteImpl(homeLink.getId(), workLink.getId());
		nr1.setLinkIds(homeLink.getId(), hwLinks, workLink.getId());
		l1.setRoute(nr1);
		l1.setTravelTime(40.0*60.0);
		plan.addLeg(l1);
		// TODO Try dijkstra here.
		// Work.
		a2 = new ActivityImpl("work", new Coord(9.0, 9.0));
		a2.setStartTime(7*3600);
		a2.setEndTime(16*3600);
		plan.addActivity(a2);
		// Work -> home.
		l2 = new LegImpl(TransportMode.car);
		nr2 = new LinkNetworkRouteImpl(workLink.getId(), homeLink.getId());
		l2.setRoute(nr2);
		l2.setTravelTime(20.0*60.0);
		plan.addLeg(l2);
		// Home.
		a3 = new ActivityImpl("home", new Coord(1.0, 1.0));
		a3.setStartTime(17*3600);
		plan.addActivity(a3);
		//---------------------------------------------------------------------
		p2.addPlan(plan);
		p.addPerson(p2);

		PopulationWriter pw = new PopulationWriter(p, n);
		pw.writeFileV4(utils.getOutputDirectory() + "/populationTest.xml");
		//=====================================================================
		log.info("Wrote population.");



	}

}

