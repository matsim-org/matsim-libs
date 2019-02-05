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

package org.matsim.contrib.ev.data;

import javax.inject.Inject;

import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.data.file.ElectricFleetProvider;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ElectricFleetModule extends AbstractModule {
	private final EvConfigGroup evCfg;

	public ElectricFleetModule(EvConfigGroup evCfg) {
		this.evCfg = evCfg;
	}

	@Override
	public void install() {
		bind(ElectricFleet.class).toProvider(
				new ElectricFleetProvider(evCfg.getVehiclesFileUrl(getConfig().getContext()))).asEagerSingleton();
		addControlerListenerBinding().to(InitAtIterationStart.class);

	}

	private static class InitAtIterationStart implements IterationStartsListener {
		@Inject
		private ElectricFleet evFleet;
		@Inject
		private ChargingInfrastructure chargingInfrastructure;
		@Inject
		private ChargingLogic.Factory logicFactory;
		@Inject
		private EventsManager eventsManager;
		@Inject
		private DriveEnergyConsumption.Factory driveConsumptionFactory;
		@Inject
		private AuxEnergyConsumption.Factory auxConsumptionFactory;

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			evFleet.resetBatteriesAndConsumptions(driveConsumptionFactory, auxConsumptionFactory);
			chargingInfrastructure.initChargingLogics(logicFactory, eventsManager);
		}
	}

}
