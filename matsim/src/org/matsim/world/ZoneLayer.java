/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneLayer.java
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

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;

/**
 * The collection of zone objects in MATSim.
 * @see Layer
 * @see NetworkLayer
 * @see Facilities
 * @author Michael Balmer
 */
public class ZoneLayer extends Layer {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	protected ZoneLayer(final Id type, final String name) {
		super(type, name);
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Zone createZone(final String id, final String center_x, final String center_y,
	                             final String min_x, final String min_y, final String max_x, final String max_y,
	                             final String area, final String zoneName) {
		Id i = new IdImpl(id);
		if (this.locations.containsKey(i)) { Gbl.errorMsg(this.toString() + "[zone id=" + id + " already exists]"); }
		CoordI center = null;
		CoordI min = null;
		CoordI max = null;
		if ((center_x != null) && (center_y != null)) { center = new Coord(center_x, center_y); }
		if ((min_x != null) && (min_y != null)) { min = new Coord(min_x, min_y); }
		if ((max_x != null) && (max_y != null)) { max = new Coord(max_x, max_y); }
		Zone z = new Zone(this, new IdImpl(id), center, min, max);
		if (area != null) {
			z.setArea(Double.parseDouble(area));
		}
		z.setName(zoneName);
		this.locations.put(i,z);
		return z;
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return super.toString();
	}
}
