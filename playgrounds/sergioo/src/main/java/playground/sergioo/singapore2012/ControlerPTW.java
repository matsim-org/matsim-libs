/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerWW.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,  *
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

package playground.sergioo.singapore2012;

//import java.util.HashSet;

//import org.matsim.api.core.v01.Id;

//import playground.artemc.calibration.CalibrationStatsListener;

import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.StrategyManager;
import playground.sergioo.singapore2012.scoringFunction.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.sergioo.typesPopulation2013.analysis.ScoreStats;
import playground.sergioo.typesPopulation2013.config.groups.StrategyPopsConfigGroup;
import playground.sergioo.typesPopulation2013.controler.corelisteners.LegHistogramListener;
import playground.sergioo.typesPopulation2013.replanning.StrategyManagerPops;
import playground.sergioo.typesPopulation2013.replanning.StrategyManagerPopsConfigLoader;
import playground.sergioo.typesPopulation2013.scenario.ScenarioUtils;


/**
 * A run Controler for a transit router that depends on the travel times and wait times
 * 
 * @author sergioo
 */

public class ControlerPTW extends Controler {

	public ControlerPTW(Scenario scenario) {
		super(scenario);
	}
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.removeModule(StrategyConfigGroup.GROUP_NAME);
		config.addModule(new StrategyPopsConfigGroup());
		ConfigUtils.loadConfig(config, args[0]);
		final ControlerPTW controler = new ControlerPTW(ScenarioUtils.loadScenario(config));
		controler.setOverwriteFiles(true);
        controler.addCoreControlerListener(new LegHistogramListener(controler.getEvents(), true, controler.getScenario().getPopulation()));
        controler.addCoreControlerListener(new ScoreStats(controler.getScenario().getPopulation(), ScoreStatsControlerListener.FILENAME_SCORESTATS, true));
		//controler.addControlerListener(new CalibrationStatsListener(controler.getEvents(), new String[]{args[1], args[2]}, 1, "Travel Survey (Benchmark)", "Red_Scheme", new HashSet<Id<Person>>()));
		controler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getScenario()));
        AbstractModule myStrategyManagerModule = new AbstractModule() {

            @Override
            public void install() {
                bindToInstance(StrategyManager.class, controler.myLoadStrategyManager());
            }
        };
        controler.addOverridingModule(myStrategyManagerModule);
        controler.run();
	}

	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	private StrategyManager myLoadStrategyManager() {
		StrategyManagerPops manager = new StrategyManagerPops();
		StrategyManagerPopsConfigLoader.load(this, manager);
		return manager;
	}
}
