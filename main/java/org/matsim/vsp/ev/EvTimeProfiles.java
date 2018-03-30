/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.matsim.contrib.util.histogram.UniformHistogram;
import org.matsim.contrib.util.timeprofile.TimeProfiles;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.vsp.ev.data.ElectricVehicle;
import org.matsim.vsp.ev.data.EvData;

public class EvTimeProfiles {
	public static ProfileCalculator createSocHistogramCalculator(final EvData evData) {
		String[] header = { "0", "0.1+", "0.2+", "0.3+", "0.4+", "0.5+", "0.6+", "0.7+", "0.8+", "0.9+" };
		return TimeProfiles.createProfileCalculator(header, () -> {
			UniformHistogram histogram = new UniformHistogram(0.1, header.length);
			for (ElectricVehicle ev : evData.getElectricVehicles().values()) {
				histogram.addValue(ev.getBattery().getSoc() / ev.getBattery().getCapacity());
			}

			Long[] values = new Long[header.length];
			for (int b = 0; b < header.length; b++) {
				values[b] = histogram.getCount(b);
			}
			return values;
		});
	}

	public static ProfileCalculator createMeanSocCalculator(final EvData evData) {
		return TimeProfiles.createSingleValueCalculator("meanSOC", () -> {
			Mean mean = new Mean();
			for (ElectricVehicle ev : evData.getElectricVehicles().values()) {
				mean.increment(ev.getBattery().getSoc());
			}
			return mean.getResult() / EvUnitConversions.J_PER_kWh;// in [kWh]
		});
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
