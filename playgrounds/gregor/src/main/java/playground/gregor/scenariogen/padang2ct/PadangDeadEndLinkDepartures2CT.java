package playground.gregor.scenariogen.padang2ct;
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
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by laemmel on 13/10/15.
 */
public class PadangDeadEndLinkDepartures2CT {
	private static final String PDG_INPUT = "/Users/laemmel/svn/runs-svn/run1010/output/";
	private static final String OUT_DIR = "/Users/laemmel/devel/padang/";
	private static final int NR_AGENTS = 1000;

	public static void main(String[] args) throws IOException {


		FileUtils.deleteDirectory(new File(OUT_DIR));
		String inputDir = OUT_DIR + "/input";
		String outputDir = OUT_DIR + "/output";
		new File(inputDir).mkdirs();

		Config c = ConfigUtils.createConfig();

		c.global().setCoordinateSystem("EPSG:3395");
		Scenario sc = ScenarioUtils.createScenario(c);

		Set<Id<Link>> deadEndLinks = loadAndModifyNetwork(sc);
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

		c.qsim().setEndTime(6 * 3600);

		new ConfigWriter(c).write(inputDir + "/config.xml");


		Set<Id<Link>> validLinks = loadPopulation(sc, deadEndLinks);
		reviseNetwork(sc, validLinks);
		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());
		revisePopulation(sc);
		Population pop = sc.getPopulation();
		new PopulationWriter(pop, sc.getNetwork(), 1.).write(c.plans()
				.getInputFile());

//		CTRunner.main(new String[]{inputDir + "/config.xml", "false"});


	}

	private static void revisePopulation(Scenario sc) {
		PopulationFactory fac = sc.getPopulation().getFactory();
		List<Person> rm = new ArrayList<>();
		List<Person> add = new ArrayList<>();
		boolean rmPers = false;
		for (Person pers : sc.getPopulation().getPersons().values()) {
			for (Plan plan : pers.getPlans()) {

				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						Leg leg = ((Leg) pe);
						LinkNetworkRouteImpl r = (LinkNetworkRouteImpl) leg.getRoute();
						Id<Link> firstId = r.getStartLinkId();
						if (r.getLinkIds().size() == 0) {
							rm.add(pers);
							rmPers = true;
							break;
						}
						Id<Link> secondId = r.getLinkIds().get(0);
						Link firstLink = sc.getNetwork().getLinks().get(firstId);
						Link secondLink = sc.getNetwork().getLinks().get(secondId);
						if (firstLink.getFromNode() == secondLink.getToNode()) {
							rm.add(pers);
							Person p = fac.createPerson(pers.getId());
							Plan pl = fac.createPlan();
							p.addPlan(pl);
							Activity a0 = fac.createActivityFromLinkId(((Activity) plan.getPlanElements().get(0)).getType(), secondId);
							a0.setEndTime(((Activity) plan.getPlanElements().get(0)).getEndTime());
							pl.addActivity(a0);
							Leg l = fac.createLeg("walkct");
							pl.addLeg(l);
							Activity a1 = fac.createActivityFromLinkId(((Activity) plan.getPlanElements().get(2)).getType(), ((Activity) plan.getPlanElements().get(2)).getLinkId());
							pl.addActivity(a1);
							add.add(p);
							rmPers = true;
							break;
						}

					}
				}
				if (rmPers) {
					rmPers = false;
					break;
				}
			}
		}
		for (Person r : rm) {
			sc.getPopulation().getPersons().remove(r.getId());
		}
		for (Person a : add) {
			sc.getPopulation().addPerson(a);
		}
	}

	private static void reviseNetwork(Scenario sc, Set<Id<Link>> validLinks) {
		//sc.getNetwork().getLinks() returns an unmodifiyable map
		List<Link> rm = new ArrayList<>();
		for (Link l : sc.getNetwork().getLinks().values()) {
			if (!validLinks.contains(l.getId())) {
				rm.add(l);
			}
		}
		for (Link l : rm) {
			sc.getNetwork().removeLink(l.getId());
		}

		List<Node> rn = new ArrayList<>();
		for (Node n : sc.getNetwork().getNodes().values()) {
			if (n.getInLinks().size() == 0 && n.getOutLinks().size() == 0) {
				rn.add(n);
			}
		}
		for (Node n : rn) {
			sc.getNetwork().removeNode(n.getId());
		}

//		new NetworkCleaner().run(sc.getNetwork());

	}

	private static Set<Id<Link>> loadAndModifyNetwork(Scenario sc) {
		new MatsimNetworkReader(sc).readFile(PDG_INPUT + "/output_network.xml.gz");
		Set<String> mode = new HashSet<>();
		mode.add("walkct");
		Set<Id<Link>> ret = new HashSet<>();
		for (Link l : sc.getNetwork().getLinks().values()) {
			if (l.getId().toString().contains("el")) {
				l.setCapacity(20 * 1.33);
				l.setLength(10);
			}
			if (l.getId().toString().equals("el1")) {
				l.setCapacity(200 * 1.33);
				l.setLength(10);
			}
			l.setAllowedModes(mode);
			if (l.getToNode().getInLinks().size() == 1 || l.getFromNode().getOutLinks().size() == 1) {
				ret.add(l.getId());
			}
		}
//		ret.clear();
//		ret.add(Id.createLinkId("111039"));
//		ret.add(Id.createLinkId("11039"));
		return ret;
	}

	private static Set<Id<Link>> loadPopulation(Scenario sc, Set<Id<Link>> deadEndLinks) {
		new MatsimPopulationReader(sc).readFile(PDG_INPUT + "/output_plans.xml.gz");
		Set<Person> rm = new HashSet<>();
		for (Person pers : sc.getPopulation().getPersons().values()) {
			for (Plan plan : pers.getPlans()) {
				boolean flipFlop = true;
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						((Leg) pe).setMode("walkct");

					}
					else {
						if (pe instanceof Activity) {
							if (flipFlop) {
								((Activity) pe).setType("origin");
								if (!deadEndLinks.contains(((Activity) pe).getLinkId())) {
									rm.add(pers);
								}
							}
							else {
								((Activity) pe).setType("destination");
							}
							flipFlop = !flipFlop;
						}
					}
				}
			}
		}
		rm.clear();
		for (Person p : rm) {
			sc.getPopulation().getPersons().remove(p.getId());
		}

		Set<Id<Link>> validLinks = new HashSet<>();
		for (Person pers : sc.getPopulation().getPersons().values()) {
			for (Plan plan : pers.getPlans()) {
				boolean flipFlop = true;
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						Leg leg = ((Leg) pe);
						LinkNetworkRouteImpl r = (LinkNetworkRouteImpl) leg.getRoute();
						validLinks.add(r.getEndLinkId());
						validLinks.add(r.getStartLinkId());
						for (Id<Link> l : r.getLinkIds()) {
							validLinks.add(l);
						}

					}
					else {
						if (pe instanceof Activity) {
							validLinks.add(((Activity) pe).getLinkId());

						}
					}
				}
			}
		}

		return validLinks;
	}


}
