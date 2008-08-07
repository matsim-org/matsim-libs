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

import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.utils.geometry.Coord;

public class FacilitiesCalcMinDist {

	public FacilitiesCalcMinDist() {
		super();
	}

	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		double min_dist = Double.MAX_VALUE;

		for (Facility f : facilities.getFacilities().values()) {
			Coord c = f.getCenter();
			System.out.println("      Facility id = " + f.getId());

			for (Facility f2 : facilities.getFacilities().values()) {
				if (!f2.equals(f)) {
					Coord c2 = f2.getCenter();
					double dist = c2.calcDistance(c);
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
