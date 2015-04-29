/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
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

/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.io.File;
import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import playground.jjoubert.projects.wasteCollection.MclarpifMobsimFactory;
import playground.jjoubert.projects.wasteCollection.MclarpifPlanStrategyFactory;
import playground.jjoubert.projects.wasteCollection.MclarpifReplanner;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to run waste collection vehicles.
 * 
 * @author jwjoubert
 */
public class RunWasteExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(RunWasteExample.class.toString(), args);
		
		Scenario sc = setupScenario();
		
		Controler controler = new Controler(sc);
		MclarpifPlanStrategyFactory mclarpifPlanStrategyFactory = new MclarpifPlanStrategyFactory(sc);
		controler.addPlanStrategyFactory("mclarpifSolver", mclarpifPlanStrategyFactory);
		controler.addControlerListener(new MclarpifReplanner());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(MclarpifMobsimFactory.class);
			}
		});
		controler.run();
		
		Header.printFooter();
	}
	
	private static Scenario setupScenario(){
		
		Config config  = getConfig();
		Scenario sc = ScenarioUtils.loadScenario(config);
		
		return sc;
	}
	
	private static Config getConfig(){
		Config config = ConfigUtils.createConfig();
		/* Set input files. */
		config.network().setInputFile("/Volumes/Nifty/workspace/data-wasteExample/network.xml");
		config.plans().setInputFile("/Volumes/Nifty/workspace/data-wasteExample/population.xml");
		config.plans().setInputPersonAttributeFile("/Volumes/Nifty/workspace/data-wasteExample/populationAttributes.xml");
		
		/* Clean and set output directory. */
		String output = "/Volumes/Nifty/workspace/data-wasteExample/output/";
		FileUtils.delete(new File(output));
		config.controler().setOutputDirectory(output);
		
		/* Set simulation attributes. */
		config.controler().setLastIteration(2);
		
		/* Set main simulation modes. */
		String[] mainModes = {"car", "waste"};
		config.qsim().setMainModes(Arrays.asList(mainModes));
		config.plansCalcRoute().setNetworkModes(Arrays.asList(mainModes));
		
		/* Set scoring */
		ActivityParams depot = new ActivityParams("depot");
		depot.setTypicalDuration(Time.parseTime("00:01:00"));
		config.planCalcScore().addActivityParams(depot);
		
		/* Set the replanning strategies. */
		/* ---- Default ---- */
		StrategySettings changeExpBetaStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		changeExpBetaStrategySettings.setWeight(1.0);
		config.strategy().addStrategySettings(changeExpBetaStrategySettings);
		
		/* ---- Waste vehicles ---- */
		StrategySettings mclarpifStrategy = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		mclarpifStrategy.setStrategyName("mclarpifSolver");
		mclarpifStrategy.setSubpopulation("waste");
		mclarpifStrategy.setWeight(1.0);
		config.strategy().addStrategySettings(mclarpifStrategy);
		
		return config;
	}
	
	
	
	
	
	
	
	
}

