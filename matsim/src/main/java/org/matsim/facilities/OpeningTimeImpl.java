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

package org.matsim.facilities;

import org.matsim.core.utils.misc.Time;

public class OpeningTimeImpl implements OpeningTime {

	public static enum TimeRelation { 
		START_AND_END_BEFORE, 
		START_AND_END_AFTER, 
		START_AND_END_WITHIN, 
		START_BEFORE_END_WITHIN,
		START_WITHIN_END_AFTER,
		START_BEFORE_END_AFTER
	}
	
	private double startTime;
	private double endTime;
	
	public OpeningTimeImpl(final double startTime, final double endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.validateTimes();
	}

	//////////////////////////////////////////////////////////////////////
	//
	// interface implementation
	//
	//////////////////////////////////////////////////////////////////////

	@Override
	public int compareTo(OpeningTime other) {
		// two functionalities in one:
		// 1. the earlier start_time comes before the other. If they're the same,
		//    the end times decides which comes first
		// 2. the meaning of the return value. See the ASCII figures for that.
		if (this.startTime > other.getEndTime()) {         // this:       |-----|
			return -6;                                       // other: |--|
		}
		else if (this.startTime == other.getEndTime()) {   // this:       |-----|
			return -5;                                       // other: |----|
		}
		else if (this.startTime > other.getStartTime()) {
			if (this.endTime > other.getEndTime()) {         // this:       |-----|
				return -4;                                     // other: |--------|
			}
			else if (this.endTime == other.getEndTime()) {   // this:       |-----|
				return -3;                                     // other: |----------|
			}
			else {                                        // this:       |-----|
				return -2;                                  // other: |---------------|
			}
		}
		else if (this.startTime == other.getStartTime()) {
			if (this.endTime > other.getEndTime()) {         // this:       |-----|
				return -1;                                     // other:      |---|
			}
			else if (this.endTime == other.getEndTime()) {   // this:       |-----|
				return 0;                                      // other:      |-----|
			}
			else {                                           // this:       |-----|
				return 3;                                      // other:      |----------|
			}
		}
		else if (this.endTime > other.getEndTime()) {      // this:       |-----|
			return 2;                                        // other:        |-|
		}
		else if (this.endTime == other.getEndTime()) {     // this:       |-----|
			return 1;                                     // other:        |---|
		}
		else if (this.endTime > other.getStartTime()) {    // this:       |-----|
			return 4;                                     // other:        |--------|
		}
		else if (this.endTime == other.getStartTime()) {   // this:       |-----|
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
		if (o instanceof OpeningTimeImpl) {
			OpeningTimeImpl other = (OpeningTimeImpl)o;
			if ((other.startTime == this.startTime) && (other.endTime == this.endTime)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public final int hashCode() {
		/* equals() checks day, startTime and endTime, so we should include those into hashCode as well */
		return (Double.valueOf(this.startTime).hashCode()
				+ Double.valueOf(this.endTime).hashCode());
	}

	private final void validateTimes() {
		if (this.startTime >= this.endTime) {
			throw new RuntimeException(this + "[startTime=" + this.startTime + " >= endTime=" + this.endTime + " not allowed]");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final void setStartTime(final double start_time) {
		this.startTime = start_time;
		this.validateTimes();
	}

	@Override
	public final void setEndTime(final double end_time) {
		this.endTime = end_time;
		this.validateTimes();
	}
	
	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final double getStartTime() {
		return this.startTime;
	}

	@Override
	public final double getEndTime() {
		return this.endTime;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[startTime=" + Time.writeTime(this.startTime) + "]" +
				"[endTime=" + Time.writeTime(this.endTime) + "]";
	}

}
