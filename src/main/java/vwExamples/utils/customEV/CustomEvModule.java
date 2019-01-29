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

package vwExamples.utils.customEV;

import com.google.inject.name.Names;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup.AuxDischargingSimulation;
import org.matsim.contrib.ev.charging.ChargingHandler;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingWithQueueingLogic;
import org.matsim.contrib.ev.charging.FixedSpeedChargingStrategy;
import org.matsim.contrib.ev.data.ChargingInfrastructure;
import org.matsim.contrib.ev.data.ElectricFleet;
import org.matsim.contrib.ev.data.file.ChargingInfrastructureProvider;
import org.matsim.contrib.ev.data.file.ElectricFleetProvider;
import org.matsim.contrib.ev.discharging.*;
import org.matsim.contrib.ev.stats.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

import javax.inject.Inject;

public class CustomEvModule extends AbstractModule {
		
	private static ChargingLogic.Factory DEFAULT_CHARGING_LOGIC_FACTORY = charger -> new ChargingWithQueueingLogic(
			charger, new FixedSpeedChargingStrategy(charger.getPower()));

	private static DriveEnergyConsumption.Factory DEFAULT_DRIVE_CONSUMPTION_FACTORY //
			= ev -> new OhdeSlaskiDriveEnergyConsumption();

	// TODO fixed temperature 15 oC
	// FIXME reintroduce TemperatureProvider
	private static AuxEnergyConsumption.Factory DEFAULT_AUX_CONSUMPTION_FACTORY //
			= ev -> new OhdeSlaskiAuxEnergyConsumption(ev, () -> 15, v -> true);

	@Override
	public void install() {
		EvConfigGroup evCfg = EvConfigGroup.get(getConfig());
		DrtConfigGroup drtCfg = DrtConfigGroup.get(getConfig());

		bind(ElectricFleet.class)
				.toProvider(new ElectricFleetProvider(evCfg.getVehiclesFileUrl(getConfig().getContext())))
				.asEagerSingleton();
		
//			//Added by saxer
//			bind(Fleet.class)
//			.toProvider(new FleetProvider(drtCfg.getVehiclesFile()))
//			.asEagerSingleton();
//			//Added by saxer
			
			
//			
		
		bind(DriveEnergyConsumption.Factory.class).toInstance(DEFAULT_DRIVE_CONSUMPTION_FACTORY);

        if (evCfg.getAuxDischargingSimulation() == AuxDischargingSimulation.seperateAuxDischargingHandler) {
			// "isTurnedOn" returns true ==> should not be used when for "seperateAuxDischargingHandler"
			bind(AuxEnergyConsumption.Factory.class).toInstance(DEFAULT_AUX_CONSUMPTION_FACTORY);
		}

		bind(Network.class).annotatedWith(Names.named(ChargingInfrastructure.CHARGERS)).to(Network.class)
				.asEagerSingleton();
		bind(ChargingInfrastructure.class)
				.toProvider(new ChargingInfrastructureProvider(evCfg.getChargersFileUrl(getConfig().getContext())))
				.asEagerSingleton();
		bind(ChargingLogic.Factory.class).toInstance(DEFAULT_CHARGING_LOGIC_FACTORY);

		bind(DriveDischargingHandler.class).asEagerSingleton();
		addEventHandlerBinding().to(DriveDischargingHandler.class);

		if (evCfg.getAuxDischargingSimulation() == AuxDischargingSimulation.seperateAuxDischargingHandler) {
			bind(CustomAuxDischargingHandler.class).asEagerSingleton();
			addMobsimListenerBinding().to(CustomAuxDischargingHandler.class);
			addEventHandlerBinding().to(CustomAuxDischargingHandler.class);
		}

		bind(ChargingHandler.class).asEagerSingleton();
		addMobsimListenerBinding().to(ChargingHandler.class);

		if (EvConfigGroup.get(getConfig()).getTimeProfiles()) {
			addMobsimListenerBinding().toProvider(SocHistogramTimeProfileCollectorProvider.class);
			addMobsimListenerBinding().toProvider(IndividualSocTimeProfileCollectorProvider.class);
			addMobsimListenerBinding().toProvider(ChargerOccupancyTimeProfileCollectorProvider.class);
			addMobsimListenerBinding().toProvider(ChargerOccupancyXYDataProvider.class);
			addMobsimListenerBinding().toProvider(VehicleTypeAggregatedSocTimeProfileCollectorProvider.class);
			// add more time profiles if necessary
		}
        addControlerListenerBinding().to(EVControlerListener.class).asEagerSingleton();
        bind(ChargerPowerCollector.class).asEagerSingleton();

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
