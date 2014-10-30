/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioGenerator.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
import org.matsim.core.network.NodeImpl;
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

public class BiPedExperiments {
	private static String inputDir = "/Users/laemmel/devel/bipedexp/input";
	private static String outputDir = "/Users/laemmel/devel/bipedexp/output";


//	private static final double WIDTH = 15;//Lavalampe
//	private static final double OUTER_RADIUS = 35;
	private static final double WIDTH = 10;
	private static final double OUTER_RADIUS = 30;
//	private static final double WIDTH = 2.4;
//	private static final double OUTER_RADIUS = 30;
	
	private static double SPLITTING = .5;
	
	
	private static final double ALPHA_INCR = Math.PI/32;
	
	private final static double LENGTH = 10;
	
	private static final boolean uni = false;

//	private static final int nrAgents = 900; //Lavalampe
	private static final int nrAgents = 1200;

	public static final double MAX_FLOW =.6;//.8;//1.2;//1.2;
	private static final int MAX_ROUNDS = 40;

	public static void main(String [] args) {
		if (args.length == 1) {
			SPLITTING = Double.parseDouble(args[0]);
		}
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);

		Sim2DConfig s2d = Sim2DConfigUtils.createConfig();
		s2d.setTimeStepSize(0.04);
		Sim2DScenario s2dsc = Sim2DScenarioUtils.createSim2dScenario(s2d);
		
		int lastNode = create2DWorld(s2dsc);
		createNetwork(sc,lastNode);


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
		//		qsim.setMainModes(Arrays.asList(new String[]{"walk"}));
		//		Collection<String> modes =  qsim.getMainMode();
		//		modes.add("walk");
		c.controler().setMobsim("hybridQ2D");

		c.global().setCoordinateSystem("EPSG:3395");

//		createNetworkChangeEvents(s2dsc,sc);

		
		c.qsim().setEndTime(60*80);

