/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.kai.usecases.plansremoval;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * @author nagel
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String selectorName = "MySelectorForRemoval" ;

		Config config = ConfigUtils.loadConfig( args[0] ) ;
		
		config.strategy().setPlanSelectorForRemoval( selectorName );
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		Controler ctrl = new Controler( scenario ) ;
		
		// create a controler defaults "module":
		AbstractModule modules = new ControlerDefaultsModule() ;

		// create the module that adds the new functionality:
		AbstractModule modules2 = new AbstractModule(){
			@Override
			public void install() {
				PlanSelector<Plan,Person> selector = new MySelectorForRemoval() ;
				if (getConfig().strategy().getPlanSelectorForRemoval().equals(selectorName)) {
					bindPlanSelectorForRemoval().toInstance( selector);
				}
			}
		} ; 

		// set both of them simultaneously ; 
		ctrl.setModules(modules,modules2);
		
		ctrl.run();

	}

}
