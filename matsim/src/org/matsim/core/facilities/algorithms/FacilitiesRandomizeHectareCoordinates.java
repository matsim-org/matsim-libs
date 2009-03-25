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

package org.matsim.core.facilities.algorithms;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.gbl.MatsimRandom;

public class FacilitiesRandomizeHectareCoordinates {

	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		for (Facility f : facilities.getFacilities().values()) {
			Coord coord = f.getCoord();
			coord.setX((Double.valueOf(coord.getX()).intValue() / 100) * 100 + MatsimRandom.getRandom().nextInt(99));
			coord.setY((Double.valueOf(coord.getY()).intValue() / 100) * 100 + MatsimRandom.getRandom().nextInt(99));
		}

		System.out.println("    done.");
	}

}
