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

package org.matsim.contrib.util.timeprofile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.matsim.contrib.util.CSVLineBuilder;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector.ProfileCalculator;

public class TimeProfiles {
	public static ProfileCalculator combineProfileCalculators(final ProfileCalculator... calculators) {
		CSVLineBuilder builder = new CSVLineBuilder();
		for (ProfileCalculator pc : calculators) {
			builder.addAll(pc.getHeader());
		}

		return createProfileCalculator(builder.build(), () -> {
			List<Object> values = new ArrayList<>();
			for (ProfileCalculator pc : calculators) {
				values.addAll(Arrays.asList(pc.calcValues()));
			}
			return values.toArray();
		});
	}

	public static String[] combineValuesIntoStrings(Object... values) {
		String[] strings = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			strings[i] = values[i] + "";
		}
		return strings;
	}

	public static ProfileCalculator createSingleValueCalculator(String header, Supplier<Object> valueSupplier) {
		return createProfileCalculator(new String[] { header }, () -> new Object[] { valueSupplier.get() });
	}

	public static ProfileCalculator createProfileCalculator(String[] header, Supplier<Object[]> valueSupplier) {
		return new ProfileCalculator() {
			@Override
			public String[] getHeader() {
				return header;
			}

			@Override
			public Object[] calcValues() {
				return valueSupplier.get();
			}
		};
	}
}
