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
import javax.inject.Provider;

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

	public static class MyFactoryProvider implements Provider<QNetworkFactory> {
		@Inject private EventsManager events ;
		@Inject private Scenario scenario ;
		@Override public QNetworkFactory get() {
			ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory( events, scenario ) ;
			factory.setLinkSpeedCalculator(null); // fill with something reasonable
			return factory ;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig() ;
		
		Scenario scenario = ScenarioUtils.createScenario( config ) ;
		
		Controler controler = new Controler( scenario ) ;
		
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				bind( QNetworkFactory.class ).toProvider( MyFactoryProvider.class ) ;
			}
		});
		
		controler.run();
		
	}

}
