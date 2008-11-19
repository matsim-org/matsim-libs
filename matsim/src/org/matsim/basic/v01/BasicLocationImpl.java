/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.matsim.interfaces.basic.v01.BasicLocation;
import org.matsim.interfaces.basic.v01.LocationType;
import org.matsim.utils.geometry.Coord;

/**
 * @author dgrether
 */
public class BasicLocationImpl implements BasicLocation {

	private Coord coordinate = null;
	
	private Id locationId;
	
	private LocationType type;

	
	public void setCoord(Coord coord) {
		this.coordinate = coord;
	}
	
	public void setLocationId(Id id, LocationType type) {
		this.locationId = id;
		this.type = type;
	}

	public Id getId() {
		return this.locationId;
	}

	public Coord getCenter() {
		return this.coordinate;
	}

	public LocationType getLocationType() {
		return this.type;
	}

}