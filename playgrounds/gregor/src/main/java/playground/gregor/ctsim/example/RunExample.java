package playground.gregor.ctsim.example;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.gregor.ctsim.run.CTRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by laemmel on 13/10/15.
 */
public class RunExample {
	private static final String OUT_DIR = "/tmp/ctsim/";
	private static final int NR_AGENTS = 2;

	public static void main(String[] args) throws IOException {
		FileUtils.deleteDirectory(new File(OUT_DIR));
		String inputDir = OUT_DIR + "/input";
		String outputDir = OUT_DIR + "/output";
		new File(inputDir).mkdirs();

		Config c = ConfigUtils.createConfig();

		c.global().setCoordinateSystem("EPSG:3395");
		Scenario sc = ScenarioUtils.createScenario(c);

		int destination = createNetwork(sc);
		((NetworkImpl) sc.getNetwork()).setEffectiveCellSize(.26);
		((NetworkImpl) sc.getNetwork()).setEffectiveLaneWidth(.71);
		((NetworkImpl) sc.getNetwork()).setCapacityPeriod(1);

		c.network().setInputFile(inputDir + "/network.xml.gz");

		// c.strategy().addParam("Module_1",
		// "playground.gregor.sim2d_v4.replanning.Sim2DReRoutePlanStrategy");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".5");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "10");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".5");

		c.controler().setOutputDirectory(outputDir);
		c.controler().setLastIteration(20);

		c.plans().setInputFile(inputDir + "/population.xml.gz");

		PlanCalcScoreConfigGroup.ActivityParams pre = new PlanCalcScoreConfigGroup.ActivityParams("origin");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when
		// running a simulation one gets
		// "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in
		// ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);

		PlanCalcScoreConfigGroup.ActivityParams post = new PlanCalcScoreConfigGroup.ActivityParams("destination");
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
		// qsim.setEndTime(20 * 60);
		c.controler().setMobsim("ctsim");
		c.global().setCoordinateSystem("EPSG:3395");

		c.qsim().setEndTime(30 * 60);

		new ConfigWriter(c).write(inputDir + "/config.xml");

		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());

		createPopulation(sc, destination);

		Population pop = sc.getPopulation();
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans()
				.getInputFile());

		CTRunner.main(new String[]{inputDir + "/config.xml", "true"});

	}

	private static void createPopulation(Scenario sc, int destination) {
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 0;
		for (int i = 0; i < NR_AGENTS / 2; i++) {
			Person pers = fac.createPerson(Id.create("b" + i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin",
					Id.create("0", Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("walkct");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination",
					Id.create(destination, Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}
//		for (int i = NR_AGENTS / 2; i < NR_AGENTS; i++) {
//			Person pers = fac.createPerson(Id.create("b" + i, Person.class));
//			Plan plan = fac.createPlan();
//			pers.addPlan(plan);
//			Activity act0;
//			act0 = fac.createActivityFromLinkId("origin",
//					Id.create(destination + 1, Link.class));
//			act0.setEndTime(t);
//			plan.addActivity(act0);
//			Leg leg = fac.createLeg("walkct");
//			plan.addLeg(leg);
//			Activity act1 = fac.createActivityFromLinkId("destination",
//					Id.create(1, Link.class));
//			plan.addActivity(act1);
//			pop.addPerson(pers);
//		}
	}


	private static int createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();

		double length = 30;
		double width = 20;

		int id = 0;
		Node n0 = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(0, 0));
		Node n1 = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(length, 0));
		net.addNode(n0);
		net.addNode(n1);

		List<Node> someNodes = new ArrayList<>();
		for (double y = -30; y <= 30; y += 10) {
			Node n = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(length * 3, y));
			net.addNode(n);
			someNodes.add(n);
		}
		Node n2 = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(length * 4, 0));
		Node n3 = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(length * 5, 0));
		net.addNode(n2);
		net.addNode(n3);
		id = 0;
		{
			Link l0 = fac.createLink(Id.createLinkId(id++), n0, n1);
			net.addLink(l0);
			l0.setCapacity(width);
			l0.setLength(length);
			Link l0Rev = fac.createLink(Id.createLinkId(id++), n1, n0);
			net.addLink(l0Rev);
			l0Rev.setCapacity(width);
			l0Rev.setLength(length);
		}
		int cnt = 0;
		for (Node n : someNodes) {
			{
				Link l0 = fac.createLink(Id.createLinkId(id++), n1, n);
				net.addLink(l0);
				l0.setCapacity(width / someNodes.size());
				l0.setLength(length * 2);//?check!!
				if (cnt++ % 2 != 0) {
					Link l0Rev = fac.createLink(Id.createLinkId(id++), n, n1);
					net.addLink(l0Rev);
					l0Rev.setCapacity(width / someNodes.size());
					l0Rev.setLength(length * 2);
				}
			}

			{
				Link l0 = fac.createLink(Id.createLinkId(id++), n, n2);
				net.addLink(l0);
				l0.setCapacity(width / someNodes.size());
				l0.setLength(length);//?check!!
				Link l0Rev = fac.createLink(Id.createLinkId(id++), n2, n);
				net.addLink(l0Rev);
				l0Rev.setCapacity(width / someNodes.size());
				l0Rev.setLength(length);
			}
		}
		int ret;
		{
			ret = id++;
			Link l0 = fac.createLink(Id.createLinkId(ret), n2, n3);
			net.addLink(l0);
			l0.setCapacity(width);
			l0.setLength(length);
		}
		{
			Link l0 = fac.createLink(Id.createLinkId(id++), n3, n2);
			net.addLink(l0);
			l0.setCapacity(width);
			l0.setLength(length);
		}

		Set<String> modes = new HashSet<>();
		modes.add("walkct");
		for (Link l : net.getLinks().values()) {
			l.setAllowedModes(modes);
			l.setFreespeed(20);
		}
		return ret;

	}
}
