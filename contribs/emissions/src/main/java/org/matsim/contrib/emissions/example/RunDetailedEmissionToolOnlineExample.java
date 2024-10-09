/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOnline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.emissions.example;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * After creating a config file with
 * {@link CreateEmissionConfig CreateEmissionConfig}
 * this class runs a simulation and calculates emissions online.
 * Results are written into events file (including emission events) for some iterations (as specified by the config).
 * <p></p>
 * See <a href="{@docRoot}/src-html/org/matsim/contrib/emissions/example/RunEmissionToolOnlineExample.html#line.39">here</a> for the listing.

 *
 * @author benjamin, julia
 */

public final class RunDetailedEmissionToolOnlineExample {

//	private static final String configFile = "./scenarios/sampleScenario/testv2_Vehv1/config_detailed.xml";

	public static Config prepareConfig( String[] args ) {
		Config config;
//		if ( args == null || args.length == 0 ) {
//			config = ConfigUtils.loadConfig( configFile, new EmissionsConfigGroup() );
//		} else {
			config = ConfigUtils.loadConfig( args, new EmissionsConfigGroup() );
//		}
		return config;
	}

	public static void run( Scenario scenario, AbstractModule... modules ) {
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(EmissionModule.class).asEagerSingleton();
			}
		});
		for ( AbstractModule module : modules ) {
			controler.addOverridingModule( module );
		}
		controler.run();
	}
	public static void main(String[] args) {
		Config config = prepareConfig( args ) ;

		Scenario scenario = ScenarioUtils.loadScenario(config);
		run( scenario ) ;
	}

}
