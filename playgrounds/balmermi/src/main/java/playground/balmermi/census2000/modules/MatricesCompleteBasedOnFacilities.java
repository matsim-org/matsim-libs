/* *********************************************************************** *
 * project: org.matsim.*
 * MatricesCompleteBasedOnFacilities.java
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

package playground.balmermi.census2000.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.world.MappedLocation;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

/**
 * <p>
 * <b>MATSim-FUSION Module</b>
 * </p>
 *
 * <p>
 * Corrects entries of the given matrices such that:
 * (i) no work entry ends in a zone with no work facility (such entries will be removed),
 * (ii) no education entry ends in a zone with no education facility (such entries will be removed) and
 * (iii) zones with no starting work or education entries will get a new entry with a target zone
 * containing the nearest work (education resp.) facility from the center of the start zone (typically
 * the same zone).
 * </p>
 * <p>
 * Log messages:<br>
 * log info line for each removed and added entry.
 * </p>
 *
 * @author Michael Balmer
 */
public class MatricesCompleteBasedOnFacilities {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String EDUCATION = "education";
	private static final String WORK = "work";

	private final ActivityFacilitiesImpl facilities;
	private final ZoneLayer layer;

	/**
	 * The mapping between each <i>facility</i> id and the <i>zone</i> it belongs to.
	 */
	private final HashMap<Id,Zone> fac_zone_map = new HashMap<Id,Zone>();

	/**
	 * The mapping between each <i>zone</i> id and the <i>facilities</i> which belongs to that zone.
	 */
	private final HashMap<Id, ArrayList<ActivityFacilityImpl>> zone_fac_map = new HashMap<Id, ArrayList<ActivityFacilityImpl>>();

	/**
	 * Fast access data structure for all <i>facilities</i> containing <code>work</code> <i>activities</i>.
	 */
	private QuadTree<ActivityFacilityImpl> workFacQuadTree = null;

	/**
	 * Fast access data structure for all <i>facilities</i> containing <code>education</code> <i>activities</i>.
	 */
	private QuadTree<ActivityFacilityImpl> educFacQuadTree = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public MatricesCompleteBasedOnFacilities(final ActivityFacilitiesImpl facilities, final ZoneLayer layer) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		this.layer = layer;
		this.initMaps();
		MatsimRandom.getRandom().nextInt();
		this.buildWorkFacQuadTree();
		this.buildEducFacQuadTree();
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// init methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * creates the two mapping data structures (<code>fac_zone_map</code> and <code>zone_fac_map</code>).
	 */
	private final void initMaps() {
		System.out.println("      init fac<=>zone mapping...");
		int f_cnt = 0;
		for (ActivityFacilityImpl f : this.facilities.getFacilities().values()) {
			ArrayList<Zone> zones = new ArrayList<Zone>();
			Iterator<? extends MappedLocation> z_it = this.layer.getLocations().values().iterator();
			while (z_it.hasNext()) {
				Zone z = (Zone)z_it.next();
				if (z.contains(f.getCoord())) { zones.add(z); }
			}
			if (zones.isEmpty()) {
				ArrayList<MappedLocation> zs = this.layer.getNearestLocations(f.getCoord(),null);
				for (int i=0; i<zs.size(); i++) {
					zones.add((Zone)zs.get(i));
				}
			}
			if (zones.isEmpty()) { Gbl.errorMsg("Fac id=" + f.getId() + ": no zone found!"); }

			Zone zone = this.selectZone(zones);

			// adding fac => zone mapping
			if (this.fac_zone_map.put(f.getId(),zone) != null) {
				Gbl.errorMsg("Fac id=" + f.getId() + " is already added to the map!");
			}
			// adding zone => fac mapping
			ArrayList<ActivityFacilityImpl> fs = this.zone_fac_map.get(zone.getId());
			if (fs == null) { fs = new ArrayList<ActivityFacilityImpl>(); }
			fs.add(f);
			this.zone_fac_map.put(zone.getId(),fs);

			// progress report
			f_cnt++;
			if (f_cnt % 50000 == 0) { System.out.println("        facilitiy # " + f_cnt); }
		}

		// print some log infos
		System.out.println("        fac_zone_map size =" + this.fac_zone_map.size());
		System.out.println("        zone_fac_map size =" + this.zone_fac_map.size());
		System.out.println("        Zone to Fac mapping:...");
		Iterator<Id> zid_it = this.zone_fac_map.keySet().iterator();
		while (zid_it.hasNext()) {
			Id zid = zid_it.next();
			System.out.println("          Zone id=" + zid + ": #Facs=" + this.zone_fac_map.get(zid).size());
		}
		System.out.println("        done.");

		System.out.println("      done.");
	}

	//////////////////////////////////////////////////////////////////////

