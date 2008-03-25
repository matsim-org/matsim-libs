/* *********************************************************************** *
 * project: org.matsim.*
 * Opentime.java
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


/* *********************************************************************** *
 *                  org.matsim.demandmodeling.facilities                   *
 *                              Opentime.java                              *
 *                          ---------------------                          *
 * copyright       : (C) 2006 by                                           *
 *                   Michael Balmer, Konrad Meister, Marcel Rieser,        *
 *                   David Strippgen, Kai Nagel, Kay W. Axhausen,          *
 *                   Technische Universitaet Berlin (TU-Berlin) and        *
 *                   Swiss Federal Institute of Technology Zurich (ETHZ)   *
 * email           : balmermi at gmail dot com                             *
 *                 : rieser at gmail dot com                               *
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

package playground.ciarif.retailers;

import org.matsim.gbl.Gbl;
import org.matsim.utils.misc.Time;

//TODO: REMOVE
public class Opentime implements Comparable<Opentime> {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String day;
	private double startTime;
	private double endTime;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public Opentime(final String day, final String start_time, final String end_time) {
		this.day = day;
		this.startTime = Time.parseTime(start_time);
		this.endTime = Time.parseTime(end_time);
		this.acceptTimes();
	}

	//////////////////////////////////////////////////////////////////////
	//
	// interface implementation
	//
	//////////////////////////////////////////////////////////////////////

	public int compareTo(Opentime other) {
		// two functionalities in one:
		// 1. the earlier start_time comes before the other. If they're the same,
		//    the end times decides which comes first
		// 2. the meaning of the return value. See the ASCII figures for that.
		if (this.startTime > other.endTime) {         // this:       |-----|
			return -6;                                    // other: |--|
		}
		else if (this.startTime == other.endTime) {   // this:       |-----|
			return -5;                                    // other: |----|
		}
		else if (this.startTime > other.startTime) {
			if (this.endTime > other.endTime) {         // this:       |-----|
				return -4;                                  // other: |--------|
			}
			else if (this.endTime == other.endTime) {   // this:       |-----|
				return -3;                                  // other: |----------|
			}
			else {                                        // this:       |-----|
				return -2;                                  // other: |---------------|
			}
		}
		else if (this.startTime == other.startTime) {
			if (this.endTime > other.endTime) {         // this:       |-----|
				return -1;                                  // other:      |---|
			}
			else if (this.endTime == other.endTime) {   // this:       |-----|
				return 0;                                   // other:      |-----|
			}
			else {                                        // this:       |-----|
				return 3;                                   // other:      |----------|
			}
		}
		else if (this.endTime > other.endTime) {      // this:       |-----|
			return 2;                                     // other:        |-|
		}
		else if (this.endTime == other.endTime) {     // this:       |-----|
			return 1;                                     // other:        |---|
		}
		else if (this.endTime > other.startTime) {    // this:       |-----|
			return 4;                                     // other:        |--------|
		}
		else if (this.endTime == other.startTime) {   // this:       |-----|
			return 5;                                     // other:            |----|
		}
		else {                                          // this:       |-----|
			return 6;                                     // other:              |--|
		}
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final boolean equals(final Object o) {
		if (o instanceof Opentime) {
			Opentime other = (Opentime)o;
			if (other.day.equals(this.day) && (other.startTime == this.startTime) && (other.endTime == this.endTime)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public final int hashCode() {
		/* equals() checks day, startTime and endTime, so we should include those into hashCode as well */
		return (this.day.hashCode() + Double.valueOf(this.startTime).hashCode()
				+ Double.valueOf(this.endTime).hashCode());
	}

	private final void acceptTimes() {
		if (this.startTime >= this.endTime) {
			Gbl.errorMsg(this + "[startTime=" + this.startTime + " >= endTime=" + this.endTime + " not allowed]");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	protected final void setStartTime(final double start_time) {
		this.startTime = start_time;
		this.acceptTimes();
	}

	protected final void setEndTime(final double end_time) {
		this.endTime = end_time;
		this.acceptTimes();
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final String getDay() {
		return this.day;
	}

	public final double getStartTime() {
		return this.startTime;
	}

	public final double getEndTime() {
		return this.endTime;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[day=" + this.day + "]" +
				"[startTime=" + this.startTime + "]" +
				"[endTime=" + this.endTime + "]";
	}
}
