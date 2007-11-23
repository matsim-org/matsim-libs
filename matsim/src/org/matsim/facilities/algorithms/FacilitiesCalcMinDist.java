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

import java.util.Iterator;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.utils.geometry.CoordI;
import org.matsim.world.Location;

public class FacilitiesCalcMinDist extends FacilitiesAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesCalcMinDist() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");
		
		double min_dist = Double.MAX_VALUE;
		
		Iterator<Location> f_it = facilities.getLocations().values().iterator();
		while (f_it.hasNext()) {
			Facility f = (Facility)f_it.next();
			CoordI c = f.getCenter();
			System.out.println("      Facility id = " + f.getId());
			
			Iterator<Location> f2_it = facilities.getLocations().values().iterator();
			while (f2_it.hasNext()) {
				Facility f2 = (Facility)f2_it.next();
				if (!f2.equals(f)) {
					CoordI c2 = f2.getCenter();
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
