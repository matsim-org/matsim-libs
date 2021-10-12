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
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.vehicles.Vehicles;

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
		// 3 options:
		// - vehicle specifications provided in a separate XML file (http://matsim.org/files/dtd/electric_vehicles_v1.dtd)
		// - vehicle specifications derived from the "standard" matsim vehicles (only if they are read from a file,
		//     i.e. VehiclesSource.fromVehiclesData)
		// - vehicle specifications provided via a custom binding for ElectricFleetSpecification
		if (evCfg.getVehiclesFile() != null) {
			bind(ElectricFleetSpecification.class).toProvider(() -> {
				ElectricFleetSpecification fleetSpecification = new ElectricFleetSpecificationImpl();
				new ElectricFleetReader(fleetSpecification).parse(
						ConfigGroup.getInputFileURL(getConfig().getContext(), evCfg.getVehiclesFile()));
				return fleetSpecification;
			}).asEagerSingleton();
		} else if (getConfig().qsim().getVehiclesSource() == QSimConfigGroup.VehiclesSource.fromVehiclesData) {
			bind(ElectricFleetSpecification.class).toProvider(new Provider<>() {
				@Inject
				private Vehicles vehicles;

				@Override
				public ElectricFleetSpecification get() {
					return ElectricVehicleSpecificationWithMatsimVehicle.createFleetSpecificationFromMatsimVehicles(
							vehicles);
				}
			}).asEagerSingleton();
		}

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ElectricFleet.class).toProvider(new Provider<>() {
					@Inject
					private ElectricFleetSpecification fleetSpecification;
					@Inject
					private DriveEnergyConsumption.Factory driveConsumptionFactory;
					@Inject
					private AuxEnergyConsumption.Factory auxConsumptionFactory;
					@Inject
					private ChargingPower.Factory chargingPowerFactory;

					@Override
					public ElectricFleet get() {
						return ElectricFleets.createDefaultFleet(fleetSpecification, driveConsumptionFactory,
								auxConsumptionFactory, chargingPowerFactory);
					}
				}).asEagerSingleton();
			}
		});
	}
}
