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

package playground.singapore.typesPopulation.controler;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.StrategyManager;
import playground.singapore.typesPopulation.config.groups.StrategyPopsConfigGroup;
import playground.singapore.typesPopulation.controler.corelisteners.PlansDumping;
import playground.singapore.typesPopulation.replanning.StrategyManagerPops;
import playground.singapore.typesPopulation.replanning.StrategyManagerPopsConfigLoader;
import playground.singapore.typesPopulation.scenario.ScenarioUtils;

/**
 * A run Controler for running many types of population
 * 
 * @author sergioo
 */

public class ControlerPops {

	private static Controler controler;
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.removeModule(StrategyConfigGroup.GROUP_NAME);
		config.addModule(new StrategyPopsConfigGroup());
		ConfigUtils.loadConfig(config, args[0]);
		controler = new Controler(ScenarioUtils.loadScenario(config));
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.addControlerListener(new PlansDumping());
        AbstractModule myStrategyManagerModule = new AbstractModule() {

            @Override
            public void install() {
				bind(StrategyManager.class).toInstance(myLoadStrategyManager());
			}
        };
        controler.addOverridingModule(myStrategyManagerModule);
        controler.run();
	}

	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	private static StrategyManager myLoadStrategyManager() {
		StrategyManagerPops manager = new StrategyManagerPops();
		controler.addControlerListener(manager);
		StrategyManagerPopsConfigLoader.load(controler, manager);
		return manager;
	}
	
}