	/**
	 * returns the zone of the <code>zones</code> list with the lowest number
	 * of facilities. This method is only used by {@link #initMaps()} to distribute
	 * facilities belonging to more than one possible zone such that the final
	 * distribution is uniform.
	 *
	 * @param zones
	 * @return The zone of the <code>zones</code> list with the lowest number
	 * of facilities.
	 */
	private final Zone selectZone(final ArrayList<Zone> zones) {
		int[] f_cnt = new int[zones.size()];
		for (int i=0; i<zones.size(); i++) {
			Zone z = zones.get(i);
			ArrayList<ActivityFacilityImpl> facs = this.zone_fac_map.get(z.getId());
			if (facs == null) { f_cnt[i] = 0; }
			else { f_cnt[i] = facs.size(); }
		}
		int index = -1;
		int min = Integer.MAX_VALUE;
		for (int i=0; i<f_cnt.length; i++) {
			if (min > f_cnt[i]) {
				min = f_cnt[i];
				index = i;
			}
		}
		return zones.get(index);
	}

	//////////////////////////////////////////////////////////////////////

	private void buildWorkFacQuadTree() {
		Gbl.startMeasurement();
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ActivityFacilityImpl f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(WORK) != null) {
				if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
				if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
				if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
				if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("building quad tree: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.workFacQuadTree = new QuadTree<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		for (ActivityFacilityImpl f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(WORK) != null) {
				this.workFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
			}
		}
		Gbl.printRoundTime();
	}

	//////////////////////////////////////////////////////////////////////

	private void buildEducFacQuadTree() {
		Gbl.startMeasurement();
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ActivityFacilityImpl f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(EDUCATION) != null) {
				if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
				if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
				if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
				if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("building quad tree: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.educFacQuadTree = new QuadTree<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		for (ActivityFacilityImpl f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(EDUCATION) != null) {
				this.educFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
			}
		}
		Gbl.printRoundTime();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(final Matrices matrices) {
		System.out.println("    running " + this.getClass().getName() + " module...");

		Matrix work_m = matrices.getMatrix(WORK);
		Matrix educ_m = matrices.getMatrix(EDUCATION);

		// removing entries ending at zone z
		Iterator<? extends MappedLocation> z_it = this.layer.getLocations().values().iterator();
		while (z_it.hasNext()) {
			Zone z = (Zone)z_it.next();

			if (!this.zone_fac_map.containsKey(z.getId())) {
				System.out.println("      zone id=" + z.getId() + ": removing all WORK and EDUC entries ending at this zone:");
				work_m.removeToLocEntries(z.getId());
				educ_m.removeToLocEntries(z.getId());
			}
			else {
				ArrayList<ActivityFacilityImpl> facs = this.zone_fac_map.get(z.getId());
				boolean has_work = false;
				boolean has_educ = false;
				for (int i=0; i<facs.size(); i++) {
					ActivityFacilityImpl f = facs.get(i);
					if (f.getActivityOptions().get(WORK) != null) { has_work = true; }
					if (f.getActivityOptions().get(EDUCATION) != null) { has_educ = true; }
				}
				if (!has_work) {
					System.out.println("      zone id=" + z.getId() + ": removing all WORK entries ending at this zone:");
					work_m.removeToLocEntries(z.getId());
				}
				if (!has_educ) {
					System.out.println("      zone id=" + z.getId() + ": removing all EDUC entries ending at this zone:");
					educ_m.removeToLocEntries(z.getId());
				}
			}
		}

		// adding entries starting at zone z
		z_it = this.layer.getLocations().values().iterator();
		while (z_it.hasNext()) {
			Zone z = (Zone)z_it.next();

			if (!work_m.getFromLocations().containsKey(z.getId())) {
				ActivityFacilityImpl f = this.workFacQuadTree.get(z.getCoord().getX(),z.getCoord().getY());
				Zone to_zone = this.fac_zone_map.get(f.getId());
				work_m.setEntry(z.getId(),to_zone.getId(), 1);
				System.out.println("      zone id=" + z.getId() + ": added a WORK entry to zone id=" + to_zone.getId() + " (fac id=" + f.getId() + ").");
			}
			if (!educ_m.getFromLocations().containsKey(z.getId())) {
				ActivityFacilityImpl f = this.educFacQuadTree.get(z.getCoord().getX(),z.getCoord().getY());
				Zone to_zone = this.fac_zone_map.get(f.getId());
				educ_m.setEntry(z.getId(),to_zone.getId(), 1);
				System.out.println("      zone id=" + z.getId() + ": added a EDUC entry to zone id=" + to_zone.getId() + " (fac id=" + f.getId() + ").");
			}
		}

		System.out.println("    done.");
	}




}
