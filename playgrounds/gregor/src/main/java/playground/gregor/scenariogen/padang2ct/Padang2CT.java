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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by laemmel on 13/10/15.
 */
public class Padang2CT {
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

		loadAndModifyNetwork(sc);
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

		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());

		loadPopulation(sc);

		Population pop = sc.getPopulation();
		new PopulationWriter(pop, sc.getNetwork(), 1).write(c.plans()
				.getInputFile());

//		CTRunner.main(new String[]{inputDir + "/config.xml", "false"});

	}

	private static void loadAndModifyNetwork(Scenario sc) {
		new MatsimNetworkReader(sc).readFile(PDG_INPUT + "/output_network.xml.gz");
		Set<String> mode = new HashSet<>();
		mode.add("walkct");
		for (Link l : sc.getNetwork().getLinks().values()) {
			if (l.getId().toString().contains("el")) {
				l.setCapacity(100 * 1.33);
			}
			l.setAllowedModes(mode);
		}
	}

	private static void loadPopulation(Scenario sc) {
		new MatsimPopulationReader(sc).readFile(PDG_INPUT + "/output_plans.xml.gz");
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
	}


}
