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

package org.matsim.core.basic.v01;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.utils.misc.Time;

public class BasicActivityImpl implements BasicActivity {

	private double endTime = Time.UNDEFINED_TIME;
	
	/** @deprecated I don't think this is used/interpreted anywhere. Kai, jun09 */
	private double startTime = Time.UNDEFINED_TIME;
	
	private String type;
	private Coord coord = null;
	protected BasicLink link = null;
	protected ActivityFacility facility = null;

	protected BasicActivityImpl(String type) {
		this.type = type;
	}
	
	public final double getEndTime() {
		return this.endTime;
	}

	public final String getType() {
		return this.type;
	}

	public final void setEndTime(final double endTime) {
		this.endTime = endTime;
	}

	public final void setType(final String type) {
		this.type = type.intern();
	}


	public final Coord getCoord() {
		return this.coord;
	}

	/* deliberately */ protected void setCoord(final Coord coord) {
		this.coord = coord;
	}

	/** @deprecated I don't think this is used/interpreted anywhere. Kai, jun09 */
	public final double getStartTime() {
		return this.startTime;
	}

	/** @deprecated I don't think this is used/interpreted anywhere. Kai, jun09 */
	public final void setStartTime(final double startTime) {
		this.startTime = startTime;
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

	public final void setLink(final BasicLink link) {
		this.link = link;
	}
	
	// here to return correct link type
	public BasicLink getLink() {
		return this.link;
	}

	public ActivityFacility getFacility() {
		return this.facility;
	}
}
