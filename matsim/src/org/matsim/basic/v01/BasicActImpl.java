/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAct.java
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

package org.matsim.basic.v01;

import org.matsim.utils.geometry.Coord;
import org.matsim.utils.misc.Time;

public class BasicActImpl implements BasicAct {

	// TODO: should be private but needs refactoring in derived classes
	protected double endTime = Time.UNDEFINED_TIME;
	protected double startTime = Time.UNDEFINED_TIME;
	protected double dur = Time.UNDEFINED_TIME;

	private String type;
	private Coord coord = null;
	private Id linkId;
	private Id facilityId;

	public BasicActImpl(String type) {
		this.type = type;
	}
	
	public final double getEndTime() {
		return this.endTime;
	}

	public final String getType() {
		return this.type;
	}
//
//	public BasicLink getLink() {
//		return this.link;
//	}

//	public Facility getFacility() {
//		return this.facility;
//	}

	public final void setEndTime(final double endTime) {
		this.endTime = endTime;
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

	public final double getStartTime() {
		return this.startTime;
	}

	public final void setStartTime(final double startTime) {
		this.startTime = startTime;
	}

	@Override
	public int hashCode() {
		return this.type.hashCode() ^ this.linkId.toString().hashCode(); // XOR of two hashes
	}

	public void setFacilityId(Id locationId) {
		this.facilityId = locationId;
	}
	
	public void setLinkId(Id linkid) {
		this.linkId = linkid;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public Id getFacilityId() {
		return this.facilityId;
	}


	public double getDur() {
		return this.dur;
	}

	public void setDur(final double dur) {
		this.dur = dur;
	}
}
