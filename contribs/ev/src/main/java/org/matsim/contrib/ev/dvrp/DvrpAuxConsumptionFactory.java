/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.util.function.BiPredicate;
import java.util.function.DoubleSupplier;

import javax.inject.Inject;

import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.OhdeSlaskiAuxEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;

import com.google.inject.Injector;

public class DvrpAuxConsumptionFactory implements AuxEnergyConsumption.Factory {
	@Inject
	private Injector injector;

	private final String mode;
	private final DoubleSupplier temperatureProvider;
	//FIXME change to BiPredicate<DvrpVehicle, Double> or even Predicate<DvrpVehicle>
	private final BiPredicate<DvrpVehicleSpecification, Double> turnedOnPredicate;

	public DvrpAuxConsumptionFactory(String mode, DoubleSupplier temperatureProvider,
			BiPredicate<DvrpVehicleSpecification, Double> turnedOnPredicate) {
		this.mode = mode;
		this.temperatureProvider = temperatureProvider;
		this.turnedOnPredicate = turnedOnPredicate == null ? (v, t) -> true : turnedOnPredicate;
	}

	@Override
	public AuxEnergyConsumption create(ElectricVehicleSpecification electricVehicleSpecification) {
		FleetSpecification fleet = injector.getInstance(DvrpModes.key(FleetSpecification.class, mode));
		DvrpVehicleSpecification vehicle = fleet.getVehicleSpecifications().get(electricVehicleSpecification.getId());

		return new OhdeSlaskiAuxEnergyConsumption(electricVehicleSpecification, temperatureProvider,
				(ev, t) -> turnedOnPredicate.test(vehicle, t));
	}
}
