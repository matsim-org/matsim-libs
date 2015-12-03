/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.test.test4;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig() ;
		
		String name = null ;

		int lastIteration = 1000 ;
		config.controler().setLastIteration(lastIteration);
		StrategySettings stratSets = new StrategySettings() ;
		stratSets.setStrategyName(name);
		stratSets.setDisableAfter( (int)(0.8*lastIteration) );
		config.strategy().addStrategySettings(stratSets);
		
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		

		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		Controler controler = new Controler( scenario ) ;
		
		controler.addOverridingModule( new AbstractModule(){
			@Override
			public void install() {
				this.bindMobsim().toProvider( MyMobsimProvider.class ) ;
			}
		});
		
		
		controler.run();
	}

}
