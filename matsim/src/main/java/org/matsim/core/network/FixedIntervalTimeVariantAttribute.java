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

package org.matsim.core.network;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.trafficmonitoring.TimeBinUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import com.google.common.base.Preconditions;

/**
 * This class follows the rules assumed in {@link TravelTimeCalculator}: The constructor arguments
 * timeSlice and maxTime have the same meaning as there, and the last time bin is open ended.
 */
final class FixedIntervalTimeVariantAttribute implements TimeVariantAttribute {
	private final int timeSlice;
	private final int numSlots;

	private double baseValue;
	private double[] values;

	private int eventsCount = 0;
	private int eventsCountWhenLastRecalc = -1;

	public FixedIntervalTimeVariantAttribute(int timeSlice, int maxTime) {
		this.timeSlice = timeSlice;
		this.numSlots = TimeBinUtils.getTimeBinCount(maxTime, timeSlice);
	}

	@Override
	public boolean isRecalcRequired() {
		return eventsCountWhenLastRecalc != eventsCount;
	}

	//TODO before calling this method we could convert changeEvents into a sequence of non-null changeValues
	@Override
	public void recalc(NavigableMap<Double, List<NetworkChangeEvent>> changeEvents, ChangeValueGetter valueGetter,
			double baseValue1) {
		this.baseValue = baseValue1;

		if (eventsCount == 0) {
			return;
		}

		//To save memory, the array is constructed only if there is at least one ChangeEvent.
		//This saves a lot of memory in cases when only one attribute is time variant, while
		//the remaining two are invariant.
		double[] newValues = values == null ? new double[numSlots] : values;

		int numEvent = 0;
		int fromBin = 0;//inclusive
		double currentValue = baseValue1;
		if (changeEvents != null) {
			for (Map.Entry<Double, List<NetworkChangeEvent>> entry : changeEvents.entrySet()) {
				// Several events may share a start time. They all apply, in registration order, but they fill the
				// bins once, so the attribute takes a single value from that time onwards.
				double valueHere = currentValue;
				boolean changedHere = false;
				for (NetworkChangeEvent event : entry.getValue()) {
					ChangeValue value = valueGetter.getChangeValue(event);
					if (value != null) {
						Preconditions.checkArgument(event.getStartTime() >= 0,
								"The current implementation supports only non-negative change event times");
						valueHere = apply(valueHere, value);
						changedHere = true;
					}
				}

				if (changedHere) {
					numEvent++;
					int toBin = (int)(entry.getKey() / timeSlice);//exclusive
					Arrays.fill(newValues, fromBin, toBin, currentValue);
					currentValue = valueHere;
					fromBin = toBin;
				}
			}
		}

		// Validate before publishing: leaving eventsCountWhenLastRecalc updated after a failure would make
		// isRecalcRequired() report false and the half-filled array would then be read as if it were complete.
		if (numEvent != this.eventsCount) {
			throw new RuntimeException("Expected number of change events ("
					+ (this.eventsCount)
					+ ") differs from the number of events found ("
					+ numEvent
					+ ")!");
		}

		Arrays.fill(newValues, fromBin, newValues.length, currentValue);
		this.values = newValues;
		eventsCountWhenLastRecalc = eventsCount;
	}

	private static double apply(final double currentValue, final ChangeValue value) {
		return switch (value.getType()) {
			case ABSOLUTE_IN_SI_UNITS -> value.getValue();
			case FACTOR -> currentValue * value.getValue();
			case OFFSET_IN_SI_UNITS -> currentValue + value.getValue();
		};
	}

	@Override
	public double getValue(final double time) {
		Preconditions.checkArgument(!Double.isNaN(time), "NaN time is not supported");
		if (eventsCount == 0) {
			return baseValue;
		}

		int bin = TimeBinUtils.getTimeBinIndex(time, timeSlice, numSlots);
		return bin < 0 ? baseValue : values[bin];
	}

	@Override
	public void incChangeEvents() {
		eventsCount++;
	}

	@Override
	public void clearEvents() {
		eventsCount = 0;
		eventsCountWhenLastRecalc = -1;
		values = null;
	}
}
