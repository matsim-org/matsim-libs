/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.example;/*
 * created by jbischoff, 19.03.2019
 */

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.routing.EvNetworkRoutingProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;

public class RunEvExample {
	static final String DEFAULT_CONFIG_FILE = "test/input/org/matsim/contrib/ev/example/RunEvExample/config.xml";
	private static final Logger log = LogManager.getLogger(RunEvExample.class);

	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			log.info("Starting simulation run with the following arguments:");
			log.info("args: " + Arrays.toString( args ) );
		} else {
			if ( new File(DEFAULT_CONFIG_FILE).exists()) {
				log.info("Starting simulation run with the local example config file");
				args[0] = DEFAULT_CONFIG_FILE;
			} else {
				log.info("Starting simulation run with the example config file from GitHub repository");
				args[0] = "https://raw.githubusercontent.com/matsim-org/matsim/master/contribs/ev/" + DEFAULT_CONFIG_FILE;
			}
		}
		new RunEvExample().run(args);
	}

	public void run( String [] args ) {
		Config config = ConfigUtils.loadConfig(args, new EvConfigGroup());
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		//--
		Scenario scenario = ScenarioUtils.loadScenario(config);
		//--
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new EvModule());
		// (ok)

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( this.getConfig(), QSimComponentsConfigGroup.class );
				List<String> cmps = qsimComponentsConfig.getActiveComponents();
				cmps.add(  EvModule.EV_COMPONENT ) ;
				qsimComponentsConfig.setActiveComponents( cmps );


				addRoutingModuleBinding(TransportMode.car).toProvider(new EvNetworkRoutingProvider(TransportMode.car));
				// (I assume that this is not on EvModule since one might want to evaluate if a standard route leads to an empty battery or not.  ???)

				installQSimModule(new AbstractQSimModule() {
					@Override protected void configureQSim() {
//						bind(VehicleChargingHandler.class).asEagerSingleton();
						// this can be added to next line (does not need separate binding).

						addMobsimScopeEventHandlerBinding().to(VehicleChargingHandler.class).asEagerSingleton();
						// (possibly not in EvModule because one wants to leave the decision to generate these events to the user??)
						// (leaving this out fails the events equality)
					}
				});
			}
		});

//		controler.configureQSimComponents(components -> components.addNamedComponent(EvModule.EV_COMPONENT));
		// (replaced by "cmps.add( ...)" above.)

		//--
		controler.run();
	}
}
