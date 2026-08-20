/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.run.examples;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.router.DvrpRoutingModuleProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpModeLimitedMaxSpeedTravelTimeModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;

/**
 * @author michal.mac
 */
public class RunDrt2DrtExample {

	static void main(String[] args) {
//		T:\Tilmann\BS_Modell\09_Intermove_Data_Transfer\dvrp-grid
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"),
			"multi_mode_one_shared_taxi_config.xml");
		RunDrt2DrtExample.run(configUrl, false, 0);
	}

	public static void run(URL configUrl, boolean otfvis, int lastIteration) {
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.controller().setLastIteration(lastIteration);

		Controler controler = DrtControlerCreator.createControler(config, otfvis);

		// max allowed speed for AV
		double maxSpeed = controler.getScenario()
				.getVehicles()
				.getVehicleTypes()
				.get(Id.create("autonomous_vehicle", VehicleType.class))
				.getMaximumVelocity();

		controler.addOverridingModule(
				new DvrpModeLimitedMaxSpeedTravelTimeModule("drt_autonomous", config.qsim().getTimeStepSize(),
						maxSpeed));

		controler.addOverridingModule(new AbstractDvrpModeModule("braunschweig") {

			@Override
			public void install() {
				modalMapBinder(DvrpRoutingModuleProvider.Stage.class, RoutingModule.class).addBinding(
						DvrpRoutingModuleProvider.Stage.ACCESS)
					.to(Key.get(RoutingModule.class, Names.named("schwarzerBerg")));// not singleton

			}
		});

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		controler.run();
	}
}
