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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * @author nagel
 *
 */
public class RunConfigurableQNetworkFactoryExample {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig(args[0]) ;
		
		run(config);
		
	}

	static void run(Config config) {
		final Scenario scenario = ScenarioUtils.createScenario( config ) ;
		
		Controler controler = new Controler( scenario ) ;
		
		final EventsManager events = controler.getEvents() ;
		
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {}
			
			@Provides @Singleton
			QNetworkFactory provideQNetworkFactory(MobsimTimer mobsimTimer, AgentCounter agentCounter) {
				final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory( events, scenario, mobsimTimer, agentCounter ) ;
				factory.setLinkSpeedCalculator(null); // fill with something reasonable
				factory.setTurnAcceptanceLogic(null); // fill with something reasonable
				// NOTE: Other than when using a provider, this uses the same factory instance over all iterations, re-configuring 
				// it in every iteration via the initializeFactory(...) method. kai, mar'16 
				return factory;
			}
		});
		
		controler.run();
	}

}
