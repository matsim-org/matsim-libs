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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.common.timeprofile.TimeProfiles;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

public class IndividualChargeTimeProfileCollectorProvider implements Provider<MobsimListener> {
	private final ElectricFleet evFleet;
	private final MatsimServices matsimServices;
	private final int maxVehicleColumns;

	@Inject
	public IndividualChargeTimeProfileCollectorProvider(ElectricFleet evFleet, MatsimServices matsimServices, Config config) {
		this.evFleet = evFleet;
		this.matsimServices = matsimServices;
		maxVehicleColumns = ConfigUtils.addOrGetModule(config, EvConfigGroup.class).numberOfIndividualTimeProfiles;
	}

	public static ProfileCalculator createIndividualChargeCalculator(final ElectricFleet evFleet, int maxVehicleColumns) {

		int columns = Math.min(evFleet.getElectricVehicles().size(), maxVehicleColumns);
		List<ElectricVehicle> allEvs = new ArrayList<>(evFleet.getElectricVehicles().values());
		List<ElectricVehicle> selectedEvs = allEvs.stream().limit(columns).toList();

		ImmutableList<String> header = selectedEvs.stream().map(ev -> ev.getId() + "").collect(toImmutableList());

		return TimeProfiles.createProfileCalculator(header, () -> selectedEvs.stream()
				.collect(toImmutableMap(ev -> ev.getId() + "",
						ev -> EvUnits.J_to_kWh(ev.getBattery().getCharge()))));/*in [kWh]*/
	}

	@Override
	public MobsimListener get() {
		ProfileCalculator calc = createIndividualChargeCalculator(evFleet, maxVehicleColumns);
		return new TimeProfileCollector(calc, 300, "individual_charge_time_profiles", matsimServices);
	}
}
