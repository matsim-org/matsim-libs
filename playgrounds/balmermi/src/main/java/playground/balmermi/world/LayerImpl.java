/* *********************************************************************** *
 * project: org.matsim.*
 * Layer.java
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

package playground.balmermi.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Basic collection of same geographical objects in MATSim.
 * @see ActivityFacilitiesImpl
 * @see ZoneLayer
 * @author Michael Balmer
 */
public class LayerImpl implements Layer {

	final TreeMap<Id, BasicLocation> locations = new TreeMap<Id,BasicLocation>();

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final BasicLocation getLocation(final Id location_id) {
		return this.locations.get(location_id);
	}

	/**
	 * Note: this is method is, I think, <em> not </em> quad-tree based, and therefore is rather slow in
	 * most cases.
     *
	 * @param coord A coordinate to which the nearest location should be returned.
	 *
	 * @return the Location with the smallest distance to the given coordinate. If multiple locations have
	 * the same minimal distance, all of them are returned.
	 */
	@Override
	public final ArrayList<BasicLocation> getNearestLocations(final Coord coord) {
		return getNearestLocations(coord, null);
	}

	/**
	 * Note: this is method is, I think, <em> not </em> quad-tree based, and therefore is rather slow in
	 * most cases.
	 *
	 * @param coord A coordinate to which the nearest location should be returned.
	 * @param excludeLocation A location that should be ignored when finding the nearest location. Useful to
	 * find the nearest neighbor of the excluded location.
	 *
	 * @return the Location with the smallest distance to the given coordinate. If multiple locations have
	 * the same minimal distance, all of them are returned.
	 *
	 */
	@Override
	public final ArrayList<BasicLocation> getNearestLocations(final Coord coord, final BasicLocation excludeLocation) {
		ArrayList<BasicLocation> locs = new ArrayList<BasicLocation>();
		double shortestDistance = Double.MAX_VALUE;
		Iterator<BasicLocation> loc_it = this.locations.values().iterator();
		while (loc_it.hasNext()) {
			BasicLocation loc = loc_it.next();
			if (loc != excludeLocation) {
				double distance = CoordUtils.calcDistance(loc.getCoord(), coord);
				if (distance == shortestDistance) { locs.add(loc); }
				if (distance < shortestDistance) { shortestDistance = distance; locs.clear(); locs.add(loc); }
			}
		}
		return locs;
	}

	@Override
	public final Map<Id, BasicLocation> getLocations() {
		return this.locations;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		return "[nof_locations=" + this.locations.size() + "]";
	}
}
