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

package playground.michalm.ev;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.matsim.contrib.taxi.util.stats.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.taxi.util.stats.TimeProfiles;
import org.matsim.contrib.util.histogram.UniformHistogram;

import com.google.common.collect.Iterables;

import playground.michalm.ev.data.*;

public class EvTimeProfiles {
	public static ProfileCalculator createSocHistogramCalculator(final EvData evData) {
		String[] header = { "0", "0.1+", "0.2+", "0.3+", "0.4+", "0.5+", "0.6+", "0.7+", "0.8+", "0.9+" };
		return new TimeProfiles.MultiValueProfileCalculator(header) {
			@Override
			public Long[] calcValues() {
				UniformHistogram histogram = new UniformHistogram(0.1, header.length);
				for (ElectricVehicle ev : evData.getElectricVehicles().values()) {
					histogram.addValue(ev.getBattery().getSoc() / ev.getBattery().getCapacity());
				}

				Long[] values = new Long[header.length];
				for (int b = 0; b < header.length; b++) {
					values[b] = histogram.getCount(b);
				}
				return values;
			}
		};
	}

	public static ProfileCalculator createMeanSocCalculator(final EvData evData) {
		return new TimeProfiles.SingleValueProfileCalculator("meanSOC") {
			@Override
			public Double calcValue() {
				Mean mean = new Mean();
				for (ElectricVehicle ev : evData.getElectricVehicles().values()) {
					mean.increment(ev.getBattery().getSoc());
				}
				return mean.getResult() / EvUnitConversions.J_PER_kWh;// in [kWh]
			}
		};
	}

	private static final int MAX_VEHICLE_COLUMNS = 50;

	public static ProfileCalculator createIndividualSocCalculator(final EvData evData) {
		int columns = Math.min(evData.getElectricVehicles().size(), MAX_VEHICLE_COLUMNS);
		Iterable<ElectricVehicle> selectedEvs = Iterables.limit(evData.getElectricVehicles().values(), columns);

		String[] header = new String[columns];
		int col = 0;
		for (ElectricVehicle ev : selectedEvs) {
			header[col++] = ev.getId() + "";
		}

		return new TimeProfiles.MultiValueProfileCalculator(header) {
			@Override
			public Double[] calcValues() {
				Double[] vals = new Double[columns];
				int col = 0;
				for (ElectricVehicle ev : selectedEvs) {
					vals[col++] = ev.getBattery().getSoc() / EvUnitConversions.J_PER_kWh;// in [kWh]
				}
				return vals;
			}
		};
	}
}
