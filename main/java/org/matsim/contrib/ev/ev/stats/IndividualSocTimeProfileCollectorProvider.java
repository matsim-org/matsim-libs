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

package org.matsim.contrib.ev.ev.stats;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.contrib.ev.ev.EvUnits;
import org.matsim.contrib.ev.ev.data.ElectricFleet;
import org.matsim.contrib.ev.ev.data.ElectricVehicle;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.util.timeprofile.TimeProfiles;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IndividualSocTimeProfileCollectorProvider implements Provider<MobsimListener> {
	private final ElectricFleet evFleet;
	private final MatsimServices matsimServices;

	@Inject
	public IndividualSocTimeProfileCollectorProvider(ElectricFleet evFleet, MatsimServices matsimServices) {
		this.evFleet = evFleet;
		this.matsimServices = matsimServices;
	}

	@Override
	public MobsimListener get() {
		ProfileCalculator calc = createIndividualSocCalculator(evFleet);
		return new TimeProfileCollector(calc, 300, "individual_soc_time_profiles", matsimServices);
	}

	private static final int MAX_VEHICLE_COLUMNS = 50;

	public static ProfileCalculator createIndividualSocCalculator(final ElectricFleet evFleet) {
		int columns = Math.min(evFleet.getElectricVehicles().size(), MAX_VEHICLE_COLUMNS);
		List<ElectricVehicle> allEvs = new ArrayList<>();
		allEvs.addAll(evFleet.getElectricVehicles().values());
		Collections.shuffle(allEvs);
		List<ElectricVehicle> selectedEvs = allEvs.stream().limit(columns).collect(Collectors.toList());

		String[] header = selectedEvs.stream().map(ev -> ev.getId() + "").toArray(String[]::new);

		return TimeProfiles.createProfileCalculator(header, () -> {
			return selectedEvs.stream()//
					.map(ev -> EvUnits.J_to_kWh(ev.getBattery().getSoc()))// in [kWh]
					.toArray(Double[]::new);
		});
	}

}
