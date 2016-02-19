/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesCalcMinDist.java
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

package org.matsim.facilities.algorithms;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class FacilitiesCalcMinDist {

	public FacilitiesCalcMinDist() {
		super();
	}

	public void run(ActivityFacilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		double min_dist = Double.MAX_VALUE;

		for (ActivityFacility f : facilities.getFacilities().values()) {
			Coord c = f.getCoord();
			System.out.println("      Facility id = " + f.getId());

			for (ActivityFacility f2 : facilities.getFacilities().values()) {
				if (!f2.equals(f)) {
					Coord c2 = f2.getCoord();
					double dist = CoordUtils.calcEuclideanDistance(c2, c);
					if (dist < min_dist) { min_dist = dist; }
					if (dist == 0.0) {
						System.out.println("      dist=0! fid=" + f.getId() + " <=> f2id=" + f2.getId());
					}
				}
			}
		}
		System.out.println("      min_dist = " + min_dist);
		System.out.println("    done.");
	}
}
