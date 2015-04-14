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

package playground.sergioo.typesPopulation2013.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.StrategyManager;
import playground.sergioo.typesPopulation2013.config.groups.StrategyPopsConfigGroup;
import playground.sergioo.typesPopulation2013.controler.corelisteners.PlansDumping;
import playground.sergioo.typesPopulation2013.replanning.StrategyManagerPops;
import playground.sergioo.typesPopulation2013.replanning.StrategyManagerPopsConfigLoader;
import playground.sergioo.typesPopulation2013.scenario.ScenarioUtils;

/**
 * A run Controler for running many types of population
 * 
 * @author sergioo
 */

public class ControlerPops extends Controler {

	public ControlerPops(Config config) {
		super(config);
	}
	public ControlerPops(Scenario scenario) {
		super(scenario);
	}
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.removeModule(StrategyConfigGroup.GROUP_NAME);
		config.addModule(new StrategyPopsConfigGroup());
		ConfigUtils.loadConfig(config, args[0]);
		final ControlerPops controler = new ControlerPops(ScenarioUtils.loadScenario(config));
		controler.setOverwriteFiles(true);
		controler.addCoreControlerListener(new PlansDumping());
        AbstractModule myStrategyManagerModule = new AbstractModule() {

            @Override
            public void install() {
				bind(StrategyManager.class).toInstance(controler.myLoadStrategyManager());
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
