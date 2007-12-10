/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesMakeSample.java
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
import org.matsim.gbl.Gbl;
import org.matsim.utils.identifiers.IdI;

public class FacilitiesMakeSample extends FacilitiesAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final double pct;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesMakeSample(final double pct) {
		super();
		this.pct = pct;
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
			double rnd = Gbl.random.nextDouble();
			if (rnd>pct) {
				fid_set.add(fid);
			}
		}

		long sampleSize=facilities.getFacilities().size()- fid_set.size();
		System.out.println("      Number of facilities in sample = " + sampleSize + "...");
		fid_it = fid_set.iterator();
		while (fid_it.hasNext()) {
			IdI fid = fid_it.next();
			facilities.getFacilities().remove(fid);
		}
		System.out.println("      done.");

		System.out.println("    done.");
	}
}

