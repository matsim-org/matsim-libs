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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.contrib.common.timeprofile.TimeProfileCollector.ProfileCalculator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TimeProfiles {
	public static ProfileCalculator combineProfileCalculators(final ProfileCalculator... calculators) {
		return createProfileCalculator(Arrays.stream(calculators)
						.flatMap(c -> c.getHeader().stream())
						.collect(ImmutableList.toImmutableList()),//
				() -> Arrays.stream(calculators)
						.flatMap(c -> c.calcValues().entrySet().stream())
						.collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
	}

	public static ProfileCalculator createSingleValueCalculator(String header, DoubleSupplier valueSupplier) {
		return createProfileCalculator(ImmutableList.of(header),
				() -> ImmutableMap.of(header, valueSupplier.getAsDouble()));
	}

	public static ProfileCalculator createProfileCalculator(ImmutableList<String> header,
			Supplier<ImmutableMap<String, Double>> valueSupplier) {
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

	public static <T> ImmutableList<T> createExtendedHeader(ImmutableList<T> defaultColumns, Stream<T> columns,
			Comparator<T> comparator) {
		Set<T> defaultHeader = new HashSet<>(defaultColumns);
		List<T> additionalColumns = columns.filter(Predicate.not(defaultHeader::contains))
				.distinct()
				.sorted(comparator)
				.collect(Collectors.toList());
		return ImmutableList.<T>builder().addAll(defaultColumns).addAll(additionalColumns).build();
	}
}
