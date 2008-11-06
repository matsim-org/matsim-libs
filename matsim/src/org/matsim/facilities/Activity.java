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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.gbl.Gbl;

public class Activity {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String type;
	private int capacity = Integer.MAX_VALUE; // MAX_VALUE == unlimited capcacity
	private final Facility facility;

	// TreeMap(String day,TreeSet(Opentime opentime))
	private Map<DayType,TreeSet<OpeningTime>> opentimes = new TreeMap<DayType,TreeSet<OpeningTime>>();

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public Activity(final String type, final Facility facility) {
		this.type = type;
		this.facility = facility;
		if (this.facility == null) { Gbl.errorMsg("facility=null not allowed!"); }
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////
	@Deprecated
	public final void createOpentime(final DayType day, final double startTime, final double endTime) {
		OpeningTime o = new OpeningTime(day, startTime, endTime);
		this.addOpentime(o);
	}
	
	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	public final void addOpentime(final OpeningTime opentime) {
		DayType day = opentime.getDay();
		if (!this.opentimes.containsKey(day)) {
			this.opentimes.put(day,new TreeSet<OpeningTime>());
		}
		TreeSet<OpeningTime> o_set = this.opentimes.remove(day);
		if (o_set.isEmpty()) {
			o_set.add(opentime);
			this.opentimes.put(day,o_set);
		}
		else {
			TreeSet<OpeningTime> new_o_set = new TreeSet<OpeningTime>();
			Iterator<OpeningTime> o_it = o_set.iterator();
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
						Gbl.errorMsg("[Something is wrong]");
					}
				}
			}
			this.opentimes.put(day,new_o_set);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////

	public final boolean containsOpentime(final OpeningTime o) {
		Set<OpeningTime> o_set = this.getOpentimes(o.getDay());
		if (o_set == null) {
			return false;
		}
		return o_set.contains(o);
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setCapacity(final int capacity) {
		if (capacity < 0) {
			throw new NumberFormatException("A capacity of an activity must be >= 0.");
		}
		this.capacity = capacity;
	}

	public void setOpentimes(Map<DayType, TreeSet<OpeningTime>> opentimes) {
		this.opentimes = opentimes;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final String getType() {
		return this.type;
	}

	public final Facility getFacility() {
		return this.facility;
	}

	public final int getCapacity() {
		return this.capacity;
	}

	public final Map<DayType,TreeSet<OpeningTime>> getOpentimes() {
		return this.opentimes;
	}

	public final Set<OpeningTime> getOpentimes(final DayType day) {
		return this.opentimes.get(day);
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[type=" + this.type + "]" +
				"[capacity=" + this.capacity + "]" +
				"[facility_id=" + this.facility.getId() + "]" +
				"[nof_opentimes=" + this.getOpentimes().size() + "]";
	}
}
