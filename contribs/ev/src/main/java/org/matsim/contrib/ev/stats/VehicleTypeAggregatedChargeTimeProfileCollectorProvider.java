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

package org.matsim.contrib.ev.stats;

import static java.util.stream.Collectors.*;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.contrib.common.timeprofile.TimeProfileCollector;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.common.timeprofile.TimeProfiles;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class VehicleTypeAggregatedChargeTimeProfileCollectorProvider implements Provider<MobsimListener> {
	private final ElectricFleet evFleet;
	private final MatsimServices matsimServices;

	@Inject
	public VehicleTypeAggregatedChargeTimeProfileCollectorProvider(ElectricFleet evFleet, MatsimServices matsimServices) {
		this.evFleet = evFleet;
		this.matsimServices = matsimServices;
	}

	@Override
	public MobsimListener get() {
		ProfileCalculator calc = createIndividualChargeCalculator(evFleet);
		return new TimeProfileCollector(calc, 300, "average_charge_time_profiles", matsimServices);
	}

	private static final String ALL_VEHICLES_ID = "all vehicles";

	public static ProfileCalculator createIndividualChargeCalculator(final ElectricFleet evFleet) {

		Set<String> vehicleTypes = evFleet.getElectricVehicles()
				.values()
				.stream()
				.map(ev -> ev.getVehicleSpecification().getMatsimVehicle().getType().getId() + "")
				.collect(Collectors.toCollection(LinkedHashSet::new));
		vehicleTypes.add(ALL_VEHICLES_ID);
		ImmutableList<String> header = ImmutableList.copyOf(vehicleTypes);
		return TimeProfiles.createProfileCalculator(header, () -> {
			Map<String, Double> averageSocByType = evFleet.getElectricVehicles()
					.values()
					.stream()
					.collect(groupingBy(ev -> ev.getVehicleSpecification().getMatsimVehicle().getType().getId() + "",
							mapping(ev -> EvUnits.J_to_kWh(ev.getBattery().getCharge()), averagingDouble(c -> c))));
			double averageSoc = evFleet.getElectricVehicles()
					.values()
					.stream()
					.mapToDouble(ev -> EvUnits.J_to_kWh(ev.getBattery().getCharge()))
					.average()
					.getAsDouble();
			averageSocByType.put(ALL_VEHICLES_ID, averageSoc);
			return ImmutableMap.copyOf(averageSocByType);
		});
	}

}
