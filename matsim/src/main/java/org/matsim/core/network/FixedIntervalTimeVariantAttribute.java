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
import java.util.TreeMap;

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
	public void recalc(TreeMap<Double, NetworkChangeEvent> changeEvents, ChangeValueGetter valueGetter,
			double baseValue1) {
		this.baseValue = baseValue1;

		if (eventsCount == 0) {
			return;
		}

		//To save memory, the array is constructed only if there is at least one ChangeEvent.
		//This saves a lot of memory in cases when only one attribute is time variant, while
		//the remaining two are invariant.
		if (values == null) {
			values = new double[numSlots];
		}

		int numEvent = 0;
		int fromBin = 0;//inclusive
		double currentValue = baseValue1;
		if (changeEvents != null) {
			for (NetworkChangeEvent event : changeEvents.values()) {
				ChangeValue value = valueGetter.getChangeValue(event);
				if (value != null) {
					numEvent++;

					Preconditions.checkArgument(event.getStartTime() >= 0,
							"The current implementation supports only non-negative change event times");
					int toBin = (int)(event.getStartTime() / timeSlice);//exclusive
					Arrays.fill(values, fromBin, toBin, currentValue);

					switch (value.getType()) {
						case ABSOLUTE_IN_SI_UNITS:
							currentValue = value.getValue();
							break;
						case FACTOR:
							currentValue *= value.getValue();
							break;
						case OFFSET_IN_SI_UNITS:
							currentValue += value.getValue();
							break;
						default:
							throw new RuntimeException("unknown ChangeType");
					}
					fromBin = toBin;
				}
			}
		}
		Arrays.fill(values, fromBin, values.length, currentValue);
		eventsCountWhenLastRecalc = eventsCount;

		if (numEvent != this.eventsCount) {
			throw new RuntimeException("Expected number of change events ("
					+ (this.eventsCount)
					+ ") differs from the number of events found ("
					+ numEvent
					+ ")!");
		}
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
