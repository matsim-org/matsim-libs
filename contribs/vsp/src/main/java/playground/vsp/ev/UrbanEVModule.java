/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.vsp.ev;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.ev.charging.ChargingModule;
import org.matsim.contrib.ev.discharging.DischargingModule;
import org.matsim.contrib.ev.fleet.ElectricFleetModule;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureModule;
import org.matsim.contrib.ev.stats.EvStatsModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Inject;
import com.google.inject.Singleton;

public class UrbanEVModule extends AbstractModule {
	@Inject
	private Config config;

	@Override
	public void install() {
		UrbanEVConfigGroup configGroup = (UrbanEVConfigGroup)config.getModules().get(UrbanEVConfigGroup.GROUP_NAME);
		if (configGroup == null)
			throw new IllegalArgumentException(
					"no config group of type " + UrbanEVConfigGroup.GROUP_NAME + " was specified in the config");

		//standard EV stuff except for ElectricFleetModule
		install(new ChargingInfrastructureModule());
		install(new ChargingModule());
		install(new DischargingModule());
		install(new EvStatsModule());
		install(new ElectricFleetModule());

		//bind custom EVFleet stuff
		bind(ElectricFleetUpdater.class).in(Singleton.class);
		addControlerListenerBinding().to(ElectricFleetUpdater.class);
		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				//this is responsible for charging vehicles according to person activity start and end events..
				bind(UrbanVehicleChargingHandler.class);
				addMobsimScopeEventHandlerBinding().to(UrbanVehicleChargingHandler.class);
			}
		});

		//bind urban ev planning stuff
		addMobsimListenerBinding().to(UrbanEVTripsPlanner.class);
		//TODO find a better solution for this
		Collection<String> whileChargingActTypes = configGroup.getWhileChargingActivityTypes().isEmpty() ?
				config.planCalcScore().getActivityTypes() :
				configGroup.getWhileChargingActivityTypes();
		bind(ActivityWhileChargingFinder.class).toInstance(new ActivityWhileChargingFinder(whileChargingActTypes,
				configGroup.getMinWhileChargingActivityDuration_s()));

		//TODO maybe move this out of this module...
		//bind custom analysis
		//install(new XYModule());
		addEventHandlerBinding().to(ChargerToXY.class).in(Singleton.class);
		addMobsimListenerBinding().to(ChargerToXY.class);
		addEventHandlerBinding().to(ActsWhileChargingAnalyzer.class).in(Singleton.class);
		addControlerListenerBinding().to(ActsWhileChargingAnalyzer.class);
	}

	private Set<String> getOpenBerlinActivityTypes() {
		Set<String> activityTypes = new HashSet<>();
		for (long ii = 600; ii <= 97200; ii += 600) {
			activityTypes.add("home_" + ii + ".0");
			activityTypes.add("work_" + ii + ".0");
			activityTypes.add("leisure_" + ii + ".0");
			activityTypes.add("shopping_" + ii + ".0");
			activityTypes.add("other_" + ii + ".0");
		}
		return activityTypes;
	}

}
