/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package tutorial.programming.example11PluggablePlanStrategyInCode;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

public class RunPluggablePlanStrategyInCodeExample {
	private static final String STRATEGY_NAME = "doSomethingSpecial";

	public static void main(final String[] args) {
		ControlerUtils.initializeOutputLogging();
		

		Config config;
		if ( args.length==0 ) {
			config = ConfigUtils.loadConfig("examples/equil/config.xml") ;
		} else {
			config = ConfigUtils.loadConfig(args[0]);
		}
		
		//add a strategy to the config
		StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(STRATEGY_NAME);
		stratSets.setWeight(0.1);
		config.strategy().addStrategySettings(stratSets);
		
		//let the output directory be overwritten
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		//only run one iteration
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		
		final Controler controler = new Controler(config);
		
		//add the binding strategy 
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding(STRATEGY_NAME).toProvider(MyPlanStrategyFactory.class);
			}
		});
		controler.run();

	}

}
