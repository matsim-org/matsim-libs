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
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.Opentime;
import org.matsim.world.Location;

public class FacilitiesCombine extends FacilitiesAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesCombine() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		// TreeMap<XCOORD,TreeMap<YCOORD,FACILITY>>
		TreeMap<Double,TreeMap<Double,Facility>> facs = new TreeMap<Double, TreeMap<Double,Facility>>();

		Iterator<? extends Location> f_it = facilities.getLocations().values().iterator();
		while (f_it.hasNext()) {
			Facility f = (Facility)f_it.next();
			Double x = f.getCenter().getX();
			Double y = f.getCenter().getY();
			if (facs.containsKey(x)) { // same x coord
				TreeMap<Double,Facility> tree = facs.get(x);
				if (tree.containsKey(y)) { // and same y coord
					Facility f2 = tree.get(y);

					System.out.println("      Combining f_id=" + f.getId() + " into f2_id=" + f2.getId());
					Iterator<Activity> a_it = f.getActivities().values().iterator();
					while (a_it.hasNext()) {
						Activity a = a_it.next();
						if (f2.getActivity(a.getType()) == null) {
							Activity a2 = f2.createActivity(a.getType());
							a2.setCapacity(a.getCapacity());
						}
						else {
							Activity a2 = f2.getActivity(a.getType());
							int cap2 = a2.getCapacity();
							int cap = a.getCapacity();
							if ((cap < Integer.MAX_VALUE) && (cap2 < Integer.MAX_VALUE)) { a2.setCapacity(cap + cap2); }
							else { a2.setCapacity(Integer.MAX_VALUE); }
						}
						Iterator<TreeSet<Opentime>> ts_it = a.getOpentimes().values().iterator();
						while (ts_it.hasNext()) {
							TreeSet<Opentime> ts = ts_it.next();
							Iterator<Opentime> o_it = ts.iterator();
							while (o_it.hasNext()) {
								Opentime o = o_it.next();
								f2.getActivity(a.getType()).addOpentime(o);
							}
						}
					}
				}
				else { // not the same y coord
					tree.put(y,f);
				}
			}
			else { // not the same x coord
				TreeMap<Double,Facility> tree = new TreeMap<Double, Facility>();
				tree.put(y,f);
				facs.put(x,tree);
			}
		}

		TreeMap<Id, Facility> fs = (TreeMap<Id, Facility>) facilities.getFacilities();
		fs.clear();

		Iterator<TreeMap<Double,Facility>> t_it = facs.values().iterator();
		while (t_it.hasNext()) {
			TreeMap<Double,Facility> t = t_it.next();
			Iterator<Facility> ff_it = t.values().iterator();
			while (ff_it.hasNext()) {
				Facility ff = ff_it.next();
				fs.put(ff.getId(),ff);
			}
		}

		System.out.println("    done.");
	}
}
