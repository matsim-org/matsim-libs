/* *********************************************************************** *
 * project: org.matsim.*
 * Location.java
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

package playground.fabrice.primloc;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacilityImpl;

/**
 * Basic geographical class in MATSim.
 * @see Link
 * @see ActivityFacilityImpl
 * @see Zone
 * @author Michael Balmer
 */
public abstract class AbstractLocation implements BasicLocation {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected final Id id;
	protected final Coord center;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	/**
	 * A unique location for a given layer.
	 * @param id The unique id of that location.
	 * @param center The center of that location. Does not have to be the middle of the location object.
	 */
	protected AbstractLocation(final Id id, final Coord center) {
		this.id = id;
		this.center = center;
		if (this.center == null) {
			throw new RuntimeException("Location id=" + id + " instanciate without coordinate!");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public Id getId() {
		return this.id;
	}

	@Override
	public final Coord getCoord() {
		return this.center;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		return "[id=" + this.getId() + "]" +
		       "[center=" + this.center + "]";
	}
}
