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

public class SoloRefG {
	
	public static void main(String [] args) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		createNetwork(sc);
		
		Sim2DConfig s2d = Sim2DConfigUtils.createConfig();
		s2d.setTimeStepSize(0.04);
		Sim2DScenario s2dsc = Sim2DScenarioUtils.createSim2dScenario(s2d);
		create2DWorld(s2dsc);
		
		String inputDir = "/Users/laemmel/devel/simple/input";
		String outputDir = "/Users/laemmel/devel/simple/output";
		
		
		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(.71);
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
		c.controler().setLastIteration(500);
		
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
		qsim.setEndTime(2*3600);
		c.controler().setMobsim("hybridQ2D");
		
		c.global().setCoordinateSystem("EPSG:3395");
		new ConfigWriter(c).write(inputDir+ "/config.xml");

		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());
		
		createPopulation(sc);
		Population pop = sc.getPopulation();
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans().getInputFile());

	}

	private static void createPopulation(Scenario sc) {
		int nrAgents = 60000;
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 0;
		for (int i = 0; i < nrAgents; i++) {
			Person pers = fac.createPerson(Id.create("b"+i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin", Id.create("l1", Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", Id.create("l3", Link.class));
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
		env.setId(Id.create("env0", Sim2DEnvironment.class));
		
		sc2.addSim2DEnvironment(env);
		
		int[] open = {0,2};
		GeometryFactory geofac = new GeometryFactory();
		Coordinate c0 = new Coordinate(5,-2);
		Coordinate c1 = new Coordinate(5,2);
		Coordinate c2 = new Coordinate(35,2);
		Coordinate c3 = new Coordinate(35,-2);
		Coordinate c4 = new Coordinate(5,-2);
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
		Section sec = env.createAndAddSection(Id.create("sec0", Section.class), p, open, null, 0);
		
		NetworkImpl net = NetworkImpl.createNetwork();
		NetworkFactoryImpl fac = net.getFactory();
		NodeImpl n0 = fac.createNode(Id.create(2, Node.class), new CoordImpl(5,0));
		NodeImpl n1 = fac.createNode(Id.create(3, Node.class), new CoordImpl(35,0));
		net.addNode(n0);
		net.addNode(n1);
		Id<Link> id = Id.create("l2d0", Link.class);
		Link l = fac.createLink(id, n0, n1);
		l.setFreespeed(1.34);
		l.setLength(10);
		Set<String> modes = new HashSet<String>();
		modes.add("walk2d");modes.add("walk");modes.add("car");
		l.setAllowedModes(modes);
		net.addLink(l);
		net.setCapacityPeriod(1);
		env.setNetwork(net);
		sec.addRelatedLinkId(id);
		
	}

	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(Id.create(0, Node.class), new CoordImpl(0,0));
		Node n1 = fac.createNode(Id.create(1, Node.class), new CoordImpl(1,0));
		Node n2 = fac.createNode(Id.create(2, Node.class), new CoordImpl(5,0));
		Node n3 = fac.createNode(Id.create(3, Node.class), new CoordImpl(35,0));
		Node n4 = fac.createNode(Id.create(4, Node.class), new CoordImpl(40,0));
		Node n5 = fac.createNode(Id.create(5, Node.class), new CoordImpl(41,0));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);
		net.addNode(n5);
		double flow = 1.3 * 4;
		Link l0 = fac.createLink(Id.create("l0", Link.class), n0, n1);
		Link l1 = fac.createLink(Id.create("l1", Link.class), n1, n2);
		Link l2 = fac.createLink(Id.create("l2", Link.class), n3, n4);
		Link l3 = fac.createLink(Id.create("l3", Link.class), n4, n5);
		Set<String> modes = new HashSet<String>();
		 modes.add("walk");modes.add("car");
		l0.setLength(1);
		l1.setLength(4);
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
		l0.setCapacity(4*flow);
		l1.setCapacity(4*flow);
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
	}

}
