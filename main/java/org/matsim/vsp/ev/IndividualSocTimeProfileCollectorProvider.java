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

import java.util.List;
import java.util.stream.Collectors;

import org.matsim.contrib.util.timeprofile.TimeProfileCollector;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.util.timeprofile.TimeProfiles;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.vsp.ev.data.ElectricVehicle;
import org.matsim.vsp.ev.data.EvData;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class IndividualSocTimeProfileCollectorProvider implements Provider<MobsimListener> {
	private final EvData evData;
	private final MatsimServices matsimServices;

	@Inject
	public IndividualSocTimeProfileCollectorProvider(EvData evData, MatsimServices matsimServices) {
		this.evData = evData;
		this.matsimServices = matsimServices;
	}

	@Override
	public MobsimListener get() {
		ProfileCalculator calc = createIndividualSocCalculator(evData);
		return new TimeProfileCollector(calc, 300, "individual_soc_time_profiles", matsimServices);
	}

	private static final int MAX_VEHICLE_COLUMNS = 50;

	public static ProfileCalculator createIndividualSocCalculator(final EvData evData) {
		int columns = Math.min(evData.getElectricVehicles().size(), MAX_VEHICLE_COLUMNS);
		List<ElectricVehicle> selectedEvs = evData.getElectricVehicles().values().stream().limit(columns)
				.collect(Collectors.toList());
		String[] header = selectedEvs.stream().map(ev -> ev.getId() + "").toArray(String[]::new);

		return TimeProfiles.createProfileCalculator(header, () -> {
			return selectedEvs.stream()//
					.map(ev -> ev.getBattery().getSoc() / EvUnitConversions.J_PER_kWh)// in [kWh]
					.toArray(Double[]::new);
		});
	}

}
