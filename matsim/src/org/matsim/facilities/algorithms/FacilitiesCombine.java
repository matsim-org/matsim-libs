/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesCombine.java
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
import java.util.SortedSet;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.OpeningTime;

public class FacilitiesCombine {

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesCombine() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void combine(ActivityFacilityImpl f,ActivityFacilityImpl f2) {
		System.out.println("      Combining f_id=" + f.getId() + " into f2_id=" + f2.getId());
		Iterator<ActivityOptionImpl> a_it = f.getActivityOptions().values().iterator();
		while (a_it.hasNext()) {
			ActivityOptionImpl a = a_it.next();
			if (f2.getActivityOption(a.getType()) == null) {
				ActivityOptionImpl a2 = f2.createActivityOption(a.getType());
				a2.setCapacity(a.getCapacity());
			}
			else {
				ActivityOptionImpl a2 = f2.getActivityOption(a.getType());
				double cap2 = a2.getCapacity();
				double cap = a.getCapacity();
				if ((cap < Integer.MAX_VALUE) && (cap2 < Integer.MAX_VALUE)) { a2.setCapacity(cap + cap2); }
				else { a2.setCapacity(Integer.MAX_VALUE); }
			}
			Iterator<SortedSet<OpeningTime>> ts_it = a.getOpeningTimes().values().iterator();
			while (ts_it.hasNext()) {
				SortedSet<OpeningTime> ts = ts_it.next();
				Iterator<OpeningTime> o_it = ts.iterator();
				while (o_it.hasNext()) {
					OpeningTime o = o_it.next();
					f2.getActivityOption(a.getType()).addOpeningTime(o);
				}
			}
		}
		if (Integer.parseInt(f2.getId().toString()) > Integer.parseInt(f.getId().toString())) {
			System.out.println("      => assigning f_id="+f.getId()+" to f2.");
			f2.setId(f.getId());
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final ActivityFacilitiesImpl facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");
		System.out.println("      # facilities = " + facilities.getFacilities().size());

		// TreeMap<XCOORD,TreeMap<YCOORD,FACILITY>>
		TreeMap<Double,TreeMap<Double,ActivityFacilityImpl>> facs = new TreeMap<Double, TreeMap<Double,ActivityFacilityImpl>>();

		for (ActivityFacilityImpl f : facilities.getFacilities().values()) {
			Double x = f.getCoord().getX();
			Double y = f.getCoord().getY();
			if (facs.containsKey(x)) { // same x coord
				TreeMap<Double,ActivityFacilityImpl> tree = facs.get(x);
				if (tree.containsKey(y)) { // and same y coord
					ActivityFacilityImpl f2 = tree.get(y);
					this.combine(f,f2);
				}
				else { // not the same y coord
					tree.put(y,f);
				}
			}
			else { // not the same x coord
				TreeMap<Double,ActivityFacilityImpl> tree = new TreeMap<Double, ActivityFacilityImpl>();
				tree.put(y,f);
				facs.put(x,tree);
			}
		}

		TreeMap<Id, ActivityFacilityImpl> fs = (TreeMap<Id, ActivityFacilityImpl>)facilities.getFacilities();
		fs.clear();

		Iterator<TreeMap<Double,ActivityFacilityImpl>> t_it = facs.values().iterator();
		while (t_it.hasNext()) {
			TreeMap<Double,ActivityFacilityImpl> t = t_it.next();
			Iterator<ActivityFacilityImpl> ff_it = t.values().iterator();
			while (ff_it.hasNext()) {
				ActivityFacilityImpl ff = ff_it.next();
				fs.put(ff.getId(),ff);
			}
		}

		System.out.println("      # facilities = " + facilities.getFacilities().size());
		System.out.println("    done.");
	}
}
