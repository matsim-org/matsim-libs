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

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithmI;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.world.Location;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

import playground.balmermi.census2000.data.Persons;

public class PersonSetPrimLoc extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String E = "e";
	private static final String W = "w";
	private static final Id MUNICIPALITY = new IdImpl("municipality");
	private static final String EDUCATION = "education";
	private static final String WORK = "work";

	private final Facilities facilities;
	private final Persons persons;
	private final Matrices matrices;
	private final TreeMap<Id, ArrayList<Facility>> zone_work_fac_mapping = new TreeMap<Id, ArrayList<Facility>>();
	private final TreeMap<Id, ArrayList<Facility>> zone_educ_fac_mapping = new TreeMap<Id, ArrayList<Facility>>();

	private QuadTree<Facility> workFacQuadTree = null;
	private QuadTree<Facility> educFacQuadTree = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetPrimLoc(final Facilities facilities, final Matrices matrices, final Persons persons) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		this.persons = persons;
		this.matrices = matrices;
		this.buildWorkFacQuadTree();
		this.buildEducFacQuadTree();
		this.buildZoneFacilityMapping();
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
		for (Facility f : this.facilities.getFacilities().values()) {
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
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.workFacQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		for (Facility f : this.facilities.getFacilities().values()) {
			if (f.getActivity(WORK) != null) {
				this.workFacQuadTree.put(f.getCenter().getX(),f.getCenter().getY(),f);
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
		for (Facility f : this.facilities.getFacilities().values()) {
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
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.educFacQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		for (Facility f : this.facilities.getFacilities().values()) {
			if (f.getActivity(EDUCATION) != null) {
				this.educFacQuadTree.put(f.getCenter().getX(),f.getCenter().getY(),f);
			}
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
	}

	public final void buildZoneFacilityMapping() {
		ZoneLayer layer = (ZoneLayer)Gbl.getWorld().getLayer(MUNICIPALITY);
		Iterator<? extends Location> f_it = this.facilities.getLocations().values().iterator();
		while (f_it.hasNext()) {
			Facility f = (Facility)f_it.next();
			ArrayList<Zone> zones = new ArrayList<Zone>();
			Iterator<? extends Location> z_it = layer.getLocations().values().iterator();
			while (z_it.hasNext()) {
				Zone z = (Zone)z_it.next();
				if (z.contains(f.getCenter())) { zones.add(z); }
			}

			if (zones.isEmpty()) {
				System.out.println("      No zone found for facility id=" + f.getId() + "...");
				z_it = layer.getLocations().values().iterator();
				zones.add((Zone)layer.getNearestLocations(f.getCenter(),null).get(0));
				System.out.println("      Therefore added a neighbor zone id=" + zones.get(0).getId());
			}

			Zone z = zones.get(Gbl.random.nextInt(zones.size()));
			if (f.getActivity(WORK) != null) {
				ArrayList<Facility> facs = this.zone_work_fac_mapping.get(z.getId());
				if (facs == null) { facs = new ArrayList<Facility>(); }
				facs.add(f);
				this.zone_work_fac_mapping.put(z.getId(),facs);
			}
			if (f.getActivity(EDUCATION) != null) {
				ArrayList<Facility> facs = this.zone_educ_fac_mapping.get(z.getId());
				if (facs == null) { facs = new ArrayList<Facility>(); }
				facs.add(f);
				this.zone_educ_fac_mapping.put(z.getId(),facs);
			}
		}

		System.out.println("      Zone to work-facility mapping:");
		Iterator<Id> z_it = this.zone_work_fac_mapping.keySet().iterator();
		while (z_it.hasNext()) {
			Zone z = (Zone)layer.getLocation(z_it.next());
			ArrayList<Facility> facs = this.zone_work_fac_mapping.get(z.getId());
			System.out.println("        Zone id=" + z.getId() + " ==> #facs=" + facs.size());
		}
		System.out.println("      Zone to educ-facility mapping:");
		z_it = this.zone_educ_fac_mapping.keySet().iterator();
		while (z_it.hasNext()) {
			Zone z = (Zone)layer.getLocation(z_it.next());
			ArrayList<Facility> facs = this.zone_educ_fac_mapping.get(z.getId());
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
		int r = Gbl.random.nextInt(dist_sum[n-1]);
		for (int i=0; i<n; i++) {
			if (r < dist_sum[i]) {
				return (Zone)from_loc_entries.get(i).getToLocation();
			}
		}
		Gbl.errorMsg("It should never reach this line!");
		return null;
	}

	private final Facility getPrimActFacility(final ArrayList<Facility> facs, final String act_type) {
		if (facs.isEmpty()) { Gbl.errorMsg("facs are empty! This should not happen!"); }

		int[] dist_sum = new int[facs.size()];
		dist_sum[0] = facs.get(0).getActivity(act_type).getCapacity();
		if ((dist_sum[0] <= 0) || (dist_sum[0] == Integer.MAX_VALUE)) {
			dist_sum[0] = 1;
			facs.get(0).getActivity(act_type).setCapacity(1);
		}
		int n = facs.size();
		for (int i=1; i<n; i++) {
			int val = facs.get(i).getActivity(act_type).getCapacity();
			if ((val <= 0) || (val == Integer.MAX_VALUE)) {
				val = 1;
				facs.get(i).getActivity(act_type).setCapacity(1);
			}
			dist_sum[i] = dist_sum[i-1] + val;
		}
		int r = Gbl.random.nextInt(dist_sum[n-1]);
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
		ArrayList<Act> work_list = new ArrayList<Act>();
		ArrayList<Act> educ_list = new ArrayList<Act>();
		Iterator<?> act_it = person.getSelectedPlan().getIteratorAct();
		while (act_it.hasNext()) {
			Act act = (Act)act_it.next();
			if (W.equals(act.getType())) {
				work_list.add(act);
			}
			else if (E.equals(act.getType())) {
				educ_list.add(act);
			}
		}

		if (!work_list.isEmpty()) {
			Integer p_id = Integer.valueOf(person.getId().toString());
			Zone home_zone = this.persons.getPerson(p_id).getHousehold().getMunicipality().getZone();
			Zone to_zone = this.getPrimActZone(this.matrices.getMatrix(WORK).getFromLocEntries(home_zone));
			ArrayList<Facility> to_facs = this.zone_work_fac_mapping.get(to_zone.getId());
			Facility to_fac = null;
			if (to_facs == null) {
				System.out.println("      Person id=" + person.getId() + ": no work fac in to_zone id=" +
				                   to_zone.getId() + ". Getting a close one...");
				to_fac = this.workFacQuadTree.get(to_zone.getCenter().getX(),to_zone.getCenter().getY());
				System.out.println("      done. (to_fac id=" + to_fac.getId() + ")");
			}
			else {
				to_fac = this.getPrimActFacility(to_facs,WORK);
			}
			Coord coord = (Coord)to_fac.getCenter();
			for (int i= 0; i<work_list.size(); i++) {
				Act a = work_list.get(i);
				a.setCoord(coord);
			}
		}
		else if (!educ_list.isEmpty()) {
			Integer p_id = Integer.valueOf(person.getId().toString());
			Zone home_zone = this.persons.getPerson(p_id).getHousehold().getMunicipality().getZone();
			Zone to_zone = this.getPrimActZone(this.matrices.getMatrix(EDUCATION).getFromLocEntries(home_zone));
			ArrayList<Facility> to_facs = this.zone_educ_fac_mapping.get(to_zone.getId());

			Facility to_fac = null;
			if (to_facs == null) {
				System.out.println("      Person id=" + person.getId() + ": no educ fac in to_zone id=" +
				                   to_zone.getId() + ". Getting a close one...");
				to_fac = this.educFacQuadTree.get(to_zone.getCenter().getX(),to_zone.getCenter().getY());
				System.out.println("      done. (to_fac id=" + to_fac.getId() + ")");
			}
			else {
				to_fac = this.getPrimActFacility(to_facs,EDUCATION);
			}
			Coord coord = (Coord)to_fac.getCenter();
			for (int i= 0; i<educ_list.size(); i++) {
				Act a = educ_list.get(i);
				a.setCoord(coord);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public void run(final Plan plan) {
	}
}

