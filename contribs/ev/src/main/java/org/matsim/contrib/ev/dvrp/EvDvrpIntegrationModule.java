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
import org.matsim.contrib.dvrp.data.DvrpVehicle;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup.AuxDischargingSimulation;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.data.Charger;
import org.matsim.contrib.ev.data.ChargingInfrastructure;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author michalm
 */
public class EvDvrpIntegrationModule extends AbstractModule {
    private Function<Charger, ChargingStrategy> chargingStrategyFactory;
    private DoubleSupplier temperatureProvider;
	private Predicate<DvrpVehicle> turnedOnPredicate;

    private final String mode;
    private String vehicleFile;

    public EvDvrpIntegrationModule(String mode) {
        this.mode = mode;
    }

    @Override
    public void install() {
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

        bind(Network.class).annotatedWith(Names.named(ChargingInfrastructure.CHARGERS))//
                .to(Key.get(Network.class, Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING))).asEagerSingleton();
        bind(ChargingLogic.Factory.class).toInstance(
                charger -> new ChargingWithQueueingAndAssignmentLogic(charger, chargingStrategyFactory.apply(charger)));
        bind(AuxEnergyConsumption.Factory.class).toInstance(
                new DvrpAuxConsumptionFactory(mode, temperatureProvider, turnedOnPredicate));
        bind(DvrpModes.key(Fleet.class, mode)).toProvider(new EvDvrpFleetProvider(vehicleFile)).asEagerSingleton();
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

    public EvDvrpIntegrationModule setVehicleFile(String vehicleFile) {
        this.vehicleFile = vehicleFile;
        return this;
    }
}
