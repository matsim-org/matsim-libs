/* *********************************************************************** *
 * project: org.matsim.*
 * CrossingScenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.boundarycondition;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import playground.gregor.sim2d_v4.io.Sim2DConfigWriter01;
import playground.gregor.sim2d_v4.io.Sim2DEnvironmentWriter02;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class CrossingScenario {

	private static String inputDir = "/Users/laemmel/devel/crossing/input";
	private static String outputDir = "/Users/laemmel/devel/crossing/output";


	private static final boolean uni = true;

	private static final int nrAgents = 16000;

	public static final double SEPC_FLOW = .2;//1.2;//1.2;

	public static void main(String [] args) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		createNetwork(sc);

		Sim2DConfig s2d = Sim2DConfigUtils.createConfig();
		s2d.setTimeStepSize(0.04);
		Sim2DScenario s2dsc = Sim2DScenarioUtils.createSim2dScenario(s2d);
		create2DWorld(s2dsc);


		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(.71);
		((NetworkImpl)sc.getNetwork()).setCapacityPeriod(1);
		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, s2dsc);



		//write s2d envs
		for (Sim2DEnvironment env : s2dsc.getSim2DEnvironments()) {
			String envFile = inputDir + "/sim2d_environment_" + env.getId() + ".gml.gz";
			String netFile = inputDir + "/sim2d_network_" + env.getId() + ".xml.gz";
			new Sim2DEnvironmentWriter02(env).write(envFile);
			new NetworkWriter(env.getEnvironmentNetwork()).write(netFile);
			s2d.addSim2DEnvironmentPath(envFile);
			s2d.addSim2DEnvNetworkMapping(envFile, netFile);
		}

		new Sim2DConfigWriter01(s2d).write(inputDir + "/s2d_config.xml");


		c.network().setInputFile(inputDir + "/network.xml.gz");

		//		c.strategy().addParam("Module_1", "playground.gregor.sim2d_v4.replanning.Sim2DReRoutePlanStrategy");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "250");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".9");

		c.controler().setOutputDirectory(outputDir);
		c.controler().setLastIteration(0);

		c.plans().setInputFile(inputDir + "/population.xml.gz");

		ActivityParams pre = new ActivityParams("origin");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);


		ActivityParams post = new ActivityParams("destination");
		post.setTypicalDuration(49); // dito
		post.setMinimalDuration(49);
		post.setClosingTime(49);
		post.setEarliestEndTime(49);
		post.setLatestStartTime(49);
		post.setOpeningTime(49);
		sc.getConfig().planCalcScore().addActivityParams(pre);
		sc.getConfig().planCalcScore().addActivityParams(post);

		sc.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
		sc.getConfig().planCalcScore().setPerforming_utils_hr(0.);


		QSimConfigGroup qsim = sc.getConfig().qsim();
		qsim.setEndTime(4*3600);

		c.controler().setMobsim("hybridQ2D");

		c.global().setCoordinateSystem("EPSG:3395");

//		createNetworkChangeEvents(sc);


		new ConfigWriter(c).write(inputDir+ "/config.xml");

		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());

		createPopulation(sc);

		Population pop = sc.getPopulation();
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans().getInputFile());

	}

	private static void createNetworkChangeEvents(Scenario sc) {
		Collection<NetworkChangeEvent> events = new LinkedList<NetworkChangeEvent>();
		NetworkChangeEventFactoryImpl fac = new NetworkChangeEventFactoryImpl();
		double incr = (4*SEPC_FLOW)/10;
		double flowCap = incr;


		//		NetworkChangeEvent ee = fac.createNetworkChangeEvent(0);
		//		ee.addLink(sc.getNetwork().getLinks().get(Id.create("l2")));
		////		ee.addLink(sc.getNetwork().getLinks().get(Id.create("l6")));
		////		ee.addLink(sc.getNetwork().getLinks().get(Id.create("t_l6")));
		//		ChangeValue ccv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,0.01);
		//		ee.setFlowCapacityChange(ccv);
		//		events.add(ee);


		for (double time = 0; time < 120*60; time += 240) {

			NetworkChangeEvent e = fac.createNetworkChangeEvent(time);
			e.addLink(sc.getNetwork().getLinks().get(Id.create("l0", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("l1", Link.class)));
			//			e.addLink(sc.getNetwork().getLinks().get(Id.create("l2", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("l3", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("l4", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("l5", Link.class)));
			//			e.addLink(sc.getNetwork().getLinks().get(Id.create("l6", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("l7", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("t_l4", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("t_l5", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("t_l6", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("t_l7", Link.class)));

			e.addLink(sc.getNetwork().getLinks().get(Id.create("l0_rev", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("l1_rev", Link.class)));
			//			e.addLink(sc.getNetwork().getLinks().get(Id.create("l2_rev", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("l3_rev", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("l4_rev", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("l5_rev", Link.class)));
			//			e.addLink(sc.getNetwork().getLinks().get(Id.create("l6_rev", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("l7_rev", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("t_l4_rev", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("t_l5_rev", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("t_l6_rev", Link.class)));
			e.addLink(sc.getNetwork().getLinks().get(Id.create("t_l7_rev", Link.class)));

			System.out.println(time/60 + " flow" + flowCap/4); 

			ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, flowCap);
			e.setFlowCapacityChange(cv);
			events.add(e);

			NetworkChangeEvent ee1 = fac.createNetworkChangeEvent(time);
			ee1.addLink(sc.getNetwork().getLinks().get(Id.create("l2", Link.class)));
			ee1.addLink(sc.getNetwork().getLinks().get(Id.create("l6", Link.class)));
			//			ee.addLink(sc.getNetwork().getLinks().get(Id.create("l6", Link.class)));
			//			ee.addLink(sc.getNetwork().getLinks().get(Id.create("t_l6", Link.class)));
			if (time < 35*60) {
				ChangeValue ccv1 = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,0.95*flowCap);

				ee1.setFlowCapacityChange(ccv1);
			} else if (time < 39*60){
				ChangeValue ccv1 = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,0.7*flowCap);

				ee1.setFlowCapacityChange(ccv1);
			}else if (time < 41*60){
				ChangeValue ccv1 = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,0.001*flowCap);

				ee1.setFlowCapacityChange(ccv1);
			} else {
				ChangeValue ccv1 = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,1*flowCap);

				ee1.setFlowCapacityChange(ccv1);
			}
			events.add(ee1);


			flowCap += incr;
		}
		//		flowCap -= incr;
		//		for (double time = 20*60; time < 40*60; time += 120) {
		//			NetworkChangeEvent e = fac.createNetworkChangeEvent(time);
		//			e.addLink(sc.getNetwork().getLinks().get(Id.create("l0")));
		//			e.addLink(sc.getNetwork().getLinks().get(Id.create("l4")));
		//			e.addLink(sc.getNetwork().getLinks().get(Id.create("t_l4")));
		////			e.addLink(sc.getNetwork().getLinks().get(Id.create("mt_l4")));
		//			e.addLink(sc.getNetwork().getLinks().get(Id.create("t_l7_rev")));
		////			e.addLink(sc.getNetwork().getLinks().get(Id.create("mt_l7_rev")));
		//			e.addLink(sc.getNetwork().getLinks().get(Id.create("l3_rev")));
		//			e.addLink(sc.getNetwork().getLinks().get(Id.create("l7_rev")));
		//			ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, flowCap);
		//			e.setFlowCapacityChange(cv);
		//			events.add(e);
		//			flowCap -= incr;
		//		}

		new NetworkChangeEventsWriter().write(inputDir +"/networkChangeEvents.xml.gz", events);
		sc.getConfig().network().setTimeVariantNetwork(true);
		sc.getConfig().network().setChangeEventInputFile(inputDir+"/networkChangeEvents.xml.gz");

	}

	private static void createPopulation(Scenario sc) {
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 0;
		for (int i = 0; i < nrAgents; i++) {
			Person pers = fac.createPerson(Id.create("b"+i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", Id.create("k0", Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", Id.create("k3", Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}

		for (int i = nrAgents; i < 2*nrAgents; i++) {
			Person pers = fac.createPerson(Id.create("d"+i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", Id.create("k4", Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", Id.create("k7", Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}

	}


	private static void create2DWorld(Sim2DScenario sc2) {
		Sim2DEnvironment env = new Sim2DEnvironment();
		env.setId(Id.create("env0", Sim2DEnvironment.class));
		env.setEnvelope(new Envelope(0,36,0,36));
		try {
			env.setCRS(CRS.decode("EPSG:3395"));
		} catch (NoSuchAuthorityCodeException e) {
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sc2.addSim2DEnvironment(env);

		

		NetworkImpl net = NetworkImpl.createNetwork();
		NetworkFactoryImpl fac = net.getFactory();
		Node n0 = fac.createNode(Id.create("n0", Node.class), new CoordImpl(8,18));
		Node n1 = fac.createNode(Id.create("n1", Node.class), new CoordImpl(18,18));
		Node n2 = fac.createNode(Id.create("n2", Node.class), new CoordImpl(28,18));
		Node n3 = fac.createNode(Id.create("n3", Node.class), new CoordImpl(18,8));
		Node n4 = fac.createNode(Id.create("n4", Node.class), new CoordImpl(18,28));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);

		
		Id<Link> id0 = Id.create("l0", Link.class);
		{
		Link l = fac.createLink(id0, n0, n1);
		double flow = 4 *SEPC_FLOW;
		l.setFreespeed(1.34);
		l.setLength(10);
		l.setCapacity(flow);;
		Set<String> modes = new HashSet<String>();
		modes.add("walk2d");modes.add("walk");modes.add("car");
		l.setAllowedModes(modes);
		net.addLink(l);
		}
		Id<Link> id0Rev = Id.create("l0_rev", Link.class);
		{
		Link l = fac.createLink(id0Rev, n1, n0);
		double flow = 4 *SEPC_FLOW;
		l.setFreespeed(1.34);
		l.setLength(10);
		l.setCapacity(flow);;
		Set<String> modes = new HashSet<String>();
		modes.add("walk2d");modes.add("walk");modes.add("car");
		l.setAllowedModes(modes);
		net.addLink(l);
		}
		
		
		Id<Link> id1 = Id.create("l1", Link.class);
		{
		Link l = fac.createLink(id1, n1, n2);
		double flow = 4 *SEPC_FLOW;
		l.setFreespeed(1.34);
		l.setLength(10);
		l.setCapacity(flow);;
		Set<String> modes = new HashSet<String>();
		modes.add("walk2d");modes.add("walk");modes.add("car");
		l.setAllowedModes(modes);
		net.addLink(l);
		}
		Id<Link> id1Rev = Id.create("l1_rev", Link.class);
		{
		Link l = fac.createLink(id1Rev, n2, n1);
		double flow = 4 *SEPC_FLOW;
		l.setFreespeed(1.34);
		l.setLength(10);
		l.setCapacity(flow);;
		Set<String> modes = new HashSet<String>();
		modes.add("walk2d");modes.add("walk");modes.add("car");
		l.setAllowedModes(modes);
		net.addLink(l);
		}
		
		Id<Link> id2 = Id.create("l2", Link.class);
		{
			Link l = fac.createLink(id2, n3, n1);
			double flow = 4 *SEPC_FLOW;
			l.setFreespeed(1.34);
			l.setLength(10);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
		}
		Id<Link> id2Rev = Id.create("l2_rev", Link.class);
		{
			Link l = fac.createLink(id2Rev, n1, n3);
			double flow = 4 *SEPC_FLOW;
			l.setFreespeed(1.34);
			l.setLength(10);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
		}
		
		Id<Link> id3 = Id.create("l3", Link.class);
		{
			Link l = fac.createLink(id3, n1, n4);
			double flow = 4 *SEPC_FLOW;
			l.setFreespeed(1.34);
			l.setLength(10);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
		}
		Id<Link> id3Rev = Id.create("l3_rev", Link.class);
		{
			Link l = fac.createLink(id3Rev, n4, n1);
			double flow = 4 *SEPC_FLOW;
			l.setFreespeed(1.34);
			l.setLength(10);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
		}
		
		net.setCapacityPeriod(1);
		env.setNetwork(net);
		
		{
			int[] open = {0,2};
			GeometryFactory geofac = new GeometryFactory();
			Coordinate c0 = new Coordinate(8,16);
			Coordinate c1 = new Coordinate(8,20);
			Coordinate c2 = new Coordinate(16,20);
			Coordinate c3 = new Coordinate(16,16);
			Coordinate c4 = new Coordinate(8,16);
			Coordinate[] coords = {c0,c1,c2,c3,c4};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb01 = {Id.create("sec4", Section.class)};
			Section sec = env.createAndAddSection(Id.create("sec0", Section.class), p, open, nb01 , 0);
			sec.addRelatedLinkId(id0);
			sec.addRelatedLinkId(id0Rev);
		}

		{
			int[] open = {0,2};
			GeometryFactory geofac = new GeometryFactory();
			Coordinate c0 = new Coordinate(20,8);
			Coordinate c1 = new Coordinate(16,8);
			Coordinate c2 = new Coordinate(16,16);
			Coordinate c3 = new Coordinate(20,16);
			Coordinate c4 = new Coordinate(20,8);
			Coordinate[] coords = {c0,c1,c2,c3,c4};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb01 = {Id.create("sec4", Section.class)};
			Section sec = env.createAndAddSection(Id.create("sec1", Section.class), p, open, nb01 , 0);
			sec.addRelatedLinkId(id2);
			sec.addRelatedLinkId(id2Rev);
		}
		{
			int[] open = {0,2};
			GeometryFactory geofac = new GeometryFactory();
			Coordinate c0 = new Coordinate(28,20);
			Coordinate c1 = new Coordinate(28,16);
			Coordinate c2 = new Coordinate(20,16);
			Coordinate c3 = new Coordinate(20,20);
			Coordinate c4 = new Coordinate(28,20);
			Coordinate[] coords = {c0,c1,c2,c3,c4};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb01 = {Id.create("sec4", Section.class)};
			Section sec = env.createAndAddSection(Id.create("sec2", Section.class), p, open, nb01 , 0);
			sec.addRelatedLinkId(id1);
			sec.addRelatedLinkId(id1Rev);
		}
		{
			int[] open = {0,2};
			GeometryFactory geofac = new GeometryFactory();
			Coordinate c0 = new Coordinate(16,28);
			Coordinate c1 = new Coordinate(20,28);
			Coordinate c2 = new Coordinate(20,20);
			Coordinate c3 = new Coordinate(16,20);
			Coordinate c4 = new Coordinate(16,28);
			Coordinate[] coords = {c0,c1,c2,c3,c4};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb01 = {Id.create("sec4", Section.class)};
			Section sec = env.createAndAddSection(Id.create("sec3", Section.class), p, open, nb01 , 0);
			sec.addRelatedLinkId(id3);
			sec.addRelatedLinkId(id3Rev);
		}
		{
			int[] open = {0,1,2,3};
			GeometryFactory geofac = new GeometryFactory();
			Coordinate c0 = new Coordinate(16,16);
			Coordinate c1 = new Coordinate(16,20);
			Coordinate c2 = new Coordinate(20,20);
			Coordinate c3 = new Coordinate(20,16);
			Coordinate c4 = new Coordinate(16,16);
			Coordinate[] coords = {c0,c1,c2,c3,c4};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb01 = {Id.create("sec0", Section.class),Id.create("sec1", Section.class),Id.create("sec2", Section.class),Id.create("sec3", Section.class)};
			Section sec = env.createAndAddSection(Id.create("sec4", Section.class), p, open, nb01 , 0);
//			sec.addRelatedLinkId(id0);
//			sec.addRelatedLinkId(id0Rev);
//			sec.addRelatedLinkId(id1);
//			sec.addRelatedLinkId(id1Rev);
//			sec.addRelatedLinkId(id2);
//			sec.addRelatedLinkId(id2Rev);
//			sec.addRelatedLinkId(id3);
//			sec.addRelatedLinkId(id3Rev);
		}
		

	}

	
	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node m0 = fac.createNode(Id.create("m0", Node.class), new CoordImpl(0,18));
		Node m1 = fac.createNode(Id.create("m1", Node.class), new CoordImpl(4,18));
		
		Node n0 = fac.createNode(Id.create("n0", Node.class), new CoordImpl(8,18));
		
		Node n2 = fac.createNode(Id.create("n2", Node.class), new CoordImpl(28,18));
		
		Node m2 = fac.createNode(Id.create("m2", Node.class), new CoordImpl(32,18));
		Node m3 = fac.createNode(Id.create("m3", Node.class), new CoordImpl(36,18));
		
		Node m4 = fac.createNode(Id.create("m4", Node.class), new CoordImpl(18,0));
		Node m5 = fac.createNode(Id.create("m5", Node.class), new CoordImpl(18,4));
		
		Node n3 = fac.createNode(Id.create("n3", Node.class), new CoordImpl(18,8));
		
		Node n4 = fac.createNode(Id.create("n4", Node.class), new CoordImpl(18,28));
		
		Node m6 = fac.createNode(Id.create("m6", Node.class), new CoordImpl(18,32));
		Node m7 = fac.createNode(Id.create("m7", Node.class), new CoordImpl(18,36));
		
		net.addNode(m0);
		net.addNode(m1);
		net.addNode(m2);
		net.addNode(m3);
		net.addNode(m4);
		net.addNode(m5);
		net.addNode(m6);
		net.addNode(m7);
		net.addNode(n0);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);
		double flow = SEPC_FLOW * 4;
		
		
		Link k0 = fac.createLink(Id.create("k0", Link.class), m0, m1);
		Link k1 = fac.createLink(Id.create("k1", Link.class), m1, n0);

		Link k0Rev = fac.createLink(Id.create("k0_rev", Link.class), m1, m0);
		Link k1Rev = fac.createLink(Id.create("k1_rev", Link.class), n0, m1);
		
		Link k2 = fac.createLink(Id.create("k2", Link.class), n2, m2);
		Link k3 = fac.createLink(Id.create("k3", Link.class), m2, m3);

		Link k2Rev = fac.createLink(Id.create("k2_rev", Link.class), m2, n2);
		Link k3Rev = fac.createLink(Id.create("k3_rev", Link.class), m3, m2);
		
		Link k4 = fac.createLink(Id.create("k4", Link.class), m4, m5);
		Link k5 = fac.createLink(Id.create("k5", Link.class), m5, n3);

		Link k4Rev = fac.createLink(Id.create("k4_rev", Link.class), m5, m4);
		Link k5Rev = fac.createLink(Id.create("k5_rev", Link.class), n3, m5);
		
		Link k6 = fac.createLink(Id.create("k6", Link.class), n4, m6);
		Link k7 = fac.createLink(Id.create("k7", Link.class), m6, m7);
		
		Link k6Rev = fac.createLink(Id.create("k6_rev", Link.class), m6, n4);
		Link k7Rev = fac.createLink(Id.create("k7_rev", Link.class), m7, m6);
		
		Set<String> modes = new HashSet<String>();
		modes.add("walk");modes.add("car");
		k0.setLength(4);
		k1.setLength(4);
		k2.setLength(4);
		k3.setLength(4);
		k4.setLength(4);
		k5.setLength(4);
		k6.setLength(4);
		k7.setLength(4);
		k0Rev.setLength(4);
		k1Rev.setLength(4);
		k2Rev.setLength(4);
		k3Rev.setLength(4);
		k4Rev.setLength(4);
		k5Rev.setLength(4);
		k6Rev.setLength(4);
		k7Rev.setLength(4);		
		
		

		k0.setAllowedModes(modes);
		k1.setAllowedModes(modes);
		k2.setAllowedModes(modes);
		k3.setAllowedModes(modes);
		k4.setAllowedModes(modes);
		k5.setAllowedModes(modes);
		k6.setAllowedModes(modes);
		k7.setAllowedModes(modes);

		k0Rev.setAllowedModes(modes);
		k1Rev.setAllowedModes(modes);
		k2Rev.setAllowedModes(modes);
		k3Rev.setAllowedModes(modes);
		k4Rev.setAllowedModes(modes);
		k5Rev.setAllowedModes(modes);
		k6Rev.setAllowedModes(modes);
		k7Rev.setAllowedModes(modes);
		

		k0.setFreespeed(1.34);
		k1.setFreespeed(1.34);
		k2.setFreespeed(1.34);
		k3.setFreespeed(1.34);
		k4.setFreespeed(1.34);
		k5.setFreespeed(1.34);
		k6.setFreespeed(1.34);
		k7.setFreespeed(1.34);
		
		k0Rev.setFreespeed(1.34);
		k1Rev.setFreespeed(1.34);
		k2Rev.setFreespeed(1.34);
		k3Rev.setFreespeed(1.34);
		k4Rev.setFreespeed(1.34);
		k5Rev.setFreespeed(1.34);
		k6Rev.setFreespeed(1.34);
		k7Rev.setFreespeed(1.34);
		
		
		k0.setCapacity(flow);
		k1.setCapacity(flow);
		k2.setCapacity(flow);
		k3.setCapacity(flow);
		k4.setCapacity(flow);
		k5.setCapacity(flow);
		k6.setCapacity(flow);
		k7.setCapacity(flow);
		
		k0Rev.setCapacity(flow);
		k1Rev.setCapacity(flow);
		k2Rev.setCapacity(flow);
		k3Rev.setCapacity(flow);
		k4Rev.setCapacity(flow);
		k5Rev.setCapacity(flow);
		k6Rev.setCapacity(flow);
		k7Rev.setCapacity(flow);
		
		double lanes = 4/0.71;
		k0.setNumberOfLanes(lanes);
		k1.setNumberOfLanes(lanes);
		k2.setNumberOfLanes(lanes);
		k3.setNumberOfLanes(lanes);
		k4.setNumberOfLanes(lanes);
		k5.setNumberOfLanes(lanes);
		k6.setNumberOfLanes(lanes);
		k7.setNumberOfLanes(lanes);
		
		k0Rev.setNumberOfLanes(lanes);
		k1Rev.setNumberOfLanes(lanes);
		k2Rev.setNumberOfLanes(lanes);
		k3Rev.setNumberOfLanes(lanes);
		k4Rev.setNumberOfLanes(lanes);
		k5Rev.setNumberOfLanes(lanes);
		k6Rev.setNumberOfLanes(lanes);
		k7Rev.setNumberOfLanes(lanes);

		net.addLink(k0);
		net.addLink(k1);
		net.addLink(k2);
		net.addLink(k3);
		net.addLink(k4);
		net.addLink(k5);
		net.addLink(k6);
		net.addLink(k7);
		
		net.addLink(k0Rev);
		net.addLink(k1Rev);
		net.addLink(k2Rev);
		net.addLink(k3Rev);
		net.addLink(k4Rev);
		net.addLink(k5Rev);
		net.addLink(k6Rev);
		net.addLink(k7Rev);

		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveCellSize(.26);
		((NetworkImpl)net).setEffectiveLaneWidth(.71);
	}


}
