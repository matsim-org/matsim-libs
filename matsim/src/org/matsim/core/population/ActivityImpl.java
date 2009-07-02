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

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.core.api.experimental.population.Activity;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.basic.v01.BasicActivityImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;

public class ActivityImpl extends BasicActivityImpl implements Activity {

	private BasicLink link = null;
	private ActivityFacility facility = null;
	private double dur = Time.UNDEFINED_TIME;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ActivityImpl(final String type, final LinkImpl link) {
		super(type.intern());
		this.setLink(link);
	}

	public ActivityImpl(final String type, final Coord coord) {
		super(type.intern());
		this.setCoord(coord);
	}

	public ActivityImpl(final String type, final ActivityFacility fac) {
		super(type.intern());
		this.setFacility(fac);
	}

	public ActivityImpl(final String type, final Coord coord, final LinkImpl link) {
		this(type, link);
		this.setCoord(coord);
	}

	public ActivityImpl(final ActivityImpl act) {
		super(act.getType());
		// Act coord could be null according to first c'tor!
		Coord c = act.getCoord() == null ? null : new CoordImpl(act.getCoord());
		this.setCoord(c);
		this.link = act.getLink();
		this.setStartTime(act.getStartTime());
		this.setEndTime(act.getEndTime());
		this.setDuration(act.getDuration());
		this.setFacility(act.getFacility());
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////
	
	public void setCoord( final Coord coord ) {
		super.setCoord ( coord ) ;
	}

	public void setFacility(final ActivityFacility facility) {
		this.facility = facility;
	}

	public void setLink(final BasicLink link) {
		this.link = link;
	}

	@Override
	public final String toString() {
		return "[type=" + this.getType() + "]" +
				"[coord=" + this.getCoord() + "]" +
				"[link=" + this.link + "]" +
				"[startTime=" + Time.writeTime(this.getStartTime()) + "]" +
				"[endTime=" + Time.writeTime(this.getEndTime()) + "]";
	}


	// here to return correct link type
	public final LinkImpl getLink() {
		return (LinkImpl)this.link;
	}

	public ActivityFacility getFacility() {
		return this.facility;
	}

	public void setFacilityId(final Id id) {
		throw new UnsupportedOperationException("not yet, please wait...");
	}
	
	@Override
	public final Id getLinkId() { // convenience method
		if (this.link != null)
			return this.link.getId();
		return null;
	}
	
	public void setLinkId(final Id id) {
		throw new UnsupportedOperationException("not yet, please wait...");
	}
	
	@Override
	public final Id getFacilityId() {
		if (this.facility != null)
			return this.facility.getId();
		return null;
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
	public double getDuration() {
		return this.dur;
	}

	/** @deprecated define algo with activity_end_time.  kn, jun09 */
	public void setDuration(final double dur) {
		this.dur = dur;
	}
	
}
