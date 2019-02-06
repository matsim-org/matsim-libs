/* *********************************************************************** *
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
 * *********************************************************************** */

package org.matsim.contrib.ev.dvrp;

import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup.AuxDischargingSimulation;
import org.matsim.contrib.ev.charging.ChargingModule;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.data.Charger;
import org.matsim.contrib.ev.data.ElectricFleetModule;
import org.matsim.contrib.ev.discharging.DischargingModule;
import org.matsim.contrib.ev.discharging.OhdeSlaskiDriveEnergyConsumption;
import org.matsim.contrib.ev.stats.EvStatsModule;

import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * Use this module instead of the default EvModule
 *
 * @author michalm
 */
public class EvDvrpIntegrationModule extends AbstractDvrpModeModule {
	private Function<Charger, ChargingStrategy> chargingStrategyFactory;
	private DoubleSupplier temperatureProvider;
	private Predicate<DvrpVehicle> turnedOnPredicate;

	private String vehicleFile;

	public EvDvrpIntegrationModule(String mode) {
		super(mode);
	}

	@Override
	public void install() {
		EvConfigGroup evCfg = EvConfigGroup.get(getConfig());

		if (EvConfigGroup.get(getConfig()).getAuxDischargingSimulation()
				== AuxDischargingSimulation.insideDriveDischargingHandler) {
			if (turnedOnPredicate != null) {
				throw new RuntimeException("turnedOnPredicate must not be set"
						+ " if auxDischargingSimulation == 'insideDriveDischargingHandler'");
			}
		} else {
			if (turnedOnPredicate == null) {
				throw new RuntimeException("turnedOnPredicate must be set"
						+ " if auxDischargingSimulation != 'insideDriveDischargingHandler'");
			}
		}

		install(new ElectricFleetModule(evCfg));

		install(new ChargingModule(evCfg, Key.get(Network.class, Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING)),
				charger -> new ChargingWithQueueingAndAssignmentLogic(charger,
						chargingStrategyFactory.apply(charger))));

		install(new DischargingModule(evCfg, ev -> new OhdeSlaskiDriveEnergyConsumption(),
				new DvrpAuxConsumptionFactory(getMode(), temperatureProvider, turnedOnPredicate)));

		install(new EvStatsModule(evCfg));
	}

	public EvDvrpIntegrationModule setChargingStrategyFactory(
			Function<Charger, ChargingStrategy> chargingStrategyFactory) {
		this.chargingStrategyFactory = chargingStrategyFactory;
		return this;
	}

	public EvDvrpIntegrationModule setTemperatureProvider(DoubleSupplier temperatureProvider) {
		this.temperatureProvider = temperatureProvider;
		return this;
	}

	public EvDvrpIntegrationModule setTurnedOnPredicate(Predicate<DvrpVehicle> turnedOnPredicate) {
		this.turnedOnPredicate = turnedOnPredicate;
		return this;
	}
}
