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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.discharging.VehicleTypeSpecificDriveEnergyConsumptionFactory;
import org.matsim.contrib.ev.infrastructure.LTHConsumptionModelReader;
import org.matsim.contrib.ev.routing.EvNetworkRoutingProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Runs a sample EV run using a vehicle consumption model designed at LTH in Lund which takes the speed and the slope of a link into account.
 * Link slopes may be added using a double array on the network.
 * The consumption maps are based on Domingues, Gabriel. / Modeling, Optimization and Analysis of Electromobility Systems. Lund : Department of Biomedical Engineering, Lund university, 2018. 169 p., PhD thesis
 */
public class RunEvExamplewithLTHConsumptionModel {
	static final String DEFAULT_CONFIG_FILE = "test/input/org/matsim/contrib/ev/example/RunEvExample/config.xml";
	private static final Logger log = Logger.getLogger(RunEvExamplewithLTHConsumptionModel.class);

	public static void main(String[] args) throws IOException {
		final URL configUrl;
		if (args.length > 0) {
			log.info("Starting simulation run with the following arguments:");
			configUrl = new URL(args[0]);
			log.info("config URL: " + configUrl);
		} else {
			File localConfigFile = new File(DEFAULT_CONFIG_FILE);
			if (localConfigFile.exists()) {
				log.info("Starting simulation run with the local example config file");
				configUrl = localConfigFile.toURI().toURL();
			} else {
				log.info("Starting simulation run with the example config file from GitHub repository");
				configUrl = new URL("https://raw.githubusercontent.com/matsim-org/matsim/master/contribs/ev/"
						+ DEFAULT_CONFIG_FILE);
			}
		}
		new RunEvExamplewithLTHConsumptionModel().run(configUrl);
	}

	public void run(URL configUrl) {
		Config config = ConfigUtils.loadConfig(configUrl, new EvConfigGroup());
		config.controler()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory("output/evExampleLTH");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		VehicleTypeSpecificDriveEnergyConsumptionFactory driveEnergyConsumptionFactory = new VehicleTypeSpecificDriveEnergyConsumptionFactory();
		driveEnergyConsumptionFactory.addEnergyConsumptionModelFactory("defaultVehicleType",
				new LTHConsumptionModelReader(Id.create("defaultVehicleType", VehicleType.class)).readFile(
						ConfigGroup.getInputFileURL(config.getContext(), "MidCarMap.csv").getFile()));

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new EvModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(DriveEnergyConsumption.Factory.class).toInstance(driveEnergyConsumptionFactory);
				bind(AuxEnergyConsumption.Factory.class).toInstance(electricVehicle -> (beginTime, duration, linkId) -> 0); //a dummy factory, as aux consumption is part of the drive consumption in the model
				addRoutingModuleBinding(TransportMode.car).toProvider(new EvNetworkRoutingProvider(TransportMode.car));
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
