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

package playground.gregor.scenariogen.casimpleexp;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
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
import org.matsim.core.utils.geometry.CoordUtils;

public class CAExperimentsGRID {
	private static String inputDir = "/Users/laemmel/devel/cagridexp/input";
	private static String outputDir = "/Users/laemmel/devel/cagridexp/output";

	private static final double WIDTH = 400;
	private static final double HEIGHT = 300;

	private static final int NR_ROWS = 3;
	private static final int NR_COLS = 4;

	private static final double minX = 0;
	private static final double minY = 0;
	private static final double maxX = minX + WIDTH;
	private static final double maxY = minY + HEIGHT;

	private static final int nrAgents = 1500;

	public static void main(String[] args) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);

		createNetwork(sc);

		((NetworkImpl) sc.getNetwork()).setEffectiveCellSize(.26);
		((NetworkImpl) sc.getNetwork()).setEffectiveLaneWidth(.71);
		((NetworkImpl) sc.getNetwork()).setCapacityPeriod(3600);

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
		c.controler().setMobsim("casim");
		c.global().setCoordinateSystem("EPSG:3395");

		c.qsim().setEndTime(2 * 3600);

		new ConfigWriter(c).write(inputDir + "/config.xml");

		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());

		createPopulation(sc);

		Population pop = sc.getPopulation();
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans()
				.getInputFile());

	}

	private static void createPopulation(Scenario sc) {
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 0;
		for (int i = 0; i < nrAgents / 4; i++) {
			Person pers = fac.createPerson(Id.create("b" + i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			String from = "n1_" + (NR_ROWS - 1) + "_0";
			act0 = fac.createActivityFromLinkId("origin",
					Id.create(from, Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("walkca");
			plan.addLeg(leg);
			String to = "0_" + (NR_COLS - 1) + "_n3";
			Activity act1 = fac.createActivityFromLinkId("destination",
					Id.create(to, Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}
		for (int i = nrAgents / 4; i < nrAgents / 2; i++) {
			Person pers = fac.createPerson(Id.create("a" + i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			String from = "n2_" + (NR_ROWS - 1) + "_" + (NR_COLS - 1);
			act0 = fac.createActivityFromLinkId("origin",
					Id.create(from, Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("walkca");
			plan.addLeg(leg);
			String to = "0_0_n0";
			Activity act1 = fac.createActivityFromLinkId("destination",
					Id.create(to, Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}
		for (int i = nrAgents / 2; i < 3 * nrAgents / 4; i++) {
			Person pers = fac.createPerson(Id.create("c" + i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			String from = "n0_0_0";
			act0 = fac.createActivityFromLinkId("origin",
					Id.create(from, Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("walkca");
			plan.addLeg(leg);
			String to = (NR_ROWS - 1) + "_" + (NR_COLS - 1) + "_n2";
			Activity act1 = fac.createActivityFromLinkId("destination",
					Id.create(to, Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}
		for (int i = 3 * nrAgents / 4; i < nrAgents; i++) {
			Person pers = fac.createPerson(Id.create("d" + i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			String from = "n3_0_" + (NR_COLS - 1);
			act0 = fac.createActivityFromLinkId("origin",
					Id.create(from, Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("walkca");
			plan.addLeg(leg);
			String to = (NR_ROWS - 1) + "_0_n1";
			Activity act1 = fac.createActivityFromLinkId("destination",
					Id.create(to, Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
		}
	}

	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(Id.create("n0", Node.class), new Coord(minX, minY));
		Node n1 = fac.createNode(Id.create("n1", Node.class), new Coord(minX, maxY));
		Node n2 = fac.createNode(Id.create("n2", Node.class), new Coord(maxX, maxY));
		Node n3 = fac.createNode(Id.create("n3", Node.class), new Coord(maxX, minY));

		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);

		// nodes
		Node[][] nodes = new Node[NR_ROWS][NR_COLS];
		double dx = (WIDTH / 2) / (NR_ROWS - 1);
		double dy = (HEIGHT / 2) / (NR_COLS - 1);

		double width = dx * (NR_COLS - 1);
		double height = dy * (NR_ROWS - 1);

		double y = minY + (HEIGHT - height) / 2;
		for (int i = 0; i < NR_ROWS; i++) {
			double x = minX + (WIDTH - width) / 2;
			for (int j = 0; j < NR_COLS; j++) {
				Node n = fac.createNode(Id.create(i + "_" + j, Node.class),
						new Coord(x, y));
				net.addNode(n);
				nodes[i][j] = n;
				x += dx;
			}
			y += dy;
		}

		// connector links
		{
			Node n = n0;
			int i = 0;
			int j = 0;
			Node nR = nodes[i][j];
			createLink(fac, n, nR, net, 10);
			createLink(fac, nR, n, net, 10);
		}
		{
			Node n = n1;
			int i = NR_ROWS - 1;
			int j = 0;
			Node nR = nodes[i][j];
			createLink(fac, n, nR, net, 10);
			createLink(fac, nR, n, net, 10);
		}
		{
			Node n = n2;
			int i = NR_ROWS - 1;
			int j = NR_COLS - 1;
			Node nR = nodes[i][j];
			createLink(fac, n, nR, net, 10);
			createLink(fac, nR, n, net, 10);
		}
		{
			Node n = n3;
			int i = 0;
			int j = NR_COLS - 1;
			Node nR = nodes[i][j];
			createLink(fac, n, nR, net, 10);
			createLink(fac, nR, n, net, 10);
		}

		// links
		for (int i = 0; i < NR_ROWS; i++) {
			for (int j = 0; j < NR_COLS; j++) {
				if (j + 1 < NR_COLS) {
					Node n = nodes[i][j];
					Node nR = nodes[i][j + 1];
					createLink(fac, n, nR, net, 1.2);
					createLink(fac, nR, n, net, 1.2);
				}
				if (i + 1 < NR_ROWS) {
					Node n = nodes[i][j];
					Node nR = nodes[i + 1][j];
					createLink(fac, n, nR, net, 1.2);
					createLink(fac, nR, n, net, 1.2);
				}
			}
		}

		((NetworkImpl) net).setCapacityPeriod(1);
		((NetworkImpl) net).setEffectiveCellSize(.26);
		((NetworkImpl) net).setEffectiveLaneWidth(.71);
	}

	private static void createLink(NetworkFactory fac, Node n, Node nR,
			Network net, double w) {
		Link con1 = fac.createLink(
				Id.create(n.getId() + "_" + nR.getId(), Link.class), n, nR);
		double length = CoordUtils.calcEuclideanDistance(n.getCoord(), nR.getCoord());
		con1.setLength(length);
		Set<String> modes = new HashSet<String>();
		modes.add("walkca");
		con1.setAllowedModes(modes);
		con1.setFreespeed(1.34);
		con1.setCapacity(w);
		con1.setNumberOfLanes(w / 0.61);
		net.addLink(con1);

	}
}
