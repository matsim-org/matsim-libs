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

package playground.pieter.balmermi.world;

import java.util.Map;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

/**
 * The collection of zone objects in MATSim.
 * @see LayerImpl
 * @author Michael Balmer
 */
public class ZoneLayer extends LayerImpl {

	public final Zone createZone(final Id id, final String center_x, final String center_y,
	                             final String min_x, final String min_y, final String max_x, final String max_y) {

		if (this.getLocations().containsKey(id)) { throw new RuntimeException(this.toString() + "[zone id=" + id + " already exists]"); }
		Coord center = null;
		Coord min = null;
		Coord max = null;
		if ((center_x != null) && (center_y != null)) {
			center = new Coord(Double.parseDouble(center_x), Double.parseDouble(center_y)); }
		if ((min_x != null) && (min_y != null)) {
			min = new Coord(Double.parseDouble(min_x), Double.parseDouble(min_y)); }
		if ((max_x != null) && (max_y != null)) {
			max = new Coord(Double.parseDouble(max_x), Double.parseDouble(max_y)); }
		Zone z = new Zone(id, center, min, max);
		Map<Id,BasicLocation> locations = this.getLocations();
		locations.put(id,z);
		return z;
	}

}
