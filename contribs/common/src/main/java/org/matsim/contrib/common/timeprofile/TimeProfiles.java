/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.contrib.common.timeprofile;

import java.util.function.Supplier;

import org.matsim.contrib.common.timeprofile.TimeProfileCollector.ProfileCalculator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TimeProfiles {
	public static ProfileCalculator createProfileCalculator(ImmutableList<String> header, Supplier<ImmutableMap<String, Double>> valueSupplier) {
		return new ProfileCalculator() {
			@Override
			public ImmutableList<String> getHeader() {
				return header;
			}

			@Override
			public ImmutableMap<String, Double> calcValues() {
				return valueSupplier.get();
			}
		};
	}
}
