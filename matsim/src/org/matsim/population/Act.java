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

package org.matsim.population;

import org.matsim.basic.v01.BasicActImpl;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;

public class Act extends BasicActImpl {

	protected BasicLink link = null;
	protected Facility facility = null;


	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Act(final String type, final Link link) {
		super(type.intern());
		this.setLink(link);
	}

	public Act(final String type, final Coord coord) {
		super(type.intern());
		this.setCoord(coord);
	}

	public Act(final String type, final Facility fac) {
		super(type.intern());
		this.setFacility(fac);
	}

	public Act(final String type, final Coord coord, final Link link) {
		this(type, link);
		this.setCoord(coord);
	}

	@Deprecated
	public Act(final String type, final double x, final double y, final Link link,
			final double startTime, final double endTime, final double dur, final boolean isPrimary) {
  	super(type);
  	this.setCoord(new CoordImpl(x, y));
		this.link = link;
		this.startTime = startTime;
		this.endTime = endTime;
		this.dur = dur;
	}

	public Act(final Act act) {
		super(act.getType());
		// Act coord could be null according to first c'tor!
		Coord c = act.getCoord() == null ? null : new CoordImpl(act.getCoord());
		this.setCoord(c);
		this.link = act.link;
		this.startTime = act.startTime;
		this.endTime = act.endTime;
		this.dur = act.dur;
		this.setFacility(act.getFacility());
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public void setFacility(final Facility facility) {
		this.facility = facility;
	}

	public void setLink(final BasicLink link) {
		this.link = link;
	}

	protected void setFacility(final String f_id) {
		Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
		if (facilities == null) { throw new RuntimeException("Facilities Layer does not exist!"); }
		this.facility = (Facility)facilities.getLocation(f_id);
		if (this.facility == null) { throw new RuntimeException("facility id="+f_id+" does not exist"); }
	}

	protected void setLinkFromString(final String link) {
		NetworkLayer network = (NetworkLayer)Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
		if (network == null) {
			throw new RuntimeException("Network layer does not exist");
		}
		this.link = (Link)network.getLocation(link);

		if (this.link == null) {
			throw new RuntimeException("link=" + link +" does not exist");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[type=" + this.getType() + "]" +
				"[coord=" + this.getCoord() + "]" +
				"[link=" + this.link + "]" +
				"[startTime=" + Time.writeTime(this.startTime) + "]" +
				"[endTime=" + Time.writeTime(this.endTime) + "]" +
				"[dur=" + Time.writeTime(this.dur) + "]";
	}

	public final double getDur() {
		return this.dur;
	}

	public final void setDur(final double dur) {
		this.dur = dur;
	}

	// here to return correct link type
	public final Link getLink() {
		return (Link)this.link;
	}

	public Facility getFacility() {
		return this.facility;
	}

	@Override
	public final Id getLinkId() { // convenience method
		if (this.link != null)
			return this.link.getId();
		return null;
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
	 */
	public double calculateDuration() {
		if (this.startTime == Time.UNDEFINED_TIME && this.endTime == Time.UNDEFINED_TIME) {
			if (this.dur != Time.UNDEFINED_TIME) {
				return this.dur;
			}
			throw new IllegalArgumentException("No valid time set to calculate duration of activity: StartTime: " + this.startTime + " EndTime : " + this.endTime + " Duration: " + this.dur);
		}
		//if only start time is set, assume this is the last activity of the day
		else if (this.startTime != Time.UNDEFINED_TIME && this.endTime == Time.UNDEFINED_TIME) {
			return Time.MIDNIGHT - this.startTime;
		}
		//if only the end time is set, assume this is the first activity of the day
		else if (this.startTime == Time.UNDEFINED_TIME && this.endTime != Time.UNDEFINED_TIME) {
			return this.endTime;
		}
		else {
			return this.endTime - this.startTime;
		}
	}

}
