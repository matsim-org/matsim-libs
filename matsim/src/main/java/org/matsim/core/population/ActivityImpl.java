/* *********************************************************************** *
 * project: org.matsim.*
 * Act.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;

public class ActivityImpl implements Activity {

	private double endTime = Time.UNDEFINED_TIME;

	/** @deprecated I don't think this is used/interpreted anywhere. Kai, jun09 */
	@Deprecated
	private double startTime = Time.UNDEFINED_TIME;
	
	private double dur = Time.UNDEFINED_TIME;

	private String type;
	private Coord coord = null;
	protected Link link = null;
	protected ActivityFacility facility = null;
	
	private ActivityImpl(final String type) {
		this.type = type.intern();
	}
	
	public ActivityImpl(final String type, final Link link) {
		this(type);
		this.setLink(link);
	}

	public ActivityImpl(final String type, final Coord coord) {
		this(type);
		this.setCoord(coord);
	}

	public ActivityImpl(final String type, final ActivityFacility fac) {
		this(type);
		this.setFacility(fac);
	}

	public ActivityImpl(final String type, final Coord coord, final Link link) {
		this(type, link);
		this.setCoord(coord);
	}

	public ActivityImpl(final ActivityImpl act) {
		this(act.getType());
		// Act coord could be null according to first c'tor!
		Coord c = act.getCoord() == null ? null : new CoordImpl(act.getCoord());
		this.setCoord(c);
		this.link = act.getLink();
		this.setStartTime(act.getStartTime());
		this.setEndTime(act.getEndTime());
		this.setDuration(act.getDuration());
		this.setFacility(act.getFacility());
	}

	public final double getEndTime() {
		return this.endTime;
	}
	
	public final void setEndTime(final double endTime) {
		this.endTime = endTime;
	}

	/** @deprecated I don't think this is used/interpreted anywhere. Kai, jun09 */
	@Deprecated
	public final double getStartTime() {
		return this.startTime;
	}

	/** @deprecated I don't think this is used/interpreted anywhere. Kai, jun09 */
	@Deprecated
	public final void setStartTime(final double startTime) {
		this.startTime = startTime;
	}
	
	public final String getType() {
		return this.type;
	}

	public final void setType(final String type) {
		this.type = type.intern();
	}
	
	public final Coord getCoord() {
		return this.coord;
	}

	public void setCoord(final Coord coord) {
		this.coord = coord;
	}

	public final Id getLinkId() { 
		if (this.link != null)
			return this.link.getId();
		return null;
	}
	
	public final Id getFacilityId() {
		if (this.facility != null)
			return this.facility.getId();
		return null;
	}
	
	public final void setFacility(final ActivityFacility facility) {
		this.facility = facility;
	}

	public final void setLink(final Link link) {
		this.link = link;
	}
	
	@Deprecated // use getLinkId
	public Link getLink() {
		return this.link;
	}

	@Deprecated // use getFacilityId
	public ActivityFacility getFacility() {
		return this.facility;
	}
	
	@Override
	public final String toString() {
		return "[type=" + this.getType() + "]" +
				"[coord=" + this.getCoord() + "]" +
				"[link=" + this.link + "]" +
				"[startTime=" + Time.writeTime(this.getStartTime()) + "]" +
				"[endTime=" + Time.writeTime(this.getEndTime()) + "]";
	}



	/**
	 * This method calculates the duration of the activity from the start and endtimes if set.
	 * If neither end nor starttime is set, but the duration is stored in the attribute of the
	 * class the duration is returned.
	 * If only start time is set, assume this is the last activity of the day.
	 * If only the end time is set, assume this is the first activity of the day.
	 * If the duration could neither be calculated nor the act.dur attribute is set to a value
	 * not equal to Time.UNDEFINED_TIME an exception is thrown.
	 * @return the duration in seconds
	 * @deprecated define algo with activity_end_time.  kn, jun09
	 */
	@Deprecated
	public double calculateDuration() {
		if ((this.getStartTime() == Time.UNDEFINED_TIME) && (this.getEndTime() == Time.UNDEFINED_TIME)) {
			if (this.getDuration() != Time.UNDEFINED_TIME) {
				return this.getDuration();
			}
			throw new IllegalArgumentException("No valid time set to calculate duration of activity: StartTime: " + this.getStartTime() + " EndTime : " + this.getEndTime()+ " Duration: " + this.getDuration());
		}
		//if only start time is set, assume this is the last activity of the day
		else if ((this.getStartTime() != Time.UNDEFINED_TIME) && (this.getEndTime() == Time.UNDEFINED_TIME)) {
			return Time.MIDNIGHT - this.getStartTime();
		}
		//if only the end time is set, assume this is the first activity of the day
		else if ((this.getStartTime() == Time.UNDEFINED_TIME) && (this.getEndTime() != Time.UNDEFINED_TIME)) {
			return this.getEndTime();
		}
		else {
			return this.getEndTime() - this.getStartTime();
		}
	}

	/** @deprecated define algo with activity_end_time.  kn, jun09 */
	@Deprecated
	public double getDuration() {
		return this.dur;
	}

	/** @deprecated define algo with activity_end_time.  kn, jun09 */
	@Deprecated
	public void setDuration(final double dur) {
		this.dur = dur;
	}
	
}
