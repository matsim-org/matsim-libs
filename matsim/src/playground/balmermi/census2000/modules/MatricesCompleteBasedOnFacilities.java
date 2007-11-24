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

import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.algorithms.MatricesAlgorithm;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.misc.QuadTree;
import org.matsim.world.Location;
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
public class MatricesCompleteBasedOnFacilities extends MatricesAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String EDUCATION = "education";
	private static final String WORK = "work";

	private final Facilities facilities;
	private final ZoneLayer layer;

	/**
	 * The mapping between each <i>facility</i> id and the <i>zone</i> it belongs to.
	 */
	private final HashMap<IdI,Zone> fac_zone_map = new HashMap<IdI,Zone>();

	/**
	 * The mapping between each <i>zone</i> id and the <i>facilities</i> which belongs to that zone.
	 */
	private final HashMap<IdI, ArrayList<Facility>> zone_fac_map = new HashMap<IdI, ArrayList<Facility>>();

	/**
	 * Fast access data structure for all <i>facilities</i> containing <code>work</code> <i>activities</i>.
	 */
	private QuadTree<Facility> workFacQuadTree = null;

	/**
	 * Fast access data structure for all <i>facilities</i> containing <code>education</code> <i>activities</i>.
	 */
	private QuadTree<Facility> educFacQuadTree = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public MatricesCompleteBasedOnFacilities(Facilities facilities, ZoneLayer layer) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		this.layer = layer;
		this.initMaps();
		Gbl.random.nextInt();
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
		for (Facility f : facilities.getFacilities().values()) {
			ArrayList<Zone> zones = new ArrayList<Zone>();
			Iterator<? extends Location> z_it = this.layer.getLocations().values().iterator();
			while (z_it.hasNext()) {
				Zone z = (Zone)z_it.next();
				if (z.contains(f.getCenter())) { zones.add(z); }
			}
			if (zones.isEmpty()) {
				ArrayList<Location> zs = this.layer.getNearestLocations(f.getCenter(),null);
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
			ArrayList<Facility> fs = this.zone_fac_map.get(zone.getId());
			if (fs == null) { fs = new ArrayList<Facility>(); }
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
		Iterator<IdI> zid_it = this.zone_fac_map.keySet().iterator();
		while (zid_it.hasNext()) {
			IdI zid = zid_it.next();
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
	private final Zone selectZone(ArrayList<Zone> zones) {
		int[] f_cnt = new int[zones.size()];
		for (int i=0; i<zones.size(); i++) {
			Zone z = zones.get(i);
			ArrayList<Facility> facs = this.zone_fac_map.get(z.getId());
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
		Iterator<? extends Location> f_it = this.facilities.getLocations().values().iterator();
		while (f_it.hasNext()) {
			Facility f = (Facility)f_it.next();
			if (f.getActivity(WORK) != null) {
				if (f.getCenter().getX() < minx) { minx = f.getCenter().getX(); }
				if (f.getCenter().getY() < miny) { miny = f.getCenter().getY(); }
				if (f.getCenter().getX() > maxx) { maxx = f.getCenter().getX(); }
				if (f.getCenter().getY() > maxy) { maxy = f.getCenter().getY(); }
			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("building quad tree: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.workFacQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		f_it = this.facilities.getLocations().values().iterator();
		while (f_it.hasNext()) {
			Facility f = (Facility)f_it.next();
			if (f.getActivity(WORK) != null) {
				this.workFacQuadTree.put(f.getCenter().getX(),f.getCenter().getY(),f);
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
		Iterator<? extends Location> f_it = this.facilities.getLocations().values().iterator();
		while (f_it.hasNext()) {
			Facility f = (Facility)f_it.next();
			if (f.getActivity(EDUCATION) != null) {
				if (f.getCenter().getX() < minx) { minx = f.getCenter().getX(); }
				if (f.getCenter().getY() < miny) { miny = f.getCenter().getY(); }
				if (f.getCenter().getX() > maxx) { maxx = f.getCenter().getX(); }
				if (f.getCenter().getY() > maxy) { maxy = f.getCenter().getY(); }
			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("building quad tree: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.educFacQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		f_it = this.facilities.getLocations().values().iterator();
		while (f_it.hasNext()) {
			Facility f = (Facility)f_it.next();
			if (f.getActivity(EDUCATION) != null) {
				this.educFacQuadTree.put(f.getCenter().getX(),f.getCenter().getY(),f);
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

	@Override
	public void run(Matrices matrices) {
		System.out.println("    running " + this.getClass().getName() + " module...");

		Matrix<String> work_m = matrices.getMatrix(WORK);
		Matrix<String> educ_m = matrices.getMatrix(EDUCATION);

		// removing entries ending at zone z
		Iterator<? extends Location> z_it = this.layer.getLocations().values().iterator();
		while (z_it.hasNext()) {
			Zone z = (Zone)z_it.next();

			if (!this.zone_fac_map.containsKey(z.getId())) {
				System.out.println("      zone id=" + z.getId() + ": removing all WORK and EDUC entries ending at this zone:");
				work_m.removeToLocEntries(z);
				educ_m.removeToLocEntries(z);
			}
			else {
				ArrayList<Facility> facs = this.zone_fac_map.get(z.getId());
				boolean has_work = false;
				boolean has_educ = false;
				for (int i=0; i<facs.size(); i++) {
					Facility f = facs.get(i);
					if (f.getActivity(WORK) != null) { has_work = true; }
					if (f.getActivity(EDUCATION) != null) { has_educ = true; }
				}
				if (!has_work) {
					System.out.println("      zone id=" + z.getId() + ": removing all WORK entries ending at this zone:");
					work_m.removeToLocEntries(z);
				}
				if (!has_educ) {
					System.out.println("      zone id=" + z.getId() + ": removing all EDUC entries ending at this zone:");
					educ_m.removeToLocEntries(z);
				}
			}
		}

		// adding entries starting at zone z
		z_it = this.layer.getLocations().values().iterator();
		while (z_it.hasNext()) {
			Zone z = (Zone)z_it.next();

			if (!work_m.getFromLocations().containsKey(z.getId())) {
				Facility f = this.workFacQuadTree.get(z.getCenter().getX(),z.getCenter().getY());
				Zone to_zone = this.fac_zone_map.get(f.getId());
				work_m.setEntry(z,to_zone,"1");
				System.out.println("      zone id=" + z.getId() + ": added a WORK entry to zone id=" + to_zone.getId() + " (fac id=" + f.getId() + ").");
			}
			if (!educ_m.getFromLocations().containsKey(z.getId())) {
				Facility f = this.educFacQuadTree.get(z.getCenter().getX(),z.getCenter().getY());
				Zone to_zone = this.fac_zone_map.get(f.getId());
				educ_m.setEntry(z,to_zone,"1");
				System.out.println("      zone id=" + z.getId() + ": added a EDUC entry to zone id=" + to_zone.getId() + " (fac id=" + f.getId() + ").");
			}
		}

		System.out.println("    done.");
	}




}
