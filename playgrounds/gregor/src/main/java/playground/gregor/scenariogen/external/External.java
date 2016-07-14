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

package playground.gregor.scenariogen.external;

import org.matsim.api.core.v01.Coord;
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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.HashSet;
import java.util.Set;

public class External {
	private static final int nrAgents = 10;
	private static String inputDirWrite = "/Users/laemmel/devel/jps/input";
//	private static String inputDir = "./input";
//	private static String outputDir = "./output";
//
private static String inputDir = "/Users/laemmel/devel/jps/input";
	private static String outputDir = "/Users/laemmel/devel/jps/output";

	public static void main(String[] args) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);

		createNetwork(sc);


		((NetworkImpl) sc.getNetwork()).setEffectiveCellSize(.26);
		((NetworkImpl) sc.getNetwork()).setEffectiveLaneWidth(.71);
		((NetworkImpl) sc.getNetwork()).setCapacityPeriod(1);

		c.network().setInputFile(inputDir + "/network.xml.gz");

		// c.strategy().addParam("Module_1",
		// "playground.gregor.sim2d_v4.replanning.Sim2DReRoutePlanStrategy");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "50");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".9");

		c.controler().setOutputDirectory(outputDir);
		c.controler().setLastIteration(100);

		c.plans().setInputFile(inputDir + "/population.xml.gz");

		ActivityParams pre = new ActivityParams("origin");
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
		// qsim.setEndTime(20 * 60);
		c.controler().setMobsim("extsim");
		c.global().setCoordinateSystem("EPSG:3395");

		c.qsim().setEndTime(2 * 60);

		new ConfigWriter(c).write(inputDirWrite + "/config.xml");

		new NetworkWriter(sc.getNetwork()).write(inputDirWrite + "/network.xml.gz");

		createPopulation(sc);

		Population pop = sc.getPopulation();
		new PopulationWriter(pop, sc.getNetwork()).write(inputDirWrite + "/population.xml.gz");

	}

	private static void createPopulation(Scenario sc) {
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 0;
		for (int i = 0; i < nrAgents; i++) {
			Person pers = fac.createPerson(Id.create("b" + i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			act0 = fac.createActivityFromLinkId("origin",
					Id.create("0", Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination",
					Id.create("5", Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}
	}

	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n4 = fac.createNode(Id.create("4", Node.class), new Coord(-2, 4.6));
		Node n3 = fac.createNode(Id.create("3", Node.class), new Coord(4.4, 11.2));
		Node n2 = fac.createNode(Id.create("2", Node.class), new Coord(11.2, 4.6));
		Node n1 = fac.createNode(Id.create("1", Node.class), new Coord(4.4, -2));
		Node n11 = fac.createNode(Id.create("11", Node.class), new Coord(4.4, 4.6));

		Node n5 = fac.createNode(Id.create("5", Node.class), new Coord(-20, 4.6));
		Node n6 = fac.createNode(Id.create("6", Node.class), new Coord(-4, 4.6));

		Node n7 = fac.createNode(Id.create("7", Node.class), new Coord(4.4, -4));

		Node n8 = fac.createNode(Id.create("8", Node.class), new Coord(4.4, 13.2));

		Node n9 = fac.createNode(Id.create("9", Node.class), new Coord(13.2, 4.6));

		Node n10 = fac.createNode(Id.create("10", Node.class), new Coord(20, 13.2));

		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		net.addNode(n4);
		net.addNode(n5);
		net.addNode(n6);
		net.addNode(n7);
		net.addNode(n8);
		net.addNode(n9);
		net.addNode(n10);
		net.addNode(n11);


		Link l0 = fac.createLink(Id.create("0", Link.class), n5, n6);
		Link l0Rev = fac.createLink(Id.create("0_rev", Link.class), n6, n5);


		Link l1 = fac.createLink(Id.create("1", Link.class), n6, n4);
		Link l1Rev = fac.createLink(Id.create("1_rev", Link.class), n4, n6);

		Link l2 = fac.createLink(Id.create("2", Link.class), n4, n11);
		Link l2Rev = fac.createLink(Id.create("2_rev", Link.class), n11, n4);


		Link l3 = fac.createLink(Id.create("3", Link.class), n11, n2);
		Link l3Rev = fac.createLink(Id.create("3_rev", Link.class), n2, n11);

		Link l4 = fac.createLink(Id.create("4", Link.class), n2, n9);
		Link l4Rev = fac.createLink(Id.create("4_rev", Link.class), n9, n2);

		Link l5 = fac.createLink(Id.create("5", Link.class), n9, n10);
		Link l5Rev = fac.createLink(Id.create("5_rev", Link.class), n10, n9);

		Link l6 = fac.createLink(Id.create("6", Link.class), n11, n3);
		Link l6Rev = fac.createLink(Id.create("6_rev", Link.class), n3, n11);

		Link l7 = fac.createLink(Id.create("7", Link.class), n3, n8);
		Link l7Rev = fac.createLink(Id.create("7_rev", Link.class), n8, n3);

		Link l8 = fac.createLink(Id.create("8", Link.class), n8, n9);
		Link l8Rev = fac.createLink(Id.create("8_rev", Link.class), n9, n8);

		Link l9 = fac.createLink(Id.create("9", Link.class), n11, n1);
		Link l9Rev = fac.createLink(Id.create("9_rev", Link.class), n1, n11);

		Link l10 = fac.createLink(Id.create("10", Link.class), n1, n7);
		Link l10Rev = fac.createLink(Id.create("10_rev", Link.class), n7, n1);

		Link l11 = fac.createLink(Id.create("11", Link.class), n7, n9);
		Link l11Rev = fac.createLink(Id.create("11_rev", Link.class), n9, n7);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		net.addLink(l4);
		net.addLink(l5);
		net.addLink(l6);
		net.addLink(l7);
		net.addLink(l8);
		net.addLink(l9);
		net.addLink(l10);
		net.addLink(l11);

		net.addLink(l0Rev);
		net.addLink(l1Rev);
		net.addLink(l2Rev);
		net.addLink(l3Rev);
		net.addLink(l4Rev);
		net.addLink(l5Rev);
		net.addLink(l6Rev);
		net.addLink(l7Rev);
		net.addLink(l8Rev);
		net.addLink(l9Rev);
		net.addLink(l10Rev);
		net.addLink(l11Rev);

		// extern --> Q
		Set<String> externQ = new HashSet<String>();
		externQ.add("1_rev");
		externQ.add("10");
		externQ.add("4");
		externQ.add("7");
		// Q --> Jupedsim
		Set<String> qExtern = new HashSet<String>();
		qExtern.add("2");
		qExtern.add("6_rev");
		qExtern.add("9_rev");
		qExtern.add("3_rev");

		Set<String> modes = new HashSet<String>();
		modes.add("car");
		Set<String> modesExtQ = new HashSet<String>();
		modesExtQ.add("car");
		modesExtQ.add("ext2");
		Set<String> modesQExt = new HashSet<String>();
		modesQExt.add("car");
		modesQExt.add("2ext");

		double flow = 1.6 * 1.2;
		for (Link l : net.getLinks().values()) {
			double length = CoordUtils.calcEuclideanDistance(l.getFromNode().getCoord(),
					l.getToNode().getCoord());
			l.setLength(length);
			l.setAllowedModes(modes);
			l.setFreespeed(1.34);
			l.setCapacity(flow);
			l.setNumberOfLanes(2);
			if (qExtern.contains(l.getId().toString())) {
				l.setAllowedModes(modesQExt);
			}
			if (externQ.contains(l.getId().toString())) {
				l.setAllowedModes(modesExtQ);
			}
		}
		l10.setCapacity(flow / 3.);
		l4.setCapacity(flow / 3.);
		l7.setCapacity(flow / 3.);


		((NetworkImpl) net).setCapacityPeriod(1);
		((NetworkImpl) net).setEffectiveCellSize(.26);
		((NetworkImpl) net).setEffectiveLaneWidth(.71);
	}
}
