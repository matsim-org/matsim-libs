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

package org.matsim.vsp.ev;

import javax.inject.Inject;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.vsp.ev.EvConfigGroup.AuxDischargingSimulation;
import org.matsim.vsp.ev.charging.ChargingHandler;
import org.matsim.vsp.ev.charging.ChargingLogic;
import org.matsim.vsp.ev.charging.ChargingWithQueueingLogic;
import org.matsim.vsp.ev.charging.FixedSpeedChargingStrategy;
import org.matsim.vsp.ev.data.ChargingInfrastructure;
import org.matsim.vsp.ev.data.ElectricFleet;
import org.matsim.vsp.ev.data.file.ChargingInfrastructureProvider;
import org.matsim.vsp.ev.data.file.ElectricFleetProvider;
import org.matsim.vsp.ev.discharging.AuxDischargingHandler;
import org.matsim.vsp.ev.discharging.AuxEnergyConsumption;
import org.matsim.vsp.ev.discharging.DriveDischargingHandler;
import org.matsim.vsp.ev.discharging.DriveEnergyConsumption;
import org.matsim.vsp.ev.discharging.OhdeSlaskiDriveEnergyConsumption;
import org.matsim.vsp.ev.stats.ChargerOccupancyTimeProfileCollectorProvider;
import org.matsim.vsp.ev.stats.ChargerOccupancyXYDataProvider;
import org.matsim.vsp.ev.stats.IndividualSocTimeProfileCollectorProvider;
import org.matsim.vsp.ev.stats.SocHistogramTimeProfileCollectorProvider;

import com.google.inject.name.Names;

public class EvModule extends AbstractModule {
	private static ChargingLogic.Factory DEFAULT_CHARGING_LOGIC_FACTORY = charger -> new ChargingWithQueueingLogic(
			charger, new FixedSpeedChargingStrategy(charger.getPower()));

	// Nissan Leaf
	private static DriveEnergyConsumption.Factory DEFAULT_DRIVE_CONSUMPTION_FACTORY = electricVehicle -> new OhdeSlaskiDriveEnergyConsumption();

	// no AUX consumption (TODO consider adding AUX to Drive for non-DVRP use cases...)
	private static AuxEnergyConsumption.Factory DEFAULT_AUX_CONSUMPTION_FACTORY = electricVehicle -> (period -> 0);

	@Override
	public void install() {
		EvConfigGroup evCfg = EvConfigGroup.get(getConfig());

		bind(ElectricFleet.class)
				.toProvider(new ElectricFleetProvider(evCfg.getVehiclesFileUrl(getConfig().getContext())))
				.asEagerSingleton();
		bind(DriveEnergyConsumption.Factory.class).toInstance(DEFAULT_DRIVE_CONSUMPTION_FACTORY);
		bind(AuxEnergyConsumption.Factory.class).toInstance(DEFAULT_AUX_CONSUMPTION_FACTORY);

		bind(Network.class).annotatedWith(Names.named(ChargingInfrastructure.CHARGERS)).to(Network.class)
				.asEagerSingleton();
		bind(ChargingInfrastructure.class)
				.toProvider(new ChargingInfrastructureProvider(evCfg.getChargersFileUrl(getConfig().getContext())))
				.asEagerSingleton();
		bind(ChargingLogic.Factory.class).toInstance(DEFAULT_CHARGING_LOGIC_FACTORY);

		bind(DriveDischargingHandler.class).asEagerSingleton();
		addEventHandlerBinding().to(DriveDischargingHandler.class);

		if (evCfg.getAuxDischargingSimulation() == AuxDischargingSimulation.SeperateAuxDischargingHandler) {
			bind(AuxDischargingHandler.class).asEagerSingleton();
			addMobsimListenerBinding().to(AuxDischargingHandler.class);
		}

		bind(ChargingHandler.class).asEagerSingleton();
		addMobsimListenerBinding().to(ChargingHandler.class);

		if (EvConfigGroup.get(getConfig()).getTimeProfiles()) {
			addMobsimListenerBinding().toProvider(SocHistogramTimeProfileCollectorProvider.class);
			addMobsimListenerBinding().toProvider(IndividualSocTimeProfileCollectorProvider.class);
			addMobsimListenerBinding().toProvider(ChargerOccupancyTimeProfileCollectorProvider.class);
			addMobsimListenerBinding().toProvider(ChargerOccupancyXYDataProvider.class);
			// add more time profiles if necessary
		}

		bind(InitAtIterationStart.class).asEagerSingleton();
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
