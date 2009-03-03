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

package org.matsim.facilities.algorithms;

import org.matsim.facilities.Facilities;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Facility;

public class FacilitiesRandomizeHectareCoordinates {

	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		for (Facility f : facilities.getFacilities().values()) {
			Coord coord = f.getCenter();
			coord.setX((Double.valueOf(coord.getX()).intValue() / 100) * 100 + MatsimRandom.random.nextInt(99));
			coord.setY((Double.valueOf(coord.getY()).intValue() / 100) * 100 + MatsimRandom.random.nextInt(99));
		}

		System.out.println("    done.");
	}

}
