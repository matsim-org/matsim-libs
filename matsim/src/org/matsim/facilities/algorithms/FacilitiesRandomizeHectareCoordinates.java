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

import java.util.Iterator;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordI;
import org.matsim.world.Location;

public class FacilitiesRandomizeHectareCoordinates extends FacilitiesAlgorithm {

	public FacilitiesRandomizeHectareCoordinates() {

		super();

	}

	@Override
	public void run(Facilities facilities) {

		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		CoordI coord;

		Iterator<Location> f_it = facilities.getLocations().values().iterator();
		while (f_it.hasNext()) {

			Facility f = (Facility)f_it.next();
			coord = f.getCenter();
			coord.setX((Double.valueOf(coord.getX()).intValue() / 100) * 100 + Gbl.random.nextInt(99));
			coord.setY((Double.valueOf(coord.getY()).intValue() / 100) * 100 + Gbl.random.nextInt(99));

		}

		System.out.println("    done.");

	}

}
