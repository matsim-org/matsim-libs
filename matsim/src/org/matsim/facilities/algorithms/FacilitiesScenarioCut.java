/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesScenarioCut.java
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
import java.util.TreeSet;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;

public class FacilitiesScenarioCut extends FacilitiesAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final double minX;
	private final double maxX;
	private final double minY;
	private final double maxY;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesScenarioCut(final CoordI min, final CoordI max) {
		super();
		this.minX = min.getX();
		this.maxX = max.getX();
		this.minY = min.getY();
		this.maxY = max.getY();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " module...");

		TreeSet<IdI> fid_set = new TreeSet<IdI>();
		Iterator<IdI> fid_it = facilities.getFacilities().keySet().iterator();
		while (fid_it.hasNext()) {
			IdI fid = fid_it.next();
			Facility f = facilities.getFacilities().get(fid);
			CoordI coord = f.getCenter();
			double x = coord.getX();
			double y = coord.getY();
			if (!((x < this.maxX) && (this.minX < x) && (y < this.maxY) && (this.minY < y))) {
				fid_set.add(fid);
			}
		}

		System.out.println("      Number of facilities to be cut = " + fid_set.size() + "...");
		fid_it = fid_set.iterator();
		while (fid_it.hasNext()) {
			IdI fid = fid_it.next();
			facilities.getFacilities().remove(fid);
		}
		System.out.println("      done.");

		System.out.println("    done.");
	}
}
