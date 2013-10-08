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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.geotools.referencing.CRS;
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

public class ScenarioGenerator {
	private static String inputDir = "/Users/laemmel/devel/simple/input";
	private static String outputDir = "/Users/laemmel/devel/simple/output";


	private static final boolean uni = true;

	private static final int nrAgents = 16000;

	public static final double SEPC_FLOW = 1.2;//1.2;//1.2;

	public static void main(String [] args) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		createNetwork(sc);
		//		createNetworkII(sc);
		createNetworkIII(sc);
		createNetworkIV(sc);
		//		createNetworkV(sc);

		Sim2DConfig s2d = Sim2DConfigUtils.createConfig();
		s2d.setTimeStepSize(0.04);
		Sim2DScenario s2dsc = Sim2DScenarioUtils.createSim2dScenario(s2d);
		create2DWorld(s2dsc);
		//		create2DWorld2(s2dsc);


		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(.71);
		((NetworkImpl)sc.getNetwork()).setCapacityPeriod(1);
		sc.addScenarioElement(s2dsc);



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

		createNetworkChangeEvents(sc);


		new ConfigWriter(c).write(inputDir+ "/config.xml");

		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());

		createPopulation(sc);
		createPopulation2(sc);
		createPopulationIV(sc);
		//		createPopulationV(sc);

		Population pop = sc.getPopulation();
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans().getInputFile());

	}

	private static void createNetworkChangeEvents(Scenario sc) {
		Collection<NetworkChangeEvent> events = new LinkedList<NetworkChangeEvent>();
		NetworkChangeEventFactoryImpl fac = new NetworkChangeEventFactoryImpl();
		double incr = (4*SEPC_FLOW)/10;
		double flowCap = incr;


		//		NetworkChangeEvent ee = fac.createNetworkChangeEvent(0);
		//		ee.addLink(sc.getNetwork().getLinks().get(new IdImpl("l2")));
		////		ee.addLink(sc.getNetwork().getLinks().get(new IdImpl("l6")));
		////		ee.addLink(sc.getNetwork().getLinks().get(new IdImpl("t_l6")));
		//		ChangeValue ccv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,0.01);
		//		ee.setFlowCapacityChange(ccv);
		//		events.add(ee);


		for (double time = 0; time < 120*60; time += 240) {

			NetworkChangeEvent e = fac.createNetworkChangeEvent(time);
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l0")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l1")));
			//			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l2")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l3")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l4")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l5")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l6")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l7")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("t_l4")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("t_l5")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("t_l6")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("t_l7")));

			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l0_rev")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l1_rev")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l2_rev")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l3_rev")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l4_rev")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l5_rev")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l6_rev")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l7_rev")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("t_l4_rev")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("t_l5_rev")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("t_l6_rev")));
			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("t_l7_rev")));

			System.out.println(time/60 + " flow" + flowCap/4); 

			ChangeValue cv = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, flowCap);
			e.setFlowCapacityChange(cv);
			events.add(e);

			NetworkChangeEvent ee1 = fac.createNetworkChangeEvent(time);
			ee1.addLink(sc.getNetwork().getLinks().get(new IdImpl("l2")));
			//			ee.addLink(sc.getNetwork().getLinks().get(new IdImpl("l6")));
			//			ee.addLink(sc.getNetwork().getLinks().get(new IdImpl("t_l6")));
			if (time < 35*60) {
				ChangeValue ccv1 = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,0.95*flowCap);

				ee1.setFlowCapacityChange(ccv1);
			} else if (time < 39*60){
				ChangeValue ccv1 = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,0.7*flowCap);

				ee1.setFlowCapacityChange(ccv1);
			}else if (time < 43*60){
				ChangeValue ccv1 = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,0.001*flowCap);

				ee1.setFlowCapacityChange(ccv1);
			} else {
				ChangeValue ccv1 = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,0.7*flowCap);

				ee1.setFlowCapacityChange(ccv1);
			}
			events.add(ee1);


			flowCap += incr;
		}
		//		flowCap -= incr;
		//		for (double time = 20*60; time < 40*60; time += 120) {
		//			NetworkChangeEvent e = fac.createNetworkChangeEvent(time);
		//			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l0")));
		//			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l4")));
		//			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("t_l4")));
		////			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("mt_l4")));
		//			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("t_l7_rev")));
		////			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("mt_l7_rev")));
		//			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l3_rev")));
		//			e.addLink(sc.getNetwork().getLinks().get(new IdImpl("l7_rev")));
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
			//			t += 2;
			//			if ((i+1)%(960) == 0) {
			//				t += 3*60+12;
			//			}
		}

		if (uni) {
			return;
		}
		for (int i = nrAgents; i < 2*nrAgents; i++) {
			Person pers = fac.createPerson(new IdImpl("d"+i));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", new IdImpl("l3_rev"));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", new IdImpl("l0_rev"));
			plan.addActivity(act1);
			pop.addPerson(pers);
			////			t += .5;
			//			if ((i+1)%(960) == 0) {
			//				t += 3*60+12;
			//			}
		}

	}

	private static void createPopulation2(Scenario sc) {
		Population pop = sc.getPopulation();
		//		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 0;
		for (int i = 0; i < nrAgents; i++) {
			Person pers = fac.createPerson(new IdImpl("c"+i));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", new IdImpl("l4"));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", new IdImpl("l7"));
			plan.addActivity(act1);
			pop.addPerson(pers);
			////			t += .5;
			//			if ((i+1)%(960) == 0) {
			//				t += 3*60+12;
			//			}
		}

		if (uni) {
			return;
		}

		for (int i = nrAgents; i < 2*nrAgents; i++) {
			Person pers = fac.createPerson(new IdImpl("e"+i));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", new IdImpl("l7_rev"));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", new IdImpl("l4_rev"));
			plan.addActivity(act1);
			pop.addPerson(pers);
			////			t += .5;
			//			if ((i+1)%(960) == 0) {
			//				t += 3*60+12;
			//			}
		}

	}

	private static void createPopulationIV(Scenario sc) {
		Population pop = sc.getPopulation();
		//		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 0;
		for (int i = 0; i < nrAgents; i++) {
			Person pers = fac.createPerson(new IdImpl("t_c"+i));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", new IdImpl("t_l4"));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", new IdImpl("t_l7"));
			plan.addActivity(act1);
			pop.addPerson(pers);
			////			t += .5;
			//			if ((i+1)%(960) == 0) {
			//				t += 3*60+12;
			//			}
		}

		if (uni) {
			return;
		}

		for (int i = nrAgents; i < 2*nrAgents; i++) {
			Person pers = fac.createPerson(new IdImpl("t_e"+i));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", new IdImpl("t_l7_rev"));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", new IdImpl("t_l4_rev"));
			plan.addActivity(act1);
			pop.addPerson(pers);
			////			t += .5;
			//			if ((i+1)%(960) == 0) {
			//				t += 3*60+12;
			//			}
		}

	}

	private static void createPopulationV(Scenario sc) {

		Population pop = sc.getPopulation();
		//		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 0;
		for (int i = 0; i < nrAgents; i++) {
			Person pers = fac.createPerson(new IdImpl("mt_c"+i));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", new IdImpl("mt_l4"));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", new IdImpl("mt_l7"));
			plan.addActivity(act1);
			pop.addPerson(pers);
			////			t += .5;
			//			if ((i+1)%(960) == 0) {
			//				t += 3*60+12;
			//			}
		}

		if (uni) {
			return;
		}

		for (int i = nrAgents; i < 2*nrAgents; i++) {
			Person pers = fac.createPerson(new IdImpl("mt_e"+i));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", new IdImpl("mt_l7_rev"));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", new IdImpl("mt_l4_rev"));
			plan.addActivity(act1);
			pop.addPerson(pers);
			////			t += .5;
			//			if ((i+1)%(960) == 0) {
			//				t += 3*60+12;
			//			}
		}

	}

	private static void create2DWorld(Sim2DScenario sc2) {
		Sim2DEnvironment env = new Sim2DEnvironment();
		env.setId(new IdImpl("env0"));

		sc2.addSim2DEnvironment(env);

		int[] open = {0,2};
		GeometryFactory geofac = new GeometryFactory();
		Coordinate c0 = new Coordinate(5,-2);
		Coordinate c1 = new Coordinate(5,2);
		//		Coordinate c1a = new Coordinate(8,2);
		//		Coordinate c1b = new Coordinate(20,20);
		//		Coordinate c1c = new Coordinate(32,2);
		Coordinate c2 = new Coordinate(35,2);
		Coordinate c3 = new Coordinate(35,-2);
		//		Coordinate c3a = new Coordinate(32,-2);
		//		Coordinate c3b = new Coordinate(20,-20);
		//		Coordinate c3c = new Coordinate(8,-2);
		Coordinate c4 = new Coordinate(5,-2);
		env.setEnvelope(new Envelope(5,35,-2,2));
		try {
			env.setCRS(CRS.decode("EPSG:3395"));
		} catch (NoSuchAuthorityCodeException e) {
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		//		Coordinate[] coords = {c0,c1,c1a,c1b,c1c,c2,c3,c3a,c3b,c3c,c4};
		Coordinate[] coords = {c0,c1,c2,c3,c4};
		LinearRing lr = geofac.createLinearRing(coords );
		Polygon p = geofac.createPolygon(lr , null);
		Section sec = env.createAndAddSection(new IdImpl("sec0"), p, open, null, 0);

		NetworkImpl net = NetworkImpl.createNetwork();
		NetworkFactoryImpl fac = net.getFactory();
		NodeImpl n0 = fac.createNode(new IdImpl(2), new CoordImpl(5,-.001));
		NodeImpl n1 = fac.createNode(new IdImpl(3), new CoordImpl(35,0));
		net.addNode(n0);
		net.addNode(n1);
		IdImpl id = new IdImpl("l2d0");
		Link l = fac.createLink(id, n0, n1);

		double flow = 4 *SEPC_FLOW;
		l.setFreespeed(1.34);
		l.setLength(30);
		l.setCapacity(flow);;
		Set<String> modes = new HashSet<String>();
		modes.add("walk2d");modes.add("walk");modes.add("car");
		l.setAllowedModes(modes);
		net.addLink(l);

		IdImpl idRev = new IdImpl("l2d0_rev");
		Link lRev = fac.createLink(idRev, n1, n0);
		lRev.setFreespeed(1.34);
		lRev.setLength(30);
		lRev.setAllowedModes(modes);
		lRev.setCapacity(flow);
		net.addLink(lRev);

		net.setCapacityPeriod(1);
		env.setNetwork(net);
		sec.addRelatedLinkId(id);
		sec.addRelatedLinkId(idRev);

	}

	private static void create2DWorld2(Sim2DScenario sc2) {
		Sim2DEnvironment env = new Sim2DEnvironment();
		env.setId(new IdImpl("env1"));

		sc2.addSim2DEnvironment(env);

		int[] open = {0,2};
		GeometryFactory geofac = new GeometryFactory();
		Coordinate c0 = new Coordinate(5,8);
		Coordinate c1 = new Coordinate(5,12);
		Coordinate c2 = new Coordinate(35,12);
		Coordinate c3 = new Coordinate(35,8);
		Coordinate c4 = new Coordinate(5,8);
		env.setEnvelope(new Envelope(c0,c2));
		try {
			env.setCRS(CRS.decode("EPSG:3395"));
		} catch (NoSuchAuthorityCodeException e) {
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		Coordinate[] coords = {c0,c1,c2,c3,c4};
		LinearRing lr = geofac.createLinearRing(coords );
		Polygon p = geofac.createPolygon(lr , null);
		Section sec = env.createAndAddSection(new IdImpl("sec0"), p, open, null, 0);

		NetworkImpl net = NetworkImpl.createNetwork();
		NetworkFactoryImpl fac = net.getFactory();
		NodeImpl n0 = fac.createNode(new IdImpl(8), new CoordImpl(5,10));
		NodeImpl n1 = fac.createNode(new IdImpl(9), new CoordImpl(35,10));
		net.addNode(n0);
		net.addNode(n1);
		IdImpl id = new IdImpl("l2d1");
		Link l = fac.createLink(id, n0, n1);
		l.setFreespeed(1.34);
		l.setLength(10);
		Set<String> modes = new HashSet<String>();
		modes.add("walk2d");modes.add("walk");modes.add("car");
		l.setAllowedModes(modes);
		net.addLink(l);

		IdImpl idRev = new IdImpl("l2d1_rev");
		Link lRev = fac.createLink(idRev, n1, n0);
		lRev.setFreespeed(1.34);
		lRev.setLength(30);
		lRev.setAllowedModes(modes);
		net.addLink(lRev);

		net.setCapacityPeriod(1);
		env.setNetwork(net);
		sec.addRelatedLinkId(id);
		sec.addRelatedLinkId(idRev);

	}

	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(new IdImpl(0), new CoordImpl(-23,0));
		Node n1 = fac.createNode(new IdImpl(1), new CoordImpl(-9,0.01));
		Node n2 = fac.createNode(new IdImpl(2), new CoordImpl(5,-.001));
		Node n3 = fac.createNode(new IdImpl(3), new CoordImpl(35,0));
		Node n4 = fac.createNode(new IdImpl(4), new CoordImpl(49,0.01));
		Node n5 = fac.createNode(new IdImpl(5), new CoordImpl(63,0.02));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);
		net.addNode(n5);
		double flow = SEPC_FLOW * 4;
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
		l0.setLength(14);
		l1.setLength(14);
		l2.setLength(14);
		l3.setLength(14);

		l0Rev.setLength(14);
		l1Rev.setLength(14);
		l2Rev.setLength(14);
		l3Rev.setLength(14);

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

		double lanes = 4/0.71;
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

	private static void createNetworkII(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(new IdImpl(6), new CoordImpl(-20,10));
		Node n1 = fac.createNode(new IdImpl(7), new CoordImpl(-9,10.01));
		Node n2 = fac.createNode(new IdImpl(8), new CoordImpl(5,9.999));
		Node n3 = fac.createNode(new IdImpl(9), new CoordImpl(35,10));
		Node n4 = fac.createNode(new IdImpl(10), new CoordImpl(42,10.01));
		Node n5 = fac.createNode(new IdImpl(11), new CoordImpl(43,10));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);
		net.addNode(n5);
		double flow = SEPC_FLOW * 4;
		Link l0 = fac.createLink(new IdImpl("l4"), n0, n1);
		Link l1 = fac.createLink(new IdImpl("l5"), n1, n2);
		Link l2 = fac.createLink(new IdImpl("l6"), n3, n4);
		Link l3 = fac.createLink(new IdImpl("l7"), n4, n5);
		Set<String> modes = new HashSet<String>();
		modes.add("walk");modes.add("car");
		l0.setLength(6);
		l1.setLength(14);
		l2.setLength(5);
		l3.setLength(1);
		l0.setAllowedModes(modes);
		l1.setAllowedModes(modes);
		l2.setAllowedModes(modes);
		l3.setAllowedModes(modes);
		l0.setFreespeed(1.34);
		l1.setFreespeed(1.34);
		l2.setFreespeed(1.34);
		l3.setFreespeed(1.34);
		l0.setCapacity(flow);
		l1.setCapacity(flow);
		l2.setCapacity(flow);
		l3.setCapacity(flow);
		double lanes = 4/0.71;
		l0.setNumberOfLanes(lanes);
		l1.setNumberOfLanes(lanes);
		l2.setNumberOfLanes(lanes);
		l3.setNumberOfLanes(lanes);
		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveCellSize(.26);
		((NetworkImpl)net).setEffectiveLaneWidth(.71);

		throw new RuntimeException("implement 'create reverse links'!!!");
	}

	private static void createNetworkIII(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(new IdImpl(6), new CoordImpl(-23,10));
		Node n1 = fac.createNode(new IdImpl(7), new CoordImpl(-9,10.01));
		Node n2 = fac.createNode(new IdImpl(8), new CoordImpl(5,9.999));
		Node n3 = fac.createNode(new IdImpl(9), new CoordImpl(35,10));
		Node n4 = fac.createNode(new IdImpl(10), new CoordImpl(49,10.01));
		Node n5 = fac.createNode(new IdImpl(11), new CoordImpl(63,10));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);
		net.addNode(n5);
		double flow = SEPC_FLOW * 4;
		Link l0 = fac.createLink(new IdImpl("l4"), n0, n1);
		Link l1 = fac.createLink(new IdImpl("l5"), n1, n2);
		Link l1b = fac.createLink(new IdImpl("l5b"), n2, n3);
		Link l2 = fac.createLink(new IdImpl("l6"), n3, n4);
		Link l3 = fac.createLink(new IdImpl("l7"), n4, n5);

		Link l0Rev = fac.createLink(new IdImpl("l4_rev"), n1, n0);
		Link l1Rev = fac.createLink(new IdImpl("l5_rev"), n2, n1);
		Link l1bRev = fac.createLink(new IdImpl("l5b_rev"), n3, n2);
		Link l2Rev = fac.createLink(new IdImpl("l6_rev"), n4, n3);
		Link l3Rev = fac.createLink(new IdImpl("l7_rev"), n5, n4);

		Set<String> modes = new HashSet<String>();
		modes.add("walk");modes.add("car");


		l0.setLength(14);
		l1.setLength(14);
		l1b.setLength(30);
		l2.setLength(14);
		l3.setLength(14);

		l0Rev.setLength(14);
		l1Rev.setLength(14);
		l1bRev.setLength(30);
		l2Rev.setLength(14);
		l3Rev.setLength(14);

		l0.setAllowedModes(modes);
		l1.setAllowedModes(modes);
		l1b.setAllowedModes(modes);
		l2.setAllowedModes(modes);
		l3.setAllowedModes(modes);

		l0Rev.setAllowedModes(modes);
		l1Rev.setAllowedModes(modes);
		l1bRev.setAllowedModes(modes);
		l2Rev.setAllowedModes(modes);
		l3Rev.setAllowedModes(modes);

		l0.setFreespeed(1.34);
		l1.setFreespeed(1.34);
		l1b.setFreespeed(1.34);
		l2.setFreespeed(1.34);
		l3.setFreespeed(1.34);

		l0Rev.setFreespeed(1.34);
		l1Rev.setFreespeed(1.34);
		l1bRev.setFreespeed(1.34);
		l2Rev.setFreespeed(1.34);
		l3Rev.setFreespeed(1.34);

		l0.setCapacity(flow);
		l1.setCapacity(flow);
		l1b.setCapacity(flow);
		l2.setCapacity(flow);
		l3.setCapacity(flow);

		l0Rev.setCapacity(flow);
		l1Rev.setCapacity(flow);
		l1bRev.setCapacity(flow);
		l2Rev.setCapacity(flow);
		l3Rev.setCapacity(flow);

		double lanes = 4/0.71;
		l0.setNumberOfLanes(lanes);
		l1.setNumberOfLanes(lanes);
		l1b.setNumberOfLanes(lanes);
		l2.setNumberOfLanes(lanes);
		l3.setNumberOfLanes(lanes);

		l0Rev.setNumberOfLanes(lanes);
		l1Rev.setNumberOfLanes(lanes);
		l1bRev.setNumberOfLanes(lanes);
		l2Rev.setNumberOfLanes(lanes);
		l3Rev.setNumberOfLanes(lanes);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l1b);
		net.addLink(l2);
		net.addLink(l3);

		net.addLink(l0Rev);
		net.addLink(l1Rev);
		net.addLink(l1bRev);
		net.addLink(l2Rev);
		net.addLink(l3Rev);

		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveCellSize(.26);
		((NetworkImpl)net).setEffectiveLaneWidth(.71);
	}

	private static void createNetworkIV(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(new IdImpl(12), new CoordImpl(-23,20));
		Node n1 = fac.createNode(new IdImpl(13), new CoordImpl(-9,20.01));
		Node n2 = fac.createNode(new IdImpl(14), new CoordImpl(5,19.999));
		Node n3 = fac.createNode(new IdImpl(15), new CoordImpl(35,20));
		Node n4 = fac.createNode(new IdImpl(16), new CoordImpl(49,20.01));
		Node n5 = fac.createNode(new IdImpl(17), new CoordImpl(63,20));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);
		net.addNode(n5);
		double flow = SEPC_FLOW * 4;
		Link l0 = fac.createLink(new IdImpl("t_l4"), n0, n1);
		Link l1 = fac.createLink(new IdImpl("t_l5"), n1, n2);
		Link l1b = fac.createLink(new IdImpl("t_l5b"), n2, n3);
		Link l2 = fac.createLink(new IdImpl("t_l6"), n3, n4);
		Link l3 = fac.createLink(new IdImpl("t_l7"), n4, n5);

		Link l0Rev = fac.createLink(new IdImpl("t_l4_rev"), n1, n0);
		Link l1Rev = fac.createLink(new IdImpl("t_l5_rev"), n2, n1);
		Link l1bRev = fac.createLink(new IdImpl("t_l5b_rev"), n3, n2);
		Link l2Rev = fac.createLink(new IdImpl("t_l6_rev"), n4, n3);
		Link l3Rev = fac.createLink(new IdImpl("t_l7_rev"), n5, n4);

		Set<String> modes = new HashSet<String>();
		modes.add("walk");modes.add("car");


		l0.setLength(14);
		l1.setLength(14);
		l1b.setLength(30);
		l2.setLength(14);
		l3.setLength(14);

		l0Rev.setLength(14);
		l1Rev.setLength(14);
		l1bRev.setLength(30);
		l2Rev.setLength(14);
		l3Rev.setLength(14);

		l0.setAllowedModes(modes);
		l1.setAllowedModes(modes);
		l1b.setAllowedModes(modes);
		l2.setAllowedModes(modes);
		l3.setAllowedModes(modes);

		l0Rev.setAllowedModes(modes);
		l1Rev.setAllowedModes(modes);
		l1bRev.setAllowedModes(modes);
		l2Rev.setAllowedModes(modes);
		l3Rev.setAllowedModes(modes);

		l0.setFreespeed(1.34);
		l1.setFreespeed(1.34);
		l1b.setFreespeed(1.34);
		l2.setFreespeed(1.34);
		l3.setFreespeed(1.34);

		l0Rev.setFreespeed(1.34);
		l1Rev.setFreespeed(1.34);
		l1bRev.setFreespeed(1.34);
		l2Rev.setFreespeed(1.34);
		l3Rev.setFreespeed(1.34);

		l0.setCapacity(flow);
		l1.setCapacity(flow);
		l1b.setCapacity(flow);
		l2.setCapacity(flow);
		l3.setCapacity(flow);

		l0Rev.setCapacity(flow);
		l1Rev.setCapacity(flow);
		l1bRev.setCapacity(flow);
		l2Rev.setCapacity(flow);
		l3Rev.setCapacity(flow);

		double lanes = 4/0.71;
		l0.setNumberOfLanes(lanes);
		l1.setNumberOfLanes(lanes);
		l1b.setNumberOfLanes(lanes);
		l2.setNumberOfLanes(lanes);
		l3.setNumberOfLanes(lanes);

		l0Rev.setNumberOfLanes(lanes);
		l1Rev.setNumberOfLanes(lanes);
		l1bRev.setNumberOfLanes(lanes);
		l2Rev.setNumberOfLanes(lanes);
		l3Rev.setNumberOfLanes(lanes);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l1b);
		net.addLink(l2);
		net.addLink(l3);

		net.addLink(l0Rev);
		net.addLink(l1Rev);
		net.addLink(l1bRev);
		net.addLink(l2Rev);
		net.addLink(l3Rev);

		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveCellSize(.26);
		((NetworkImpl)net).setEffectiveLaneWidth(.71);
	}

	private static void createNetworkV(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(new IdImpl(18), new CoordImpl(-23,30));
		Node n1 = fac.createNode(new IdImpl(19), new CoordImpl(-9,30.01));
		Node n2 = fac.createNode(new IdImpl(20), new CoordImpl(5,29.999));
		Node n3 = fac.createNode(new IdImpl(21), new CoordImpl(35,30));
		Node n4 = fac.createNode(new IdImpl(22), new CoordImpl(49,30.01));
		Node n5 = fac.createNode(new IdImpl(23), new CoordImpl(63,30));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);
		net.addNode(n5);
		double flow = SEPC_FLOW * 4;
		Link l0 = fac.createLink(new IdImpl("mt_l4"), n0, n1);
		Link l1 = fac.createLink(new IdImpl("mt_l5"), n1, n2);
		Link l1b = fac.createLink(new IdImpl("mt_l5b"), n2, n3);
		Link l2 = fac.createLink(new IdImpl("mt_l6"), n3, n4);
		Link l3 = fac.createLink(new IdImpl("mt_l7"), n4, n5);

		Link l0Rev = fac.createLink(new IdImpl("mt_l4_rev"), n1, n0);
		Link l1Rev = fac.createLink(new IdImpl("mt_l5_rev"), n2, n1);
		Link l1bRev = fac.createLink(new IdImpl("mt_l5b_rev"), n3, n2);
		Link l2Rev = fac.createLink(new IdImpl("mt_l6_rev"), n4, n3);
		Link l3Rev = fac.createLink(new IdImpl("mt_l7_rev"), n5, n4);

		Set<String> modes = new HashSet<String>();
		modes.add("walk");modes.add("car");


		l0.setLength(14);
		l1.setLength(14);
		l1b.setLength(30);
		l2.setLength(14);
		l3.setLength(14);

		l0Rev.setLength(14);
		l1Rev.setLength(14);
		l1bRev.setLength(30);
		l2Rev.setLength(14);
		l3Rev.setLength(14);

		l0.setAllowedModes(modes);
		l1.setAllowedModes(modes);
		l1b.setAllowedModes(modes);
		l2.setAllowedModes(modes);
		l3.setAllowedModes(modes);

		l0Rev.setAllowedModes(modes);
		l1Rev.setAllowedModes(modes);
		l1bRev.setAllowedModes(modes);
		l2Rev.setAllowedModes(modes);
		l3Rev.setAllowedModes(modes);

		l0.setFreespeed(1.34);
		l1.setFreespeed(1.34);
		l1b.setFreespeed(1.34);
		l2.setFreespeed(1.34);
		l3.setFreespeed(1.34);

		l0Rev.setFreespeed(1.34);
		l1Rev.setFreespeed(1.34);
		l1bRev.setFreespeed(1.34);
		l2Rev.setFreespeed(1.34);
		l3Rev.setFreespeed(1.34);

		l0.setCapacity(flow);
		l1.setCapacity(flow);
		l1b.setCapacity(flow);
		l2.setCapacity(flow);
		l3.setCapacity(flow);

		l0Rev.setCapacity(flow);
		l1Rev.setCapacity(flow);
		l1bRev.setCapacity(flow);
		l2Rev.setCapacity(flow);
		l3Rev.setCapacity(flow);

		double lanes = 4/0.71;
		l0.setNumberOfLanes(lanes);
		l1.setNumberOfLanes(lanes);
		l1b.setNumberOfLanes(lanes);
		l2.setNumberOfLanes(lanes);
		l3.setNumberOfLanes(lanes);

		l0Rev.setNumberOfLanes(lanes);
		l1Rev.setNumberOfLanes(lanes);
		l1bRev.setNumberOfLanes(lanes);
		l2Rev.setNumberOfLanes(lanes);
		l3Rev.setNumberOfLanes(lanes);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l1b);
		net.addLink(l2);
		net.addLink(l3);

		net.addLink(l0Rev);
		net.addLink(l1Rev);
		net.addLink(l1bRev);
		net.addLink(l2Rev);
		net.addLink(l3Rev);

		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveCellSize(.26);
		((NetworkImpl)net).setEffectiveLaneWidth(.71);
	}
}