		new ConfigWriter(c).write(inputDir+ "/config.xml");

		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());

		createPopulation(sc);

		Population pop = sc.getPopulation();
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans().getInputFile());

	}

	private static void createNetworkChangeEvents(Sim2DScenario s2dsc, Scenario sc) {
		Collection<NetworkChangeEvent> events = new LinkedList<NetworkChangeEvent>();
		NetworkChangeEventFactoryImpl fac = new NetworkChangeEventFactoryImpl();
		{
		NetworkChangeEvent e = fac.createNetworkChangeEvent(30*60);
		ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0.01);
		e.setFreespeedChange(cv);
		for (Link l : s2dsc.getSim2DEnvironments().iterator().next().getEnvironmentNetwork().getLinks().values()){
			e.addLink(l);
		}
		events.add(e);
		}
		{
			NetworkChangeEvent e = fac.createNetworkChangeEvent(30*60+20);
			ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 1.34);
			e.setFreespeedChange(cv);
			for (Link l : s2dsc.getSim2DEnvironments().iterator().next().getEnvironmentNetwork().getLinks().values()){
				e.addLink(l);
			}
			events.add(e);
		}
		
		new NetworkChangeEventsWriter().write(inputDir +"/networkChangeEvents.xml.gz", events);
		sc.getConfig().network().setTimeVariantNetwork(true);
		sc.getConfig().network().setChangeEventInputFile(inputDir+"/networkChangeEvents.xml.gz");

	}

	private static void createPopulation(Scenario sc) {
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 0;
		for (int i = 0; i < nrAgents *SPLITTING; i++) {
			Person pers = fac.createPerson(Id.create("b"+i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", Id.create("l0", Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", Id.create("l3", Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}

		if (uni) {
			return;
		}
		for (int i = (int)(nrAgents*SPLITTING+0.5); i < nrAgents; i++) {
			Person pers = fac.createPerson(Id.create("d"+i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", Id.create("l3_rev", Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", Id.create("l0_rev", Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}

	}

	

	private static int create2DWorld(Sim2DScenario sc2) {
		Sim2DEnvironment env = new Sim2DEnvironment();
		env.setId(Id.create("env0", Sim2DEnvironment.class));
		env.setEnvelope(new Envelope(-OUTER_RADIUS,OUTER_RADIUS,-OUTER_RADIUS,OUTER_RADIUS));
		try {
			env.setCRS(CRS.decode("EPSG:3395"));
		} catch (NoSuchAuthorityCodeException e) {
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sc2.addSim2DEnvironment(env);
		
		double x0 = 0;
		double y0 = OUTER_RADIUS-WIDTH;
		double x1 = 0;
		double y1 = y0+WIDTH;
		GeometryFactory geofac = new GeometryFactory();
		
		NetworkImpl net = NetworkImpl.createNetwork();
		env.setNetwork(net);
		NetworkFactoryImpl fac = net.getFactory();
		int secId = 0;
		List<Id<Section>> secIds = new ArrayList<>();
		for (double alpha = ALPHA_INCR; alpha <= 2*Math.PI+ALPHA_INCR-0.001; alpha+=ALPHA_INCR) {
			secIds.add(Id.create("sec_angle"+alpha+"_nr"+secId++, Section.class));
		}
		secId = 0;
		for (double alpha = ALPHA_INCR; alpha <= 2*Math.PI+ALPHA_INCR-0.001; alpha+=ALPHA_INCR) {
			int[] open = {0,2};
			Coordinate c0 = new Coordinate(x0,y0);
			Coordinate c1 = new Coordinate(x1,y1);
			double x2 = c1.x*Math.cos(-ALPHA_INCR) - c1.y*Math.sin(-ALPHA_INCR);
			double y2 = c1.x*Math.sin(-ALPHA_INCR) + c1.y*Math.cos(-ALPHA_INCR);
			double x3 = c0.x*Math.cos(-ALPHA_INCR) - c0.y*Math.sin(-ALPHA_INCR);
			double y3 = c0.x*Math.sin(-ALPHA_INCR) + c0.y*Math.cos(-ALPHA_INCR);
			if (secId == secIds.size()-1) {
				x2=0;
				y2=OUTER_RADIUS;
				x3=0;
				y3=OUTER_RADIUS-WIDTH;
			}
			Coordinate c2 = new Coordinate(x2,y2);
			Coordinate c3 = new Coordinate(x3,y3);
			Coordinate c4 = new Coordinate(x0,y0);
			Coordinate[] coords = {c0,c1,c2,c3,c4};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id<Section>[] nb = new Id[2];
			if (secId == 0) {
				nb[0]=secIds.get(secIds.size()-1);
				nb[1]=secIds.get(secId+1);
 			} else if (secId == secIds.size()-1) {
				nb[0]=secIds.get(secId-1);
				nb[1]=secIds.get(0);
 			} else {
				nb[0]=secIds.get(secId-1);
				nb[1]=secIds.get(secId+1);
 			}
			Section sec = env.createAndAddSection(secIds.get(secId), p, open, nb , 0);//FIXME make neighbors
			secId++;
			x0 = x3;
			y0 = y3;
			x1 = x2;
			y1 = y2;
		}


		int nodeId = 0;
		double x = 0;
		double y = OUTER_RADIUS-WIDTH/2;
		double xx = x*Math.cos(-ALPHA_INCR/10) - y*Math.sin(-ALPHA_INCR/10);
		double yy = x*Math.sin(-ALPHA_INCR/10) + y*Math.cos(-ALPHA_INCR/10);
		NodeImpl n0 = fac.createNode(Id.create(nodeId++, Node.class), new CoordImpl(xx,yy));
		net.addNode(n0);
		for (int round = 0; round < MAX_ROUNDS; round++) {
			x = 0;
			y = OUTER_RADIUS-WIDTH/2;
			secId = 0;
			for (double alpha = ALPHA_INCR; alpha <= 2*Math.PI+ALPHA_INCR-0.001; alpha+=ALPHA_INCR) {
					double x2 = x*Math.cos(-ALPHA_INCR) - y*Math.sin(-ALPHA_INCR);
					double y2 = x*Math.sin(-ALPHA_INCR) + y*Math.cos(-ALPHA_INCR);
				if (secId == secIds.size()-1 && round == MAX_ROUNDS-1) {
					x2 = x*Math.cos(-ALPHA_INCR+ALPHA_INCR/10) - y*Math.sin(-ALPHA_INCR+ALPHA_INCR/10);
					y2 = x*Math.sin(-ALPHA_INCR+ALPHA_INCR/10) + y*Math.cos(-ALPHA_INCR+ALPHA_INCR/10);
					
				}
				double dx = x2-x;
				double dy = y2-y;
				double length = Math.sqrt(dx*dx+dy*dy);
				
				NodeImpl n1 = fac.createNode(Id.create(nodeId++, Node.class), new CoordImpl(x2,y2));
				System.err.println(n1.getId());
				net.addNode(n1);
				
				Id<Section> secIdId = secIds.get(secId++);
				Id<Link> id = Id.create("l_round"+round+"_"+secIdId, Link.class);
				Link l = fac.createLink(id, n0, n1);
		
				double flow = WIDTH *MAX_FLOW;
				l.setFreespeed(1.34);
//				if (round == MAX_ROUNDS/2 && alpha == ALPHA_INCR) {
//					l.setFreespeed(0.5);
//				}
				l.setLength(length);
				l.setCapacity(flow);;
				Set<String> modes = new HashSet<String>();
				modes.add("walk2d");modes.add("walk");modes.add("car");
				l.setAllowedModes(modes);
				net.addLink(l);
		
				Id<Link> idRev = Id.create("l_rev_round"+round+"_"+secIdId, Link.class);
				Link lRev = fac.createLink(idRev, n1, n0);
				lRev.setFreespeed(1.34);
				lRev.setLength(length);
				lRev.setAllowedModes(modes);
				lRev.setCapacity(flow);
				net.addLink(lRev);
				
				Section sec = env.getSections().get(secIdId);
				sec.addRelatedLinkId(id);
				sec.addRelatedLinkId(idRev);
				
				
				x = x2;
				y = y2;
				n0 = n1;
			}
//			nodeId--;
		}
		return nodeId-1;

	}
	private static void createNetwork(Scenario sc,int lastNode) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(Id.create(-2, Node.class), new CoordImpl(-25,OUTER_RADIUS-WIDTH/2));
		Node n1 = fac.createNode(Id.create(-1, Node.class), new CoordImpl(0.1,OUTER_RADIUS-WIDTH/2+0.001));
		Node n2 = fac.createNode(Id.create(0, Node.class), new CoordImpl(0.5,OUTER_RADIUS-WIDTH/2));
		Node n3 = fac.createNode(Id.create(lastNode, Node.class), new CoordImpl(-.5,OUTER_RADIUS-WIDTH/2));
		Node n4 = fac.createNode(Id.create(lastNode+1, Node.class), new CoordImpl(-0.1,OUTER_RADIUS-WIDTH/2+0.001));
		Node n5 = fac.createNode(Id.create(lastNode+2, Node.class), new CoordImpl(25,OUTER_RADIUS-WIDTH/2));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);
		net.addNode(n5);
		double flow = MAX_FLOW * WIDTH;
		Link l0 = fac.createLink(Id.create("l0", Link.class), n0, n1);
		Link l1 = fac.createLink(Id.create("l1", Link.class), n1, n2);
		Link l2 = fac.createLink(Id.create("l2", Link.class), n3, n4);
		Link l3 = fac.createLink(Id.create("l3", Link.class), n4, n5);

		Link l0Rev = fac.createLink(Id.create("l0_rev", Link.class), n1, n0);
		Link l1Rev = fac.createLink(Id.create("l1_rev", Link.class), n2, n1);
		Link l2Rev = fac.createLink(Id.create("l2_rev", Link.class), n4, n3);
		Link l3Rev = fac.createLink(Id.create("l3_rev", Link.class), n5, n4);

		Set<String> modes = new HashSet<String>();
		modes.add("walk");modes.add("car");
		l0.setLength(10);
		l1.setLength(15);
		l2.setLength(15);
		l3.setLength(10);

		l0Rev.setLength(10);
		l1Rev.setLength(15);
		l2Rev.setLength(15);
		l3Rev.setLength(10);

		l0.setAllowedModes(modes);
		l1.setAllowedModes(modes);
		l2.setAllowedModes(modes);
		l3.setAllowedModes(modes);

		l0Rev.setAllowedModes(modes);
		l1Rev.setAllowedModes(modes);
		l2Rev.setAllowedModes(modes);
		l3Rev.setAllowedModes(modes);

		l0.setFreespeed(1.34);
		l1.setFreespeed(1.34);
		l2.setFreespeed(1.34);
		l3.setFreespeed(1.34);

		l0Rev.setFreespeed(1.34);
		l1Rev.setFreespeed(1.34);
		l2Rev.setFreespeed(1.34);
		l3Rev.setFreespeed(1.34);

		l0.setCapacity(flow*SPLITTING);
		l1.setCapacity(flow*SPLITTING);
		l2.setCapacity(flow*SPLITTING);
		l3.setCapacity(flow*SPLITTING);

		l0Rev.setCapacity(flow*(1-SPLITTING));
		l1Rev.setCapacity(flow*(1-SPLITTING));
		l2Rev.setCapacity(flow*(1-SPLITTING));
		l3Rev.setCapacity(flow*(1-SPLITTING));

		double lanes = WIDTH/0.71;
		l0.setNumberOfLanes(lanes);
		l1.setNumberOfLanes(lanes);
		l2.setNumberOfLanes(lanes);
		l3.setNumberOfLanes(lanes);

		l0Rev.setNumberOfLanes(lanes);
		l1Rev.setNumberOfLanes(lanes);
		l2Rev.setNumberOfLanes(lanes);
		l3Rev.setNumberOfLanes(lanes);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);

		net.addLink(l0Rev);
		net.addLink(l1Rev);
		net.addLink(l2Rev);
		net.addLink(l3Rev);

		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveCellSize(.26);
		((NetworkImpl)net).setEffectiveLaneWidth(.71);
	}
}
