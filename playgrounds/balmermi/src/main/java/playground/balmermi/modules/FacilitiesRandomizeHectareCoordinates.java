/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesRandomizeHectareCoordinates.java
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

package playground.balmermi.modules;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.MatsimRandom;

public class FacilitiesRandomizeHectareCoordinates {
	
	public FacilitiesRandomizeHectareCoordinates() {
		System.out.println("init " + this.getClass().getName() + " module...");
		System.out.println("done.");
	}

	public void run(ActivityFacilitiesImpl facilities) {
		System.out.println("running " + this.getClass().getName() + " module...");

		int cnt = 0;
		
		for (ActivityFacilityImpl f : facilities.getFacilities().values()) {
			Coord coord = f.getCoord();
			int x = (int)coord.getX();
			int y = (int)coord.getY();
			if ((x % 100 == 0) && (y % 100 == 0)) {
				coord.setX(x+MatsimRandom.getRandom().nextInt(100));
				coord.setY(y+MatsimRandom.getRandom().nextInt(100));
				cnt++;
			}
		}
		System.out.println("=> # facilities:            "+facilities.getFacilities().size());
		System.out.println("=> # coordinates changed:   "+cnt);
		System.out.println("=> # coordinates unchanged: "+(facilities.getFacilities().size()-cnt));
		System.out.println("done.");
	}
}
