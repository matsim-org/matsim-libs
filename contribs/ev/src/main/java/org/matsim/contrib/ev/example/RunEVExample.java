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

import java.io.IOException;
import java.util.function.Function;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.charging.FastThenSlowCharging;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.routing.EVNetworkRoutingProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;

public class RunEVExample {
	private static final Logger log = Logger.getLogger(RunEVExample.class);

	public static void main(String[] args) throws IOException {

		String configFile;
		if (args.length > 0) {
			log.info("Starting simulation run with the following arguments:");
			configFile = args[0];
			log.info("config file: " + configFile);

		} else {
			log.info("Starting simulation run with example config file from resource path");

			//load config file from resource path (see src/main/resources folder for the example)
			configFile = "config.xml";
		}
		new RunEVExample().run(configFile);
	}

	public void run(String configFile) {
		Config config = ConfigUtils.loadConfig(configFile, new EvConfigGroup());
		config.controler()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Function<Charger, ChargingStrategy> chargingStrategyFactory = charger -> new FastThenSlowCharging(
				charger.getPower());
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new EvModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addRoutingModuleBinding(TransportMode.car).toProvider(new EVNetworkRoutingProvider(TransportMode.car));
				bind(ChargingLogic.Factory.class).toInstance(
						charger -> new ChargingWithQueueingAndAssignmentLogic(charger,
								chargingStrategyFactory.apply(charger)));

				installQSimModule(new AbstractQSimModule() {
					@Override
					protected void configureQSim() {
						bind(VehicleChargingHandler.class).asEagerSingleton();
					}
				});
			}
		});
		controler.configureQSimComponents(components -> components.addNamedComponent(EvModule.EV_COMPONENT));

		controler.run();
	}
}
