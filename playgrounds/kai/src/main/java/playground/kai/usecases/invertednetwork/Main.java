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
package playground.kai.usecases.invertednetwork;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class Main {

	private Main() {
	}

	public static void main(String[] args) {
		Config config ;
		if ( args!=null && args.length >=1 ) {
			config = ConfigUtils.loadConfig( args[0] ) ;
		} else {
			config = ConfigUtils.createConfig() ;
		}
		
		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		Controler ctrl = new Controler( scenario ) ;
		
		final TravelDisutilityFactory tdf = ctrl.getTravelDisutilityFactory() ;
		// (should probably use inject syntax in routing module. kai, oct'15)
		
		ctrl.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.addRoutingModuleBinding(TransportMode.car).toInstance(new InvertedRoutingModule(scenario,tdf) ) ; 
			}
		});
		
		ctrl.run();
	}

}
