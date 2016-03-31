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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class RunConfigurableQNetworkFactoryExample {


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig() ;
		
		final Scenario scenario = ScenarioUtils.createScenario( config ) ;
		
		Controler controler = new Controler( scenario ) ;
		
		controler.addOverridingModule( new AbstractModule(){
			@Inject private EventsManager events ;
			@Override public void install() {
				final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory( events, scenario ) ;
				factory.setLinkSpeedCalculator(null); // fill with something reasonable
				bind( QNetworkFactory.class ).toInstance( factory ) ;
				// NOTE: Other than when using a provider, this uses the same factory instance over all iterations, re-configuring 
				// it in every iteration via the initializeFactory(...) method. kai, mar'16 
			}
		});
		
		controler.run();
		
	}

}
