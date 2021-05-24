/* *********************************************************************** *
 * project: org.matsim.*
 * Activity.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.facilities;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class ActivityOptionImpl implements ActivityOption {

	private final String type;
	private Double capacity = (double)Integer.MAX_VALUE;
	private SortedSet<OpeningTime> openingTimes = new TreeSet<>();

	public ActivityOptionImpl(final String type) {
		this.type = type;
	}

	public void clearOpeningTimes() {
		this.openingTimes.clear();
	}
	
	@Override
	public void addOpeningTime(OpeningTime opentime) {
		if (openingTimes.isEmpty()) {
			openingTimes.add(opentime);
			return;
		}
		TreeSet<OpeningTime> new_o_set = new TreeSet<>();
		Iterator<OpeningTime> o_it = openingTimes.iterator();
		while (o_it.hasNext()) {
			OpeningTime o = o_it.next();
			int merge_type = o.compareTo(opentime); // see Opentime for the meaning
			if ((merge_type == -6) || (merge_type == 6)) {
				// complete disjoint
				new_o_set.add(o);
				new_o_set.add(opentime);
			}
			else if ((merge_type >= -1) && (merge_type <= 2)) {
				// opentime is subset of o
				new_o_set.add(o);
			}
			else if ((merge_type == -3) || (merge_type == -2) || (merge_type == 3)) {
				// o is subset of opentime
				new_o_set.add(opentime);
			}
			else { // union of the two opentimes
				if ((merge_type == -5) || (merge_type == -4)) {
					// start_time of opentime and endtime of o
					opentime.setEndTime(o.getEndTime());
					new_o_set.add(opentime);
				}
				else if ((merge_type == 4) || (merge_type == 5)) {
					// start_time of o and endtime of opentime
					opentime.setStartTime(o.getStartTime());
					new_o_set.add(opentime);
				}
				else {
					throw new RuntimeException("[Something is wrong]");
				}
			}
		}
		this.openingTimes.clear();
		this.openingTimes.addAll(new_o_set);
	}
	
	public void setOpeningTimes(Collection<OpeningTime> times) {
		this.clearOpeningTimes();
		for (OpeningTime t : times) {
			this.addOpeningTime(t);
		}
	}

	@Override
	public final void setCapacity(double capacity) {
		if (capacity < 0) {
			throw new NumberFormatException("A capacity of an activity must be >= 0.");
		}
		this.capacity = capacity;
	}

	@Override
	public final String getType() {
		return this.type;
	}

	@Override
	public final double getCapacity() {
		return this.capacity;
	}

	@Override
	public final SortedSet<OpeningTime> getOpeningTimes() {
		return this.openingTimes;
	}

	@Override
	public final String toString() {
		return "[type=" + this.type + "]" +
				"[capacity=" + this.capacity + "]" +
				"[nof_opentimes=" + this.getOpeningTimes().size() + "]";
	}

}
