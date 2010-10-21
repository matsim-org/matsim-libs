/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetPrimLoc.java
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
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

import playground.balmermi.census2000.data.Persons;

public class PersonSetPrimLoc extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String E = "e";
	private static final String W = "w";
	private static final String EDUCATION = "education";
	private static final String WORK = "work";

	private final ActivityFacilitiesImpl facilities;
	private final Persons persons;
	private final Matrices matrices;
	private final TreeMap<Id, ArrayList<ActivityFacilityImpl>> zone_work_fac_mapping = new TreeMap<Id, ArrayList<ActivityFacilityImpl>>();
	private final TreeMap<Id, ArrayList<ActivityFacilityImpl>> zone_educ_fac_mapping = new TreeMap<Id, ArrayList<ActivityFacilityImpl>>();

	private QuadTree<ActivityFacilityImpl> workFacQuadTree = null;
	private QuadTree<ActivityFacilityImpl> educFacQuadTree = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetPrimLoc(final ActivityFacilitiesImpl facilities, final Matrices matrices, final Persons persons, final ZoneLayer municipalities) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		this.persons = persons;
		this.matrices = matrices;
		this.buildWorkFacQuadTree();
		this.buildEducFacQuadTree();
		this.buildZoneFacilityMapping(municipalities);
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// build methods
	//////////////////////////////////////////////////////////////////////

	private void buildWorkFacQuadTree() {
		Gbl.startMeasurement();
		System.out.println("      building work facility quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
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
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.workFacQuadTree = new QuadTree<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(WORK) != null) {
				this.workFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacilityImpl) f);
			}
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
	}

	private void buildEducFacQuadTree() {
		Gbl.startMeasurement();
		System.out.println("      building education facility quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
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
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.educFacQuadTree = new QuadTree<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(EDUCATION) != null) {
				this.educFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacilityImpl) f);
			}
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
	}

	public final void buildZoneFacilityMapping(ZoneLayer layer) {
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			ArrayList<Zone> zones = new ArrayList<Zone>();
			Iterator<? extends BasicLocation> z_it = layer.getLocations().values().iterator();
			while (z_it.hasNext()) {
				Zone z = (Zone)z_it.next();
				if (z.contains(f.getCoord())) { zones.add(z); }
			}

			if (zones.isEmpty()) {
				System.out.println("      No zone found for facility id=" + f.getId() + "...");
				z_it = layer.getLocations().values().iterator();
				zones.add((Zone)layer.getNearestLocations(f.getCoord(),null).get(0));
				System.out.println("      Therefore added a neighbor zone id=" + zones.get(0).getId());
			}

			Zone z = zones.get(MatsimRandom.getRandom().nextInt(zones.size()));
			if (f.getActivityOptions().get(WORK) != null) {
				ArrayList<ActivityFacilityImpl> facs = this.zone_work_fac_mapping.get(z.getId());
				if (facs == null) { facs = new ArrayList<ActivityFacilityImpl>(); }
				facs.add((ActivityFacilityImpl) f);
				this.zone_work_fac_mapping.put(z.getId(),facs);
			}
			if (f.getActivityOptions().get(EDUCATION) != null) {
				ArrayList<ActivityFacilityImpl> facs = this.zone_educ_fac_mapping.get(z.getId());
				if (facs == null) { facs = new ArrayList<ActivityFacilityImpl>(); }
				facs.add((ActivityFacilityImpl) f);
				this.zone_educ_fac_mapping.put(z.getId(),facs);
			}
		}

		System.out.println("      Zone to work-facility mapping:");
		Iterator<Id> z_it = this.zone_work_fac_mapping.keySet().iterator();
		while (z_it.hasNext()) {
			Zone z = (Zone)layer.getLocation(z_it.next());
			ArrayList<ActivityFacilityImpl> facs = this.zone_work_fac_mapping.get(z.getId());
			System.out.println("        Zone id=" + z.getId() + " ==> #facs=" + facs.size());
		}
		System.out.println("      Zone to educ-facility mapping:");
		z_it = this.zone_educ_fac_mapping.keySet().iterator();
		while (z_it.hasNext()) {
			Zone z = (Zone)layer.getLocation(z_it.next());
			ArrayList<ActivityFacilityImpl> facs = this.zone_educ_fac_mapping.get(z.getId());
			System.out.println("        Zone id=" + z.getId() + " ==> #facs=" + facs.size());
		}
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final Zone getPrimActZone(final ArrayList<Entry> from_loc_entries) {
		if (from_loc_entries.isEmpty()) { Gbl.errorMsg("From Loc Entries are empty! This should not happen!"); }

		int[] dist_sum = new int[from_loc_entries.size()];
		dist_sum[0] = (int) from_loc_entries.get(0).getValue();
		int n = from_loc_entries.size();
		for (int i=1; i<n; i++) {
			int val = (int) from_loc_entries.get(i).getValue();
			dist_sum[i] = dist_sum[i-1] + val;
		}
		int r = MatsimRandom.getRandom().nextInt(dist_sum[n-1]);
		for (int i=0; i<n; i++) {
			if (r < dist_sum[i]) {
				throw new RuntimeException("code no longer works!");
//				return (Zone)from_loc_entries.get(i).getToLocation(); // this returns now an Id which cannot be casted to Zone
			}
		}
		Gbl.errorMsg("It should never reach this line!");
		return null;
	}

	private final ActivityFacilityImpl getPrimActFacility(final ArrayList<ActivityFacilityImpl> facs, final String act_type) {
		if (facs.isEmpty()) { Gbl.errorMsg("facs are empty! This should not happen!"); }

		int[] dist_sum = new int[facs.size()];
		ActivityOptionImpl activityOption = (ActivityOptionImpl) facs.get(0).getActivityOptions().get(act_type);
		dist_sum[0] = activityOption.getCapacity().intValue();
		if ((dist_sum[0] <= 0) || (dist_sum[0] == Integer.MAX_VALUE)) {
			dist_sum[0] = 1;
			activityOption.setCapacity((double) 1);
		}
		int n = facs.size();
		for (int i=1; i<n; i++) {
			ActivityOptionImpl activityOption2 = (ActivityOptionImpl) facs.get(i).getActivityOptions().get(act_type);
			int val = activityOption2.getCapacity().intValue();
			if ((val <= 0) || (val == Integer.MAX_VALUE)) {
				val = 1;
				activityOption2.setCapacity((double) 1);
			}
			dist_sum[i] = dist_sum[i-1] + val;
		}
		int r = MatsimRandom.getRandom().nextInt(dist_sum[n-1]);
		for (int i=0; i<n; i++) {
			if (r < dist_sum[i]) {
				return facs.get(i);
			}
		}
		Gbl.errorMsg("It should never reach this line!");
		return null;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		ArrayList<ActivityImpl> work_list = new ArrayList<ActivityImpl>();
		ArrayList<ActivityImpl> educ_list = new ArrayList<ActivityImpl>();
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				if (W.equals(act.getType())) {
					work_list.add(act);
				} else if (E.equals(act.getType())) {
					educ_list.add(act);
				}
			}
		}

		if (!work_list.isEmpty()) {
			Integer p_id = Integer.valueOf(person.getId().toString());
			Zone home_zone = this.persons.getPerson(p_id).getHousehold().getMunicipality().getZone();
			Zone to_zone = this.getPrimActZone(this.matrices.getMatrix(WORK).getFromLocEntries(home_zone.getId()));
			ArrayList<ActivityFacilityImpl> to_facs = this.zone_work_fac_mapping.get(to_zone.getId());
			ActivityFacilityImpl to_fac = null;
			if (to_facs == null) {
				System.out.println("      Person id=" + person.getId() + ": no work fac in to_zone id=" +
				                   to_zone.getId() + ". Getting a close one...");
				to_fac = this.workFacQuadTree.get(to_zone.getCoord().getX(),to_zone.getCoord().getY());
				System.out.println("      done. (to_fac id=" + to_fac.getId() + ")");
			}
			else {
				to_fac = this.getPrimActFacility(to_facs,WORK);
			}
			CoordImpl coord = (CoordImpl)to_fac.getCoord();
			for (int i= 0; i<work_list.size(); i++) {
				ActivityImpl a = work_list.get(i);
				a.setCoord(coord);
			}
		}
		else if (!educ_list.isEmpty()) {
			Integer p_id = Integer.valueOf(person.getId().toString());
			Zone home_zone = this.persons.getPerson(p_id).getHousehold().getMunicipality().getZone();
			Zone to_zone = this.getPrimActZone(this.matrices.getMatrix(EDUCATION).getFromLocEntries(home_zone.getId()));
			ArrayList<ActivityFacilityImpl> to_facs = this.zone_educ_fac_mapping.get(to_zone.getId());

			ActivityFacilityImpl to_fac = null;
			if (to_facs == null) {
				System.out.println("      Person id=" + person.getId() + ": no educ fac in to_zone id=" +
				                   to_zone.getId() + ". Getting a close one...");
				to_fac = this.educFacQuadTree.get(to_zone.getCoord().getX(),to_zone.getCoord().getY());
				System.out.println("      done. (to_fac id=" + to_fac.getId() + ")");
			}
			else {
				to_fac = this.getPrimActFacility(to_facs,EDUCATION);
			}
			CoordImpl coord = (CoordImpl)to_fac.getCoord();
			for (int i= 0; i<educ_list.size(); i++) {
				ActivityImpl a = educ_list.get(i);
				a.setCoord(coord);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Plan plan) {
	}
}

