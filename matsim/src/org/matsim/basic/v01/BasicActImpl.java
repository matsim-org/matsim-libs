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

import org.matsim.facilities.Facility;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.misc.Time;

public class BasicActImpl implements BasicAct {

	// TODO: should be private but needs refactoring in derived classes
	protected double endTime = Time.UNDEFINED_TIME;
	protected String type;
	protected BasicLink link = null;
	protected Facility facility = null;

	private CoordI coord = null;

	protected double startTime = Time.UNDEFINED_TIME;
	protected double dur = Time.UNDEFINED_TIME;

	public final double getEndTime() {
		return this.endTime;
	}

	public final String getType() {
		return this.type;
	}

	public BasicLink getLink() {
		return this.link;
	}

	public Facility getFacility() {
		return this.facility;
	}

	public final void setEndTime(final double endTime) {
		this.endTime = endTime;
	}

	public final void setType(final String type) {
		this.type = type.intern();
	}

	public final void setLink(final BasicLink link) {
		this.link = link;
	}

	public final void setFacility(final Facility facility) {
		this.facility = facility;
	}

	public final CoordI getCoord() {
		return this.coord;
	}

	public void setCoord(final CoordI coord) {
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
		return this.type.hashCode() ^ this.link.getId().toString().hashCode(); // XOR of two hashes
	}


}
