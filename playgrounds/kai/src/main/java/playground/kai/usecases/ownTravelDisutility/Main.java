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
package playground.kai.usecases.ownTravelDisutility;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Arrays;

/**
 * @author nagel
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		
		config.strategy().setPlanSelectorForRemoval( "MySelectorForRemoval" );
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		Controler ctrl = new Controler( scenario ) ;

		// create my own disutlity:
		final TravelDisutility travelDisutility = new MyTravelDisutility() ;
		
		// wrap it into a "Module" for the TravelDisutility class:
		final AbstractModule abstractModule = new AbstractModule() {
			@Override
			public void install() {
				bind(TravelDisutility.class).toInstance(travelDisutility);
			}
		};
		
		// override the default TravelDisutility entry:
		AbstractModule modules = AbstractModule.override(Arrays.asList(new ControlerDefaultsModule()), abstractModule) ;

		// set the result in the controler:
		ctrl.setModules( modules );
		
		ctrl.run();

	}

}
