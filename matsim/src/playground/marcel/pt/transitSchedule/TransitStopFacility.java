/* *********************************************************************** *
 * project: org.matsim.*
 * TransitStopFacility.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.transitSchedule;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Link;

public class TransitStopFacility implements Facility {

	private final Id id;
	private final Coord coord;
	private Link link = null;
	
	public TransitStopFacility(final Id id, final Coord coord) {
		this.id = id;
		this.coord = coord;
	}
	
	public Link getLink() {
		return this.link;
	}
	
	public void setLink(final Link link) {
		this.link = link;
	}

	public Id getLinkId() {
		if (this.link == null) {
			return null;
		}
		return this.link.getId();
	}

	public Coord getCoord() {
		return this.coord;
	}

	public Id getId() {
		return this.id;
	}

}
