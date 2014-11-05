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

package playground.gregor.scenariogen.qsim2casimconverter;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class QSim2CASimConverter {

	public static void main(String[] args) {
		String inputDir = "/Users/laemmel/svn/runs-svn/run1358/output";
		String outputDir = "/Users/laemmel/devel/padang/r4.2/input";
		String newOutputDir = "/Users/laemmel/devel/padang/r4.2/output";

		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sc);
		nr.parse(inputDir + "/output_network.xml.gz");
		convertNet(sc.getNetwork());
		NetworkWriter nw = new NetworkWriter(sc.getNetwork());
		nw.write(outputDir + "/network.xml.gz");

		PopulationReader pr = new MatsimPopulationReader(sc);
		pr.readFile(inputDir + "/ITERS/it.0/0.plans.xml.gz");
		convertPlans(sc.getPopulation());
		PopulationWriter pw = new PopulationWriter(sc.getPopulation(),
				sc.getNetwork());
		pw.write(outputDir + "/population.xml.gz");

		c.network().setInputFile(outputDir + "/network.xml.gz");

		// c.strategy().addParam("Module_1",
		// "playground.gregor.sim2d_v4.replanning.Sim2DReRoutePlanStrategy");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "50");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".9");

		c.controler().setOutputDirectory(newOutputDir);
		c.controler().setLastIteration(100);

		c.plans().setInputFile(outputDir + "/population.xml.gz");

		ActivityParams pre = new ActivityParams("pre-evac");
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

		ActivityParams post = new ActivityParams("post-evac");
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
		qsim.setEndTime(20 * 60);
		c.controler().setMobsim("casim");
		c.global().setCoordinateSystem("EPSG:32747");

		c.qsim().setEndTime(21 * 3600);

		new ConfigWriter(c).write(outputDir + "/config.xml");

	}

	private static void convertPlans(Population population) {

		// Iterator<? extends Person> it = population.getPersons().values()
		// .iterator();
		// while (it.hasNext()) {
		// it.next();
		// if (MatsimRandom.getRandom().nextDouble() > 0.0001) {
		// it.remove();
		// }
		// }

		for (Person p : population.getPersons().values()) {
			for (Plan pl : p.getPlans()) {
				int cnt = 0;
				for (PlanElement el : pl.getPlanElements()) {
					if (el instanceof Leg) {
						((Leg) el).setMode("walkca");
						((LegImpl) el).setRoute(null);
					} else if (el instanceof ActivityImpl) {

						((ActivityImpl) el).setCoord(null);
						if (cnt == 0) {
							((ActivityImpl) el).setType("pre-evac");
						} else {
							((ActivityImpl) el).setType("post-evac");
						}
						cnt++;
					}
				}
			}
		}

	}

	private static void convertNet(Network network) {
		Set<String> modes = new HashSet<>();
		modes.add("walkca");
		for (Link l : network.getLinks().values()) {
			l.setAllowedModes(modes);
			double cap = l.getNumberOfLanes() * 0.61;
			l.setCapacity(cap);

			if (l.getId().toString().contains("el")) {
				l.setLength(50);
				l.setCapacity(10);
			}
		}

	}

}
