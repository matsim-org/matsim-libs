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
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.OpeningTime;

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

	private final void combine(ActivityFacility f,ActivityFacilityImpl f2) {
		System.out.println("      Combining f_id=" + f.getId() + " into f2_id=" + f2.getId());
		Iterator<? extends ActivityOption> a_it = f.getActivityOptions().values().iterator();
		while (a_it.hasNext()) {
			ActivityOptionImpl a = (ActivityOptionImpl) a_it.next();
			if (f2.getActivityOptions().get(a.getType()) == null) {
				ActivityOptionImpl a2 = f2.createAndAddActivityOption(a.getType());
				a2.setCapacity(a.getCapacity());
			}
			else {
				ActivityOptionImpl a2 = (ActivityOptionImpl) f2.getActivityOptions().get(a.getType());
				double cap2 = a2.getCapacity();
				double cap = a.getCapacity();
				if ((cap < Integer.MAX_VALUE) && (cap2 < Integer.MAX_VALUE)) { a2.setCapacity(cap + cap2); }
				else { a2.setCapacity(Integer.MAX_VALUE); }
			}
			SortedSet<OpeningTime> ts = a.getOpeningTimes();
			Iterator<OpeningTime> o_it = ts.iterator();
			while (o_it.hasNext()) {
				OpeningTime o = o_it.next();
				f2.getActivityOptions().get(a.getType()).addOpeningTime(o);
			}
		}
		if (Integer.parseInt(f2.getId().toString()) > Integer.parseInt(f.getId().toString())) {
			System.out.println("      => assigning f_id="+f.getId()+" to f2.");
			throw new RuntimeException("Can't set ids anymore.");
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final ActivityFacilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");
		System.out.println("      # facilities = " + facilities.getFacilities().size());

		// TreeMap<XCOORD,TreeMap<YCOORD,FACILITY>>
		TreeMap<Double,TreeMap<Double,ActivityFacilityImpl>> facs = new TreeMap<Double, TreeMap<Double,ActivityFacilityImpl>>();

		for (ActivityFacility f : facilities.getFacilities().values()) {
			Double x = f.getCoord().getX();
			Double y = f.getCoord().getY();
			if (facs.containsKey(x)) { // same x coord
				TreeMap<Double,ActivityFacilityImpl> tree = facs.get(x);
				if (tree.containsKey(y)) { // and same y coord
					ActivityFacilityImpl f2 = tree.get(y);
					this.combine(f,f2);
				}
				else { // not the same y coord
					tree.put(y,(ActivityFacilityImpl) f);
				}
			}
			else { // not the same x coord
				TreeMap<Double,ActivityFacilityImpl> tree = new TreeMap<Double, ActivityFacilityImpl>();
				tree.put(y,(ActivityFacilityImpl) f);
				facs.put(x,tree);
			}
		}

		Map<Id<ActivityFacility>, ? extends ActivityFacility> fs = facilities.getFacilities();
		fs.clear();

		Iterator<TreeMap<Double,ActivityFacilityImpl>> t_it = facs.values().iterator();
		while (t_it.hasNext()) {
			TreeMap<Double,ActivityFacilityImpl> t = t_it.next();
			Iterator<ActivityFacilityImpl> ff_it = t.values().iterator();
			while (ff_it.hasNext()) {
				ActivityFacilityImpl ff = ff_it.next();
				facilities.addActivityFacility(ff);
			}
		}

		System.out.println("      # facilities = " + facilities.getFacilities().size());
		System.out.println("    done.");
	}
}
