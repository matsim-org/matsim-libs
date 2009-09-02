/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesSpatialCut.java
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

package playground.balmermi.algos;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class FacilitiesSpatialCut {

	// Zurich Cut
	private final CoordImpl min = new CoordImpl(669000.0,223900.0);
	private final CoordImpl max = new CoordImpl(717000.0,283400.0);

	public void run(ActivityFacilitiesImpl facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		ArrayList<ActivityFacilityImpl> f_array = new ArrayList<ActivityFacilityImpl>();
		for (ActivityFacilityImpl f : facilities.getFacilities().values()) {
			Coord c = f.getCoord();
			if ((c.getX() >= this.min.getX()) && (c.getY() >= this.min.getY()) &&
			    (c.getX() <= this.max.getX()) && (c.getY() <= this.max.getY())) {
				// inside
				f_array.add(f);
			}
			else { // outside
			}
		}

		TreeMap<Id, ActivityFacilityImpl> fs = (TreeMap<Id, ActivityFacilityImpl>) facilities.getFacilities();
		fs.clear();

		for (int i=0; i<f_array.size(); i++) {
			ActivityFacilityImpl f = f_array.get(i);
			fs.put(f.getId(), f);
		}

		System.out.println("    done.");
	}
}
