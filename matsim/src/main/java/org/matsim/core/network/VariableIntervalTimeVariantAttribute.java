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

import com.google.common.base.Preconditions;

final class VariableIntervalTimeVariantAttribute
implements TimeVariantAttribute
{
	private int aEvents = 1;
	private double[] aValues;
	private double[] aTimes;


	@Override
	public boolean isRecalcRequired()
	{
		return (this.aTimes == null) || (this.aTimes.length != this.aEvents);
		// The first condition just says if there is no material, we don't need to do anything.
		// yyyy The second condition is a bit weird.  It essentially checks if the cached data structure (aTimes) has
		// as many entries as it should have (given by aEvents).  This does need, however, an honest calling of
		// incChangeEvents().  Why not just compare aTimes.length to changeEvents.length?
		// Counterargument might be that we may not have changeEvents.length available when we call isRecalcRequired().
		// I do think, however, that this does not happen and also cannot happen, because we need the info anyway for recalculation.
		// If going into this direction, then incChangeEvents(), isRecalcRequired(), recalc(...) might not be necessary
		// as exposed interface methods any more; in contrast, getValue would need more arguments.
		// kai, jul'17
	}


	@Override
	public void recalc(NavigableMap<Double, List<NetworkChangeEvent>> changeEvents,
			ChangeValueGetter valueGetter, double baseValue)
	{
		// Built locally and only published once validated. getValue() binary-searches aTimes and indexes aValues with
		// the result, so a partially filled pair of arrays would be read as if it were complete.
		double[] times = new double[this.aEvents];
		double[] values = new double[this.aEvents];
		times[0] = Double.NEGATIVE_INFINITY;
		values[0] = baseValue;

		int numEvent = 0;
		if (changeEvents != null) {
			// go through all change events in chronological sequence:
			for (Map.Entry<Double, List<NetworkChangeEvent>> entry : changeEvents.entrySet()) {
				// Several events may share a start time. They all apply, in registration order, but they collapse into
				// a single (time, value) pair: aTimes must stay strictly increasing for the binary search in getValue.
				double currentValue = values[numEvent];
				boolean changedHere = false;
				for (NetworkChangeEvent event : entry.getValue()) {
					ChangeValue value = valueGetter.getChangeValue(event);
					if (value == null) {
						continue;
					}
					currentValue = apply(currentValue, value);
					changedHere = true;
				}
				if (changedHere) {
					values[++numEvent] = currentValue;
					times[numEvent] = entry.getKey();
				}
			}
		}

		if (numEvent != this.aEvents - 1) {
			throw new RuntimeException("Expected number of change events (" + (this.aEvents - 1)
					+ ") differs from the number of events found (" + numEvent + ")!");
		}

		this.aTimes = times;
		this.aValues = values;
	}

	private static double apply(final double currentValue, final ChangeValue value)
	{
		return switch (value.getType()) {
			// here, we just need to replace the value:
			case ABSOLUTE_IN_SI_UNITS -> value.getValue();
			// there, the change event multiplies what we have so far:
			case FACTOR -> currentValue * value.getValue();
			case OFFSET_IN_SI_UNITS -> currentValue + value.getValue();
		};
	}


	@Override
	public double getValue(final double time)
	{
		Preconditions.checkArgument(!Double.isNaN(time), "NaN time is not supported");
		// after we have put everything into an array by recalc, we just need a binary search:
		int key = Arrays.binarySearch(this.aTimes, time);
		key = key >= 0 ? key : -key - 2;
		return this.aValues[key];
	}


	@Override
	public void incChangeEvents()
	{
		aEvents++;
	}


	@Override
	public void clearEvents()
	{
		aTimes = null;
		aValues = null;
		aEvents = 1;
	}
}
