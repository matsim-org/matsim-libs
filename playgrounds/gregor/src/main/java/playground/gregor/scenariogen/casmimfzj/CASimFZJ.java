/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.gregor.scenariogen.casmimfzj;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class CASimFZJ {

	public static void main(String[] args) {
		String input = "/Users/laemmel/devel/gripstest/input/input/config.xml";
		String outputDir = "/Users/laemmel/devel/casimfzj/input/";
		String ooutputDir = "/Users/laemmel/devel/casimfzj/output/";

		Config c = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(c, input);

		Scenario sc = ScenarioUtils.loadScenario(c);

		modifypop(sc);
		modifynet(sc);

		c.network().setInputFile(outputDir + "/network.xml.gz");
		c.plans().setInputFile(outputDir + "/plans.xml.gz");
		c.controler().setOutputDirectory(ooutputDir + "/output/");
		ActivityParams pre = new ActivityParams("work");
		pre.setTypicalDuration(8 * 3600);
		pre.setMinimalDuration(1 * 3600);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(14 * 3600);
		pre.setLatestStartTime(11 * 3600);
		pre.setOpeningTime(7 * 3600);

		ActivityParams post = new ActivityParams("lunch");
		post.setTypicalDuration(30 * 60); // dito
		post.setMinimalDuration(10 * 60);
		post.setClosingTime(13 * 3600 + 30 * 60);
		post.setEarliestEndTime(11 * 3600 + 10 * 60);
		post.setLatestStartTime(13 * 3600 + 20 * 60);
		post.setOpeningTime(11 * 3600);

		sc.getConfig().planCalcScore().addActivityParams(pre);
		sc.getConfig().planCalcScore().addActivityParams(post);

		ActivityParams home = new ActivityParams("home");
		// post.setTypicalDuration(15 * 3600); // dito
		// post.setMinimalDuration(4 * 3600);
		home.setScoringThisActivityAtAll(false);
		sc.getConfig().planCalcScore().addActivityParams(home);
		sc.getConfig().planCalcScore().addActivityParams(pre);
		sc.getConfig().planCalcScore().addActivityParams(post);
		sc.getConfig().planCalcScore().setLateArrival_utils_hr(-18);
		sc.getConfig().planCalcScore().setPerforming_utils_hr(6);
		ModeParams mp = sc.getConfig().planCalcScore()
				.getOrCreateModeParams("walkca");

		c.controler().setMobsim("casim");
		c.global().setCoordinateSystem("EPSG:3395");
		// c.strategy().addParam("Module_1",
		// "playground.gregor.sim2d_v4.replanning.Sim2DReRoutePlanStrategy");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "50");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".8");
		c.strategy().addParam("Module_3", "TimeAllocationMutator");
		c.strategy().addParam("ModuleProbability_3", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_3", "50");

		c.controler().setOutputDirectory(outputDir);
		c.controler().setLastIteration(100);
		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());
		new PopulationWriter(sc.getPopulation(), sc.getNetwork()).write(c
				.plans().getInputFile());
		new ConfigWriter(c).write(outputDir + "/config.xml");
	}

	private static void modifynet(Scenario sc) {
		Set<String> modes = new HashSet<>();
		modes.add("walkca");
		for (Link l : sc.getNetwork().getLinks().values()) {
			l.setAllowedModes(modes);
			l.setCapacity(2);
		}
		Iterator<?> it = sc.getNetwork().getLinks().entrySet().iterator();
		List<Id<Link>> rm = new ArrayList<>();
		while (it.hasNext()) {
			Entry<Id<Link>, ? extends Link> next = (Entry<Id<Link>, ? extends Link>) it
					.next();
			if (next.getKey().toString().contains("el")
					&& !next.getKey().toString().equals("el1")) {
				rm.add(next.getKey());
			}

		}
		for (Id<Link> r : rm) {
			sc.getNetwork().removeLink(r);
		}
		((NetworkImpl) sc.getNetwork()).setCapacityPeriod(1);

	}

	private static void modifypop(Scenario sc) {
		PopulationFactory fac = sc.getPopulation().getFactory();
		for (Person p : sc.getPopulation().getPersons().values()) {
			for (Plan pl : p.getPlans()) {
				ActivityImpl a = (ActivityImpl) pl.getPlanElements().get(0);

				Coord c0 = sc.getNetwork().getLinks()
						.get(Id.createLinkId(a.getLinkId())).getCoord();

				Coord rndC = getGausianRndCoord(c0);
				a.setCoord(rndC);
				double endTime = 12 * 3600. + getGausianEndTime(600., 7200.);
				a.setEndTime(11 * 3600);
				a.setType("work");
				pl.getPlanElements().clear();

				Activity home = fac.createActivityFromLinkId("home",
						Id.createLinkId("el1"));
				((ActivityImpl) home).setCoord(sc.getNetwork().getLinks()
						.get(Id.createLinkId("el1")).getCoord());
				double homeEndTime = 7 * 3600 + getGausianEndTime(600., 7200.);
				home.setEndTime(homeEndTime);
				pl.addActivity(home);
				Leg hw = fac.createLeg("undefined");
				pl.addLeg(hw);

				pl.addActivity(a);

				Leg leg = fac.createLeg("walkca");
				pl.addLeg(leg);
				Activity alunch = fac.createActivityFromLinkId("lunch",
						Id.create("12340", Link.class));
				alunch.setEndTime(endTime + 45 * 60);
				Coord rndC1 = getGausianRndCoord(sc.getNetwork().getLinks()
						.get(alunch.getLinkId()).getCoord());
				((ActivityImpl) alunch).setCoord(rndC1);
				pl.addActivity(alunch);

				Leg leg2 = fac.createLeg("walkca");
				pl.addLeg(leg2);

				Activity a2 = fac.createActivityFromLinkId("work",
						a.getLinkId());
				double endTimeW = 16 * 3600 + 37 * 60
						+ getGausianEndTime(600, 7200);
				a2.setEndTime(endTimeW);
				((ActivityImpl) a2).setCoord(a.getCoord());

				pl.addActivity(a2);

				pl.addLeg(hw);

				Activity home2 = fac.createActivityFromLinkId("home",
						Id.createLinkId("el1"));
				((ActivityImpl) home2).setCoord(sc.getNetwork().getLinks()
						.get(Id.createLinkId("el1")).getCoord());
				pl.addActivity(home2);
			}
		}
	}

	private static Coord getGausianRndCoord(Coord c0) {
		double rx = Double.POSITIVE_INFINITY;
		while (rx > 50) {
			rx = MatsimRandom.getRandom().nextGaussian() * 10;
		}
		double ry = Double.POSITIVE_INFINITY;
		while (ry > 50) {
			ry = MatsimRandom.getRandom().nextGaussian() * 10;
		}
		return new Coord(c0.getX() + rx, c0.getY() + ry);
	}

	private static double getGausianEndTime(double sigma, double range) {
		double nr = MatsimRandom.getRandom().nextGaussian() * sigma;
		while (Math.abs(nr) > range / 2) {
			nr = MatsimRandom.getRandom().nextGaussian() * sigma;
		}
		return nr;
		// return 12 * 3600;
	}
}
