/* *********************************************************************** *
 * project: org.matsim.*
 * RunScenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.emissions;

import java.util.HashSet;
import java.util.Set;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;

/**
 * @author benjamin
 *
 */
public class RunEmissionToolOnline {
	
	static String baseDirectory = "../../detailedEval/emissions/testScenario/";
	
	static String networkFile = baseDirectory + "input/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";
	
	static String plansFile = baseDirectory + "input/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";
	
	static String outputDirectory = baseDirectory + "output/";
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();
		Controler controler = new Controler(config);
		
	// controler settings	
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		
	// controlerConfigGroup
		ControlerConfigGroup ccg = controler.getConfig().controler();
		ccg.setOutputDirectory(outputDirectory);
		ccg.setFirstIteration(0);
		ccg.setLastIteration(1);
		ccg.setMobsim("qsim");
		Set set = new HashSet();
		set.add(EventsFileFormat.xml);
		ccg.setEventsFileFormats(set);
		ccg.setRunId("321");
		
	// qsimConfigGroup
		QSimConfigGroup qcg = new QSimConfigGroup();
		qcg.setStartTime(0 * 3600.);
		qcg.setEndTime(30 * 3600.);
		qcg.setFlowCapFactor(0.1);
		qcg.setStorageCapFactor(0.3);
		qcg.setNumberOfThreads(1);
		qcg.setRemoveStuckVehicles(false);
		controler.getConfig().addQSimConfigGroup(qcg);
		
	// planCalcScoreConfigGroup
		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
		
		Set<String> activities = new HashSet<String>();
		activities.add("unknown");
		activities.add("work");
		activities.add("pickup");
		activities.add("with adult");
		activities.add("other");
		activities.add("pvWork");
		activities.add("pvHome");
		activities.add("gvHome");
		activities.add("education");
		activities.add("business");
		activities.add("shopping");
		activities.add("private");
		activities.add("leisure");
		activities.add("sports");
		activities.add("home");
		activities.add("friends");
		
		for(String activity : activities){
			ActivityParams params = new ActivityParams(activity);
			params.setTypicalDuration(30 * 3600);
			pcs.addActivityParams(params);
		}

	// strategy
		StrategyConfigGroup scg = controler.getConfig().strategy();
		StrategySettings strategySettings = new StrategySettings(new IdImpl("1"));
		strategySettings.setModuleName("ChangeExpBeta");
		strategySettings.setProbability(1.0);
		scg.addStrategySettings(strategySettings);
		
	// network
		NetworkConfigGroup ncg = controler.getConfig().network();
		ncg.setInputFile(networkFile);
		
	// plans
		PlansConfigGroup pcg = controler.getConfig().plans();
		pcg.setInputFile(plansFile);
		
		controler.addControlerListener(new EmissionControlerListener());
		controler.run();
	}

}
