/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.analysis.mc.filters;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.anhorni.locationchoice.analysis.mc.MZTrip;

public class ZHTripFilter {
	
	private double radius = 30.0 * 1000.0;
	private Coord center = new Coord(683518.0, 246836.0);

	public List<MZTrip> filterRegion(List<MZTrip> trips) {
		List<MZTrip> filteredTrips = new Vector<MZTrip>();	
		Iterator<MZTrip> trips_it = trips.iterator();
		while (trips_it.hasNext()) {
			MZTrip trip = trips_it.next();
			
			if (this.intersect(trip, radius, center)) {
				filteredTrips.add(trip);
			}
		}
		return filteredTrips;
	}	
	
	private boolean intersect(MZTrip mzTrip, double radius, Coord center) {
		
		double distance = CoordUtils.distancePointLinesegment(
				mzTrip.getCoordStart(), mzTrip.getCoordEnd(), center);
		
		if (distance <= radius) return true;
		return false;
	}
}
