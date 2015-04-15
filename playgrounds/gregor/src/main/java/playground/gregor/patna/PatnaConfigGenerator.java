/* *********************************************************************** *
 * project: org.matsim.*
 * PatnaConfigGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.gregor.patna;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;


public class PatnaConfigGenerator {
	
	public static void main(String [] args) {
		String networkFile = "/Users/laemmel/svn/runs-svn/patnaIndia/run105/input/evac_network_gregor_s_version.xml.gz";
		String plansFile = "/Users/laemmel/svn/runs-svn/patnaIndia/run105/input/evac_plans_gregor_s_version.xml.gz";
		String inputDir = "/Users/laemmel/devel/patna/input";
		String outputDir = "/Users/laemmel/devel/patna/output";
		
		Config c = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(c);
		
		Network net = scenario.getNetwork();
		MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
		nr.readFile(networkFile);
		MatsimPopulationReader pr = new MatsimPopulationReader(scenario);
		pr.readFile(plansFile);
		
		
		c.network().setInputFile(inputDir + "/network.xml.gz");

		//c.strategy().addParam("Module_1", "playground.gregor.sim2d_v4.replanning.Sim2DReRoutePlanStrategy");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "75");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".9");
		c.strategy().setMaxAgentPlanMemorySize(5);

		c.controler().setOutputDirectory(outputDir);
		c.controler().setLastIteration(100);
		c.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra);
		
		
		Collection<String> netmodes = new ArrayList<>();
		netmodes.add("car");netmodes.add("bike");netmodes.add("motorbike");
		c.plansCalcRoute().setNetworkModes(netmodes );
		Map<String, ? extends Collection<? extends ConfigGroup>> sets = c.plansCalcRoute().getParameterSets();
		Iterator<? extends ConfigGroup> it = sets.values().iterator().next().iterator();
		it.next();
		it.remove();
		
		
		c.plans().setInputFile(inputDir + "/population.xml.gz");

		ActivityParams pre = new ActivityParams("home");
		// needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		pre.setTypicalDuration(49); 
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);

		ActivityParams post = new ActivityParams("evac");
		post.setTypicalDuration(49); 
		post.setMinimalDuration(49);
		post.setClosingTime(49);
		post.setEarliestEndTime(49);
		post.setLatestStartTime(49);
		post.setOpeningTime(49);
		scenario.getConfig().planCalcScore().addActivityParams(pre);
		scenario.getConfig().planCalcScore().addActivityParams(post);
		scenario.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
		scenario.getConfig().planCalcScore().setPerforming_utils_hr(0.);


		QSimConfigGroup qsim = scenario.getConfig().qsim();
		qsim.setEndTime(20*60);
		qsim.setStuckTime(100000);
		c.global().setCoordinateSystem("EPSG:24345");
		c.qsim().setEndTime(30*3600);
		
		c.travelTimeCalculator().setTraveltimeBinSize(900);
		
		
		new ConfigWriter(c).write(inputDir+ "/config.xml");
		new NetworkWriter(scenario.getNetwork()).write(c.network().getInputFile());
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(c.plans().getInputFile());
	}

}
