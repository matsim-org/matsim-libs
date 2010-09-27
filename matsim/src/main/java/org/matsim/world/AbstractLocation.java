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

package org.matsim.world;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;

/**
 * Basic geographical class in MATSim.
 * @see LinkImpl
 * @see ActivityFacilityImpl
 * @see Zone
 * @author Michael Balmer
 */
public abstract class AbstractLocation implements MappedLocation {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	// TODO [balmermi] The id should be unchangeable ('final'), but there
	// are modules which actually want to change ids of locations (see NetworkCleaner).
	// I'm not that happy the Ids can change (otherwise it would not be an id)!
	protected Id id;
	protected final Layer layer;
	protected final Coord center;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	/**
	 * A unique location for a given layer.
	 * @param layer The layer the location belongs to.
	 * @param id The unique id of that location.
	 * @param center The center of that location. Does not have to be the middle of the location object.
	 */
	protected AbstractLocation(final Layer layer, final Id id, final Coord center) {
		this.layer = layer;
		this.id = id;
		this.center = center;
		if (this.center == null) {
			Gbl.errorMsg("Location id=" + id + " instanciate without coordinate!");
		}
		if (this.layer == null) {
			Gbl.errorMsg("Location id=" + id + " instanciate without layer!");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public abstract double calcDistance(final Coord coord);


	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public void setCoord(Coord coord) {
		throw new UnsupportedOperationException( " derived classes need to implement their own setCoord methods if desired " ) ;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final Id getId() {
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
		       "[layer=" + this.layer + "]" +
		       "[center=" + this.center + "]";
	}
}
