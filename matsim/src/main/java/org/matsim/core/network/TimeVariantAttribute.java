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

import java.util.List;
import java.util.NavigableMap;

import org.matsim.core.network.NetworkChangeEvent.ChangeValue;


public interface TimeVariantAttribute
{
	interface ChangeValueGetter {
		ChangeValue getChangeValue(NetworkChangeEvent event);
	}


	static ChangeValueGetter FREESPEED_GETTER = new ChangeValueGetter() {
		@Override public ChangeValue getChangeValue(NetworkChangeEvent event) {
			return event.getFreespeedChange();
		}
	};

	static ChangeValueGetter FLOW_CAPACITY_GETTER = new ChangeValueGetter() {
		@Override public ChangeValue getChangeValue(NetworkChangeEvent event) {
			return event.getFlowCapacityChange();
		}
	};

	static ChangeValueGetter LANES_GETTER = new ChangeValueGetter() {
		@Override public ChangeValue getChangeValue(NetworkChangeEvent event) {
			return event.getLanesChange();
		}
	};

	/**
	 * For base value, use {@link Double#NEGATIVE_INFINITY}. {@link Double#NaN} is not supported
	 * @param time
	 * @return
	 */
	double getValue(final double time);

	boolean isRecalcRequired();

	/**
	 * @param changeEvents all change events registered on the link, grouped by start time. Several events may share a
	 *                     start time; every event at a given time is applied, in registration order, and together they
	 *                     yield a single value for that time.
	 */
	void recalc(NavigableMap<Double, List<NetworkChangeEvent>> changeEvents, ChangeValueGetter valueGetter,
			double baseValue);

	void incChangeEvents();

	void clearEvents();
}
