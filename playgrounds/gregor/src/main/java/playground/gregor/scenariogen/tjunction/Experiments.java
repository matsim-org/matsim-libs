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

package playground.gregor.scenariogen.tjunction;

import java.util.HashSet;
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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
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

public class Experiments {
	private static String inputDir = "/Users/laemmel/devel/tjunction/input";
	private static String outputDir = "/Users/laemmel/devel/tjunction/output";

	private static final double Bcor2 = 2.4;
	private static final double Bcor1 = 2.4;
	private static final double Bentr = 2.4;


	private static final int nrAgents = 200;

	public static final double MAX_FLOW =1.2*3600;//1.2;//1.2;
	private static final int DPETH = 0;
	

	public static void main(String [] args) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);

		Sim2DConfig s2d = Sim2DConfigUtils.createConfig();
		s2d.setTimeStepSize(0.04);
		Sim2DScenario s2dsc = Sim2DScenarioUtils.createSim2dScenario(s2d);
		create2DWorld(s2dsc);
		createNetwork(sc);


		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(.71);
		((NetworkImpl)sc.getNetwork()).setCapacityPeriod(3600);
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
		c.strategy().addParam("ModuleDisableAfterIteration_1", "100");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".9");

		c.controler().setOutputDirectory(outputDir);
		c.controler().setLastIteration(200);

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
		qsim.setEndTime(20*60);
		c.controler().setMobsim("hybridQ2D");
		c.global().setCoordinateSystem("EPSG:3395");

		c.qsim().setEndTime(60*10);

		new ConfigWriter(c).write(inputDir+ "/config.xml");

		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());

		createPopulation(sc);

		Population pop = sc.getPopulation();
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans().getInputFile());

	}

	

	private static void createPopulation(Scenario sc) {
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 0;
		for (int i = 0; i < nrAgents/2; i++) {
			Person pers = fac.createPerson(new IdImpl("b"+i));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", new IdImpl("l0"));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", new IdImpl("l8"));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}
		for (int i = nrAgents/2; i < nrAgents; i++) {
			Person pers = fac.createPerson(new IdImpl("a"+i));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", new IdImpl("l2"));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", new IdImpl("l8"));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}
	}

	private static void create2DWorld(Sim2DScenario sc2) {
		Sim2DEnvironment env = new Sim2DEnvironment();
		env.setId(new IdImpl("env0"));
		env.setEnvelope(new Envelope(-5,4,-Bcor1,4));
		try {
			env.setCRS(CRS.decode("EPSG:3395"));
		} catch (NoSuchAuthorityCodeException e) {
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sc2.addSim2DEnvironment(env);


		GeometryFactory geofac = new GeometryFactory();

		NetworkImpl net = NetworkImpl.createNetwork();
		env.setNetwork(net);
		NetworkFactoryImpl fac = net.getFactory();
		Section s0,s1;
		{
			double x0 = -5;
			double y0 = -Bcor1;		
			int[] open = {0,2,4};
			Coordinate c0 = new Coordinate(x0,y0);
			y0 = 0;
			Coordinate c1 = new Coordinate(x0,y0);
			x0 = -Bcor2;
			Coordinate c2 = new Coordinate(x0,y0);
			x0 = 0;
			Coordinate c3 = new Coordinate(x0,y0);
			x0 = 4;
			Coordinate c4 = new Coordinate(x0,y0);			
			y0 = -Bcor1;
			Coordinate c5 = new Coordinate(x0,y0);			
			x0 = -5;
			Coordinate c6 = new Coordinate(x0,y0);
			Coordinate[] coords = {c0,c1,c2,c3,c4,c5,c6};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb = new Id[]{new IdImpl("s1")};
			s0 = env.createAndAddSection(new IdImpl("s0"), p, open, nb , 0);//FIXME make neighbors
		}
		{
			double x0 = -Bcor2;
			double y0 =  0;		
			int[] open = {1,3};
			Coordinate c0 = new Coordinate(x0,y0);
			y0 = 4;
			Coordinate c1 = new Coordinate(x0,y0);
			x0 = 0;
			Coordinate c2 = new Coordinate(x0,y0);
			y0 = 0;
			Coordinate c3 = new Coordinate(x0,y0);
			x0 = -Bcor2;
			Coordinate c4 = new Coordinate(x0,y0);
			
			Coordinate[] coords = {c0,c1,c2,c3,c4};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb = new Id[]{new IdImpl("s0")};
			s1 = env.createAndAddSection(new IdImpl("s1"), p, open, nb , 0);//FIXME make neighbors
		}


		NodeImpl n2 = fac.createNode(new IdImpl("n2"), new CoordImpl(-5,-Bcor1/2));
		NodeImpl n3 = fac.createNode(new IdImpl("n3"), new CoordImpl(-Bcor2/2,0));
		NodeImpl n4 = fac.createNode(new IdImpl("n4"), new CoordImpl(4,-Bcor1/2));
		NodeImpl n5 = fac.createNode(new IdImpl("n5"), new CoordImpl(-Bcor2/2,4));
		NodeImpl n9 = fac.createNode(new IdImpl("n9"), new CoordImpl(24,-2));

		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);
		net.addNode(n5);
		net.addNode(n9);
		
		{
			double dx = -5 - (-Bcor2/2);
			double dy = Bcor1/2;
			double length = Math.sqrt(dx*dx+dy*dy);
			Link l = fac.createLink(new IdImpl("l4"), n2, n3);
			double flow = Bcor1 *MAX_FLOW;
			l.setFreespeed(1.34);
			l.setLength(length);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s0.addRelatedLinkId(l.getId());
		}
		{
			double dx = 4 + (-Bcor2/2);
			double dy = Bcor1/2;
			double length = Math.sqrt(dx*dx+dy*dy);
			Link l = fac.createLink(new IdImpl("l5"), n4, n3);
			double flow = Bcor1 *MAX_FLOW;
			l.setFreespeed(1.34);
			l.setLength(length);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s0.addRelatedLinkId(l.getId());
		}
		{
			double dx = 0;
			double dy = 4;
			double length = Math.sqrt(dx*dx+dy*dy);
			Link l = fac.createLink(new IdImpl("l6"), n3, n5);
			double flow = Bcor2 *MAX_FLOW;
			l.setFreespeed(1.34);
			l.setLength(length);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s1.addRelatedLinkId(l.getId());
		}

	}
	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(new IdImpl("n0"), new CoordImpl(-20,-Bcor1/2));
		Node n1 = fac.createNode(new IdImpl("n1"), new CoordImpl(-10,-Bcor1/2));
		Node n2 = fac.createNode(new IdImpl("n2"), new CoordImpl(-5,-Bcor1/2));
		Node n4 = fac.createNode(new IdImpl("n4"), new CoordImpl(4,-Bcor1/2));
		Node n6 = fac.createNode(new IdImpl("n6"), new CoordImpl(9,-Bcor1/2));
		Node n7 = fac.createNode(new IdImpl("n7"), new CoordImpl(19,-Bcor1/2));
		Node n5 = fac.createNode(new IdImpl("n5"), new CoordImpl(-Bcor2/2,4));
		Node n8 = fac.createNode(new IdImpl("n8"), new CoordImpl(-Bcor2/2,9));
		Node n9 = fac.createNode(new IdImpl("n9"), new CoordImpl(-Bcor2/2,19));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n4);
		net.addNode(n5);
		net.addNode(n6);
		net.addNode(n7);
		net.addNode(n8);
		net.addNode(n9);
		double flow = MAX_FLOW * Bentr;
		Link l0 = fac.createLink(new IdImpl("l0"), n0, n1);
		Link l1 = fac.createLink(new IdImpl("l1"), n1, n2);
		Link l2 = fac.createLink(new IdImpl("l2"), n7, n6);
		Link l3 = fac.createLink(new IdImpl("l3"), n6, n4);
		Link l7 = fac.createLink(new IdImpl("l7"), n5, n8);
		Link l8 = fac.createLink(new IdImpl("l8"), n8, n9);
		
		Link l0Rev = fac.createLink(new IdImpl("l0_rev"), n1, n0);
		Link l1Rev = fac.createLink(new IdImpl("l1_rev"), n2, n1);
		Link l2Rev = fac.createLink(new IdImpl("l2_rev"), n6, n7);
		Link l3Rev = fac.createLink(new IdImpl("l3_rev"), n4, n6);
		Link l7Rev = fac.createLink(new IdImpl("l7_rev"), n8, n5);
		Link l8Rev = fac.createLink(new IdImpl("l8_rev"), n9, n8);		
		
		
		Set<String> modes = new HashSet<String>();
		modes.add("walk");modes.add("car");
		l0.setLength(10);
		l1.setLength(5);
		l2.setLength(10);
		l3.setLength(5);
		l7.setLength(5);
		l8.setLength(10);
		
		l0Rev.setLength(10);
		l1Rev.setLength(5);
		l2Rev.setLength(10);
		l3Rev.setLength(5);
		l7Rev.setLength(5);
		l8Rev.setLength(10);

		l0.setAllowedModes(modes);
		l1.setAllowedModes(modes);
		l2.setAllowedModes(modes);
		l3.setAllowedModes(modes);
		l7.setAllowedModes(modes);
		l8.setAllowedModes(modes);

		l0Rev.setAllowedModes(modes);
		l1Rev.setAllowedModes(modes);
		l2Rev.setAllowedModes(modes);
		l3Rev.setAllowedModes(modes);
		l7Rev.setAllowedModes(modes);
		l8Rev.setAllowedModes(modes);
		
		l0.setFreespeed(1.34);
		l1.setFreespeed(1.34);
		l2.setFreespeed(1.34);
		l3.setFreespeed(1.34);
		l7.setFreespeed(1.34);
		l8.setFreespeed(1.34);

		l0Rev.setFreespeed(1.34);
		l1Rev.setFreespeed(1.34);
		l2Rev.setFreespeed(1.34);
		l3Rev.setFreespeed(1.34);
		l7Rev.setFreespeed(1.34);
		l8Rev.setFreespeed(1.34);
		
		l0.setCapacity(flow);
		l1.setCapacity(flow);
		l2.setCapacity(flow);
		l3.setCapacity(flow);
		l7.setCapacity(flow*10);
		l8.setCapacity(flow*10);

		l0Rev.setCapacity(flow);
		l1Rev.setCapacity(flow);
		l2Rev.setCapacity(flow);
		l3Rev.setCapacity(flow);
		l7Rev.setCapacity(flow*10);
		l8Rev.setCapacity(flow*10);
		
		double lanes = Bentr/0.71;
		l0.setNumberOfLanes(lanes);
		l1.setNumberOfLanes(lanes);
		l2.setNumberOfLanes(lanes);
		l3.setNumberOfLanes(lanes);
		l7.setNumberOfLanes(lanes);
		l8.setNumberOfLanes(lanes);
		
		l0Rev.setNumberOfLanes(lanes);
		l1Rev.setNumberOfLanes(lanes);
		l2Rev.setNumberOfLanes(lanes);
		l3Rev.setNumberOfLanes(lanes);
		l7Rev.setNumberOfLanes(lanes);
		l8Rev.setNumberOfLanes(lanes);
		
		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		net.addLink(l7);
		net.addLink(l8);
		
		
		net.addLink(l0Rev);
		net.addLink(l1Rev);
		net.addLink(l2Rev);
		net.addLink(l3Rev);
		net.addLink(l7Rev);
		net.addLink(l8Rev);

		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveCellSize(.26);
		((NetworkImpl)net).setEffectiveLaneWidth(.71);
	}
}
