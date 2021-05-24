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
	public void recalc(TreeMap<Double, NetworkChangeEvent> changeEvents,
			ChangeValueGetter valueGetter, double baseValue)
	{
		this.aTimes = new double[this.aEvents];
		this.aValues = new double[this.aEvents];
		this.aTimes[0] = Double.NEGATIVE_INFINITY;
		this.aValues[0] = baseValue;

		int numEvent = 0;
		if (changeEvents != null) {
			// go through all change events in chronological sequence:
			for (NetworkChangeEvent event : changeEvents.values()) {
				ChangeValue value = valueGetter.getChangeValue(event);
				if (value != null) {
					switch( value.getType() ) {
					case ABSOLUTE_IN_SI_UNITS:
						// here, we just need to replace the value:
						this.aValues[++numEvent] = value.getValue();
						this.aTimes[numEvent] = event.getStartTime();
						break;
					case FACTOR: {
						// there, the change event multiplies what we have so far:
						double currentValue = this.aValues[numEvent];
						this.aValues[++numEvent] = currentValue * value.getValue();
						this.aTimes[numEvent] = event.getStartTime();
						break; }
					case OFFSET_IN_SI_UNITS: {
						double currentValue = this.aValues[numEvent];
						this.aValues[++numEvent] = currentValue + value.getValue();
						this.aTimes[numEvent] = event.getStartTime();
						break; }
					default:
						throw new RuntimeException( "unknown ChangeType" ) ;
					}
				}
			}
		}

		if (numEvent != this.aEvents - 1) {
			throw new RuntimeException("Expected number of change events (" + (this.aEvents - 1)
					+ ") differs from the number of events found (" + numEvent + ")!");
		}
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
