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

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.utils.geometry.Coord;

public class FacilitiesScenarioCut {

	private final double minX;
	private final double maxX;
	private final double minY;
	private final double maxY;

	public FacilitiesScenarioCut(final Coord min, final Coord max) {
		super();
		this.minX = min.getX();
		this.maxX = max.getX();
		this.minY = min.getY();
		this.maxY = max.getY();
	}

	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " module...");

		TreeSet<Id> fid_set = new TreeSet<Id>();
		Iterator<Id> fid_it = facilities.getFacilities().keySet().iterator();
		while (fid_it.hasNext()) {
			Id fid = fid_it.next();
			Facility f = facilities.getFacilities().get(fid);
			Coord coord = f.getCenter();
			double x = coord.getX();
			double y = coord.getY();
			if (!((x < this.maxX) && (this.minX < x) && (y < this.maxY) && (this.minY < y))) {
				fid_set.add(fid);
			}
		}

		System.out.println("      Number of facilities to be cut = " + fid_set.size() + "...");
		fid_it = fid_set.iterator();
		while (fid_it.hasNext()) {
			Id fid = fid_it.next();
			facilities.getFacilities().remove(fid);
		}

		System.out.println("    done.");
	}
}
