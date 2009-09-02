/* *********************************************************************** *
 * project: org.matsim.*
 * MatricesValidateWithFacilities.java
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

package org.matsim.matrices.algorithms;

import java.util.Iterator;

import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.world.Layer;
import org.matsim.world.MappedLocation;
import org.matsim.world.World;

public class MatricesValidateWithFacilities {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final ActivityFacilitiesImpl facilities;
	private final World world;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public MatricesValidateWithFacilities(final ActivityFacilitiesImpl facilities, final World world) {
		super();
		this.facilities = facilities;
		this.world = world;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final boolean hasFacility(final MappedLocation loc, final String act_type) {
		if (loc.getDownMapping().isEmpty()) { return false; }
		Iterator<MappedLocation> dl_it = loc.getDownMapping().values().iterator();
		while (dl_it.hasNext()) {
			MappedLocation dl = dl_it.next();
			ActivityFacilityImpl f = this.facilities.getFacilities().get(dl.getId());
			if (f == null) { 
				throw new RuntimeException("SOMETHING IS WRONG!!!");
			}
			if (f.getActivityOptions().containsKey(act_type)) {
				return true;
			}
		}
		return false;
	}

	private final MappedLocation findNearestLocation(final MappedLocation location, final String act_type) {
		ActivityFacilityImpl nearest_facility = null;
		double distance = Double.MAX_VALUE;
		for (ActivityFacilityImpl f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().containsKey(act_type)) {
				double d = location.calcDistance(f.getCoord());
				if (d < distance) {
					distance = d;
					nearest_facility = f;
				}
			}
		}
		if (nearest_facility == null) {
			throw new RuntimeException("No facility with act_type = " + act_type + "exists");
		}
		MappedLocation dl = this.world.getLayer(ActivityFacilitiesImpl.LAYER_TYPE).getLocation(nearest_facility.getId());
		if (dl.getUpMapping().size() != 1) {
			Gbl.errorMsg("down location id=" + dl.getId() + " has " + dl.getUpMapping().size() + "up mappings");
		}
		MappedLocation nearest_location = dl.getUpMapping().get(dl.getUpMapping().firstKey());
		return nearest_location;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(final Matrices matrices) {

		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		Iterator<Matrix> m_it = matrices.getMatrices().values().iterator();
		while (m_it.hasNext()) {
			Matrix m = m_it.next();
			String act_type = m.getId();
			Layer m_l = m.getLayer();

			System.out.println("      checking matrix " + m.toString() + "...");

			System.out.println("        finding locs without any downmapping and remove all from- and to-trips...");
			Iterator<? extends MappedLocation> loc_it = m_l.getLocations().values().iterator();
			while (loc_it.hasNext()) {
				MappedLocation loc = loc_it.next();
				if (loc.getDownMapping().isEmpty()) {
					System.out.println("          remove from- and to-trips of location id=" + loc.getId());
					m.removeFromLocEntries(loc);
					m.removeToLocEntries(loc);
				}
			}
			System.out.println("        done.");

			System.out.println("        remove all trips with no 'home' facility at 'from' and no '" + act_type + "' facility at 'to'...");
			Iterator<? extends MappedLocation> from_loc_it = m_l.getLocations().values().iterator();
			while (from_loc_it.hasNext()) {
				MappedLocation from_loc = from_loc_it.next();
				Iterator<? extends MappedLocation> to_loc_it = m_l.getLocations().values().iterator();
				while (to_loc_it.hasNext()) {
					MappedLocation to_loc = to_loc_it.next();

					Entry e = m.getEntry(from_loc,to_loc);
					if (e != null) {
						if (!this.hasFacility(from_loc,"home") || !this.hasFacility(to_loc,act_type)) {
							m.removeEntry(e);
						}
					}
				}
			}
			System.out.println("        done.");

			System.out.println("        adding trips to locations which have no 'from' entries but consists of 'home' facilities...");
			from_loc_it = m_l.getLocations().values().iterator();
			while (from_loc_it.hasNext()) {
				MappedLocation from_loc = from_loc_it.next();
				if (this.hasFacility(from_loc,"home")) {
					if (m.getFromLocEntries(from_loc) == null) {
						MappedLocation to_loc = this.findNearestLocation(from_loc,act_type);
						Entry e = m.setEntry(from_loc,to_loc, 1);
						System.out.println("          create entry: " + e.toString());
					}
				}
			}
			System.out.println("        done.");

			System.out.println("      done.");
		}

		System.out.println("    done.");
	}
}
