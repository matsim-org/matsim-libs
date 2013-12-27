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

package playground.gregor.scenariogen.iterations;

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
import org.matsim.core.basic.v01.IdImpl;
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

public class Experiments {
	private static String inputDir = "/Users/laemmel/devel/iterations/input";
	private static String outputDir = "/Users/laemmel/devel/iterations/output";

	private static final double WIDTH = 4;
	private static final double BOTTLENECK = .8;


	private static final int nrAgents = 200;

	public static final double MAX_FLOW =0.4;//1.2;//1.2;

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
		qsim.setEndTime(4*3600);
		c.controler().setMobsim("hybridQ2D");
		c.global().setCoordinateSystem("EPSG:3395");

		c.qsim().setEndTime(60*10);

		new ConfigWriter(c).write(inputDir+ "/config.xml");

		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());

		createPopulation(sc);

		Population pop = sc.getPopulation();
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans().getInputFile());

	}

	private static void createNetworkChangeEvents(Scenario sc) {
		Collection<NetworkChangeEvent> events = new LinkedList<NetworkChangeEvent>();
		NetworkChangeEventFactoryImpl fac = new NetworkChangeEventFactoryImpl();
		double incr = (WIDTH*MAX_FLOW)/10;
		double flowCap = incr;
double f1 = flowCap;
double f2 = flowCap;
		for (double time = 0; time < 120*100; time += 30) {



			NetworkChangeEvent e = fac.createNetworkChangeEvent(time);
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l0")));
			ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, f1);
			e.setFlowCapacityChange(cv);
			events.add(e);
			//			flowCap += incr;

			NetworkChangeEvent ee = fac.createNetworkChangeEvent(time);
			ee.addLink(sc.getNetwork().getLinks().get(new IdImpl("l3_rev")));
			ChangeValue cve = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, f2);
			ee.setFlowCapacityChange(cve);
			events.add(ee);

			//			double tmp = f1;
			//			f1=f2;
			//			f2=tmp;
			f1 += incr;
			f2 += incr;
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
		for (int i = 0; i < nrAgents; i++) {
			Person pers = fac.createPerson(new IdImpl("b"+i));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", new IdImpl("l0"));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", new IdImpl("l3"));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}
	}

	private static void create2DWorld(Sim2DScenario sc2) {
		Sim2DEnvironment env = new Sim2DEnvironment();
		env.setId(new IdImpl("env0"));
		env.setEnvelope(new Envelope(0,30,0,-10));
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
		Section s0,s1,s2,s3,s4,s5,s6,s7;
		{
			double x0 = 0;
			double y0 = -4;		
			int[] open = {0,2};
			Coordinate c0 = new Coordinate(x0,y0);
			y0 = 0;
			Coordinate c1 = new Coordinate(x0,y0);
			x0 = 6;
			Coordinate c2 = new Coordinate(x0,y0);
			y0 = -4;
			Coordinate c3 = new Coordinate(x0,y0);
			x0 = 0;
			Coordinate c4 = new Coordinate(x0,y0);			

			Coordinate[] coords = {c0,c1,c2,c3,c4};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb = new Id[]{new IdImpl("s1")};
			s0 = env.createAndAddSection(new IdImpl("s0"), p, open, nb , 0);//FIXME make neighbors
		}
		{
			double x0 = 6;
			double y0 = -4;		
			int[] open = {0,2,4};
			Coordinate c0 = new Coordinate(x0,y0);
			y0 = 0;
			Coordinate c1 = new Coordinate(x0,y0);
			x0 = 10;
			Coordinate c2 = new Coordinate(x0,y0);
			y0 = -4;
			Coordinate c3 = new Coordinate(x0,y0);
			x0 = 9.8;
			Coordinate c31 = new Coordinate(x0,y0);
			x0 = 6.2;
			Coordinate c4 = new Coordinate(x0,y0);			
			x0 = 6;
			Coordinate c41 = new Coordinate(x0,y0);
			
			Coordinate[] coords = {c0,c1,c2,c3,c31,c4,c41};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb = new Id[]{new IdImpl("s0"),new IdImpl("s2"),new IdImpl("s5")};
//			Id[] nb = new Id[]{new IdImpl("s0"),new IdImpl("s2")};
			s1 = env.createAndAddSection(new IdImpl("s1"), p, open, nb , 0);//FIXME make neighbors
		}
		{

			double x0 = 10;
			double y0 = -4;		
			int[] open = {0,3};
			Coordinate c0 = new Coordinate(x0,y0);
			y0 = 0;
			Coordinate c1 = new Coordinate(x0,y0);
			x0 = 20;
			Coordinate c2 = new Coordinate(x0,y0);
//			x0 = 20;
			double d= (WIDTH-BOTTLENECK)/2;
			y0 -= d;
			Coordinate c3 = new Coordinate(x0,y0);
			y0 -= BOTTLENECK;
			Coordinate c4 = new Coordinate(x0,y0);			
			y0 = -4;
//			x0 = 19;
			Coordinate c5 = new Coordinate(x0,y0);
			x0 = 10;
			Coordinate c6 = new Coordinate(x0,y0);

			Coordinate[] coords = {c0,c1,c2,c3,c4,c5,c6};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb = new Id[]{new IdImpl("s1"),new IdImpl("s3")};
			s2 = env.createAndAddSection(new IdImpl("s2"), p, open, nb , 0);//FIXME make neighbors
		}
		{


			double x0 = 20;
			double y0 = -4;		
			int[] open = {1,4,5};
			Coordinate c0 = new Coordinate(x0,y0);
			double d= (WIDTH-BOTTLENECK)/2;
			y0 += d;
			Coordinate c1 = new Coordinate(x0,y0);
			y0 += BOTTLENECK;
			Coordinate c2 = new Coordinate(x0,y0);
			y0 = 0;
			Coordinate c3 = new Coordinate(x0,y0);
			x0 = 24;
			Coordinate c4 = new Coordinate(x0,y0);			
			y0 = -4;
			Coordinate c5 = new Coordinate(x0,y0);
			x0 = 20;
			Coordinate c6 = new Coordinate(x0,y0);
			Coordinate[] coords = {c0,c1,c2,c3,c4,c5,c6};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb = new Id[]{new IdImpl("s2"),new IdImpl("s4"),new IdImpl("s7")};
			s3 = env.createAndAddSection(new IdImpl("s3"), p, open, nb , 0);//FIXME make neighbors
		}
		{
			double x0 = 24;
			double y0 = -4;		
			int[] open = {0,2};
			Coordinate c0 = new Coordinate(x0,y0);
			y0 = 0;
			Coordinate c1 = new Coordinate(x0,y0);
			x0 = 30;
			Coordinate c2 = new Coordinate(x0,y0);
			y0 = -4;
			Coordinate c3 = new Coordinate(x0,y0);
			x0 = 24;
			Coordinate c4 = new Coordinate(x0,y0);			

			Coordinate[] coords = {c0,c1,c2,c3,c4};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb = new Id[]{new IdImpl("s3")};
			s4 = env.createAndAddSection(new IdImpl("s4"), p, open, nb , 0);//FIXME make neighbors
		}
		{
			double x0 = 6;
			double y0 = -4;		
			int[] open = {1,4};
			Coordinate c0 = new Coordinate(x0,y0);
			x0 = 6.2;
			Coordinate c01 = new Coordinate(x0,y0);
			x0 = 9.8;
			Coordinate c02 = new Coordinate(x0,y0);
			x0 = 10;
			Coordinate c1 = new Coordinate(x0,y0);
			y0 = -6;
			Coordinate c2 = new Coordinate(x0,y0);
			y0 = -10;
			Coordinate c3 = new Coordinate(x0,y0);
			x0 = 6;
			Coordinate c4 = new Coordinate(x0,y0);			
			y0 = -4;
			Coordinate c5 = new Coordinate(x0,y0);

			Coordinate[] coords = {c0,c01,c02,c1,c2,c3,c4,c5};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb = new Id[]{new IdImpl("s1"),new IdImpl("s6")};
			s5 = env.createAndAddSection(new IdImpl("s5"), p, open, nb , 0);//FIXME make neighbors
		}
		{
			double x0 = 10;
			double y0 = -10;		
			int[] open = {0,2};
			Coordinate c0 = new Coordinate(x0,y0);
			y0 = -6;
			Coordinate c1 = new Coordinate(x0,y0);
			x0 = 20;
			Coordinate c2 = new Coordinate(x0,y0);
			y0 = -10;
			Coordinate c3 = new Coordinate(x0,y0);
			x0 = 10;
			Coordinate c4 = new Coordinate(x0,y0);			

			Coordinate[] coords = {c0,c1,c2,c3,c4};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb = new Id[]{new IdImpl("s5"),new IdImpl("s7")};
//			Id[] nb = new Id[]{new IdImpl("s7")};
			s6 = env.createAndAddSection(new IdImpl("s6"), p, open, nb , 0);//FIXME make neighbors
		}
		{
			double x0 = 20;
			double y0 = -4;		
			int[] open = {0,3};
			Coordinate c0 = new Coordinate(x0,y0);
			x0 = 24;
			Coordinate c1 = new Coordinate(x0,y0);
			y0 = -10;
			Coordinate c2 = new Coordinate(x0,y0);
			x0 = 20;
			Coordinate c3 = new Coordinate(x0,y0);
			y0 = -6;
			Coordinate c4 = new Coordinate(x0,y0);			
			y0 = -4;
			Coordinate c5 = new Coordinate(x0,y0);

			Coordinate[] coords = {c0,c1,c2,c3,c4,c5};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Id[] nb = new Id[]{new IdImpl("s6"),new IdImpl("s3")};
			s7 = env.createAndAddSection(new IdImpl("s7"), p, open, nb , 0);//FIXME make neighbors
		}

		NodeImpl n2 = fac.createNode(new IdImpl("n2"), new CoordImpl(0,-2));
		NodeImpl n6 = fac.createNode(new IdImpl("n6"), new CoordImpl(6,-2));
		NodeImpl n7 = fac.createNode(new IdImpl("n7"), new CoordImpl(10,-2));
		NodeImpl n8 = fac.createNode(new IdImpl("n8"), new CoordImpl(20,-2));
		NodeImpl n9 = fac.createNode(new IdImpl("n9"), new CoordImpl(24,-2));
		NodeImpl n10 = fac.createNode(new IdImpl("n10"), new CoordImpl(8,-4));
		NodeImpl n11 = fac.createNode(new IdImpl("n11"), new CoordImpl(10,-8));
		NodeImpl n12 = fac.createNode(new IdImpl("n12"), new CoordImpl(20,-8));
		NodeImpl n13 = fac.createNode(new IdImpl("n13"), new CoordImpl(22,-4));
		NodeImpl n3 = fac.createNode(new IdImpl("n3"), new CoordImpl(30,-2));

		net.addNode(n2);
		net.addNode(n6);
		net.addNode(n7);
		net.addNode(n8);
		net.addNode(n9);
		net.addNode(n10);
		net.addNode(n11);
		net.addNode(n12);
		net.addNode(n13);
		net.addNode(n3);
		{
			Link l = fac.createLink(new IdImpl("l4"), n2, n6);
			double flow = WIDTH *MAX_FLOW;
			l.setFreespeed(1.34);
			l.setLength(6);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s0.addRelatedLinkId(l.getId());
		}
		{
			Link l = fac.createLink(new IdImpl("l5"), n6, n7);
			double flow = WIDTH *MAX_FLOW;
			l.setFreespeed(1.34);
			l.setLength(4);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s1.addRelatedLinkId(l.getId());
		}
		{
			Link l = fac.createLink(new IdImpl("l6"), n7, n8);
			double flow = WIDTH *MAX_FLOW;
			l.setFreespeed(1.34);
			l.setLength(10);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s2.addRelatedLinkId(l.getId());
		}
		{
			Link l = fac.createLink(new IdImpl("l7"), n8, n9);
			double flow = WIDTH *MAX_FLOW;
			l.setFreespeed(1.34);
			l.setLength(4);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s3.addRelatedLinkId(l.getId());
		}
		{
			Link l = fac.createLink(new IdImpl("l8"), n9, n3);
			double flow = WIDTH *MAX_FLOW;
			l.setFreespeed(1.34);
			l.setLength(6);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s4.addRelatedLinkId(l.getId());
		}
		{
			Link l = fac.createLink(new IdImpl("l9"), n6, n10);
			double flow = WIDTH *MAX_FLOW;
			l.setFreespeed(1.34);
			double dist = ((CoordImpl)n6.getCoord()).calcDistance(n10.getCoord());
			l.setLength(dist);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s1.addRelatedLinkId(l.getId());
		}
		{
			Link l = fac.createLink(new IdImpl("l10"), n10, n11);
			double flow = WIDTH *MAX_FLOW;
			l.setFreespeed(1.34);
			double dist = ((CoordImpl)n11.getCoord()).calcDistance(n10.getCoord());
			l.setLength(dist);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s5.addRelatedLinkId(l.getId());
		}
		{
			Link l = fac.createLink(new IdImpl("l12"), n11, n12);
			double flow = WIDTH *MAX_FLOW;
			l.setFreespeed(1.34);
			l.setLength(10);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s6.addRelatedLinkId(l.getId());
		}
		{
			Link l = fac.createLink(new IdImpl("l13"), n12, n13);
			double flow = WIDTH *MAX_FLOW;
			l.setFreespeed(1.34);
			double dist = ((CoordImpl)n12.getCoord()).calcDistance(n13.getCoord());
			l.setLength(dist);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s7.addRelatedLinkId(l.getId());
		}
		{
			Link l = fac.createLink(new IdImpl("l14"), n13, n9);
			double flow = WIDTH *MAX_FLOW;
			l.setFreespeed(1.34);
			double dist = ((CoordImpl)n9.getCoord()).calcDistance(n13.getCoord());
			l.setLength(dist);
			l.setCapacity(flow);;
			Set<String> modes = new HashSet<String>();
			modes.add("walk2d");modes.add("walk");modes.add("car");
			l.setAllowedModes(modes);
			net.addLink(l);
			s3.addRelatedLinkId(l.getId());
		}

	}
	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(new IdImpl("n0"), new CoordImpl(-20,-2));
		Node n1 = fac.createNode(new IdImpl("n1"), new CoordImpl(-10,-2));
		Node n2 = fac.createNode(new IdImpl("n2"), new CoordImpl(0,-2));
		Node n3 = fac.createNode(new IdImpl("n3"), new CoordImpl(30,-2));
		Node n4 = fac.createNode(new IdImpl("n4"), new CoordImpl(40,-2));
		Node n5 = fac.createNode(new IdImpl("n5"), new CoordImpl(50,-2));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);
		net.addNode(n5);
		double flow = MAX_FLOW * WIDTH;
		Link l0 = fac.createLink(new IdImpl("l0"), n0, n1);
		Link l1 = fac.createLink(new IdImpl("l1"), n1, n2);
		Link l2 = fac.createLink(new IdImpl("l2"), n3, n4);
		Link l3 = fac.createLink(new IdImpl("l3"), n4, n5);

		Link l0Rev = fac.createLink(new IdImpl("l0_rev"), n1, n0);
		Link l1Rev = fac.createLink(new IdImpl("l1_rev"), n2, n1);
		Link l2Rev = fac.createLink(new IdImpl("l2_rev"), n4, n3);
		Link l3Rev = fac.createLink(new IdImpl("l3_rev"), n5, n4);

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

		l0.setCapacity(flow);
		l1.setCapacity(flow);
		l2.setCapacity(flow);
		l3.setCapacity(flow);

		l0Rev.setCapacity(flow);
		l1Rev.setCapacity(flow);
		l2Rev.setCapacity(flow);
		l3Rev.setCapacity(flow);

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
