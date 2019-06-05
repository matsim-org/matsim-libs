/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.fleet;

import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ElectricFleetModule extends AbstractModule {
	@Inject
	private EvConfigGroup evCfg;

	@Override
	public void install() {
		bind(ElectricFleetSpecification.class).toProvider(() -> {
			ElectricFleetSpecification fleetSpecification = new ElectricFleetSpecificationImpl();
			new ElectricFleetReader(fleetSpecification).parse(
					ConfigGroup.getInputFileURL(getConfig().getContext(), evCfg.getVehiclesFile()));
			return fleetSpecification;
		}).asEagerSingleton();

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ElectricFleet.class).toProvider(new Provider<ElectricFleet>() {
					@Inject
					private ElectricFleetSpecification fleetSpecification;
					@Inject
					private DriveEnergyConsumption.Factory driveConsumptionFactory;
					@Inject(optional = true)
					private AuxEnergyConsumption.Factory auxConsumptionFactory;

					@Override
					public ElectricFleet get() {
						return ElectricFleets.createDefaultFleet(fleetSpecification, driveConsumptionFactory,
								auxConsumptionFactory);
					}
				}).asEagerSingleton();
			}
		});

		addControlerListenerBinding().to(InitAtIterationStart.class);
	}

	private static class InitAtIterationStart implements IterationStartsListener {
		@Inject
		private ChargingInfrastructure chargingInfrastructure;
		@Inject
		private ChargingLogic.Factory logicFactory;
		@Inject
		private EventsManager eventsManager;

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			chargingInfrastructure.initChargingLogics(logicFactory, eventsManager);
		}
	}
}
