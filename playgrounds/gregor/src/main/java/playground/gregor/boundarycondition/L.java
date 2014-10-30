/* *********************************************************************** *
 * project: org.matsim.*
 * ThreeDoors.java
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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import playground.gregor.sim2d_v4.io.Sim2DConfigWriter01;
import playground.gregor.sim2d_v4.io.Sim2DEnvironmentWriter03;
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

public class L {
	
	private static String inputDir = "/Users/laemmel/devel/l/input";
	private static String outputDir = "/Users/laemmel/devel/l/output";


	private static final boolean uni = true;

	private static final int nrAgents = 6000;

	public static final double SEPC_FLOW =2.2;//1.2;

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
			new Sim2DEnvironmentWriter03(env).write(envFile);
			s2d.addSim2DEnvironmentPath(envFile);
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
//			String dest = MatsimRandom.getRandom().nextBoolean() ? "k3_rev" : "k3_rev";
			Activity act1 = fac.createActivityFromLinkId("destination", Id.create("k3", Link.class));
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

		

		
		{
			int[] open = {0,2};
			Id<Node> [] openingIds = new Id[]{Id.create("n0", Node.class),Id.create("n1", Node.class)};
			GeometryFactory geofac = new GeometryFactory();
			Coordinate c0 = new Coordinate(0,0);
			Coordinate c1 = new Coordinate(0,4);
			Coordinate c2 = new Coordinate(12,4);
			Coordinate c3 = new Coordinate(8,0);
			Coordinate c4 = new Coordinate(0,0);
			Coordinate[] coords = {c0,c1,c2,c3,c4};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Section sec = env.createAndAddSection(Id.create("sec0", Section.class), p, open, new Id[]{Id.create("sec1", Section.class)} , 0,openingIds);
		}
		{
			int[] open = {0,2};
			Id<Node> [] openingIds = new Id[]{Id.create("n1", Node.class),Id.create("n2", Node.class)};
			GeometryFactory geofac = new GeometryFactory();
			Coordinate c0 = new Coordinate(8,0);
			Coordinate c1 = new Coordinate(12,4);
			Coordinate c2 = new Coordinate(12,-8);
			Coordinate c3 = new Coordinate(8,-8);
			Coordinate c4 = new Coordinate(8,0);
			Coordinate[] coords = {c0,c1,c2,c3,c4};
			LinearRing lr = geofac.createLinearRing(coords );
			Polygon p = geofac.createPolygon(lr , null);
			Section sec = env.createAndAddSection(Id.create("sec1", Section.class), p, open, new Id[]{Id.create("sec0", Section.class)} , 0,openingIds);
		}		

	}

	
	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node m0 = fac.createNode(Id.create("m0", Node.class), new CoordImpl(-40,2));
		Node m1 = fac.createNode(Id.create("m1", Node.class), new CoordImpl(-20,2.001));
		Node n0 = fac.createNode(Id.create("n0", Node.class), new CoordImpl(0,2));
		
		Node n2 = fac.createNode(Id.create("n2", Node.class), new CoordImpl(10,-8));
		Node m2 = fac.createNode(Id.create("m2", Node.class), new CoordImpl(10.001,-28));
		Node m3 = fac.createNode(Id.create("m3", Node.class), new CoordImpl(10,-48));
		
		
		net.addNode(m0);
		net.addNode(m1);
		net.addNode(m2);
		net.addNode(m3);
		net.addNode(n0);
		net.addNode(n2);
		double flow = SEPC_FLOW * 4;
		
		
		Link k0 = fac.createLink(Id.create("k0", Link.class), m0, m1);
		Link k1 = fac.createLink(Id.create("k1", Link.class), m1, n0);

		Link k0Rev = fac.createLink(Id.create("k0_rev", Link.class), m1, m0);
		Link k1Rev = fac.createLink(Id.create("k1_rev", Link.class), n0, m1);
		
		Link k2 = fac.createLink(Id.create("k2", Link.class), n2, m2);
		Link k3 = fac.createLink(Id.create("k3", Link.class), m2, m3);

		Link k2Rev = fac.createLink(Id.create("k2_rev", Link.class), m2, n2);
		Link k3Rev = fac.createLink(Id.create("k3_rev", Link.class), m3, m2);
		
		
		
		Set<String> modes = new HashSet<String>();
		modes.add("walk");modes.add("car");
		k0.setLength(20);
		k1.setLength(20);
		k2.setLength(20);
		k3.setLength(20);
		k0Rev.setLength(20);
		k1Rev.setLength(20);
		k2Rev.setLength(20);
		k3Rev.setLength(20);
		

		k0.setAllowedModes(modes);
		k1.setAllowedModes(modes);
		k2.setAllowedModes(modes);
		k3.setAllowedModes(modes);
		k0Rev.setAllowedModes(modes);
		k1Rev.setAllowedModes(modes);
		k2Rev.setAllowedModes(modes);
		k3Rev.setAllowedModes(modes);

		k0.setFreespeed(1.34);
		k1.setFreespeed(1.34);
		k2.setFreespeed(1.34);
		k3.setFreespeed(1.34);
		
		k0Rev.setFreespeed(1.34);
		k1Rev.setFreespeed(1.34);
		k2Rev.setFreespeed(1.34);
		k3Rev.setFreespeed(1.34);
		
		k0.setCapacity(flow);
		k1.setCapacity(flow);
		k2.setCapacity(flow);
		k3.setCapacity(flow);
		
		k0Rev.setCapacity(flow);
		k1Rev.setCapacity(flow);
		k2Rev.setCapacity(flow);
		k3Rev.setCapacity(flow);
		
		double lanes = 4/0.71;
		k0.setNumberOfLanes(lanes);
		k1.setNumberOfLanes(lanes);
		k2.setNumberOfLanes(lanes);
		k3.setNumberOfLanes(lanes);
		
		k0Rev.setNumberOfLanes(lanes);
		k1Rev.setNumberOfLanes(lanes);
		k2Rev.setNumberOfLanes(lanes);
		k3Rev.setNumberOfLanes(lanes);
		
		net.addLink(k0);
		net.addLink(k1);
		net.addLink(k2);
		net.addLink(k3);
		
		net.addLink(k0Rev);
		net.addLink(k1Rev);
		net.addLink(k2Rev);
		net.addLink(k3Rev);
		
		((NetworkImpl)net).setCapacityPeriod(1);
		((NetworkImpl)net).setEffectiveCellSize(.26);
		((NetworkImpl)net).setEffectiveLaneWidth(.71);
	}

}
