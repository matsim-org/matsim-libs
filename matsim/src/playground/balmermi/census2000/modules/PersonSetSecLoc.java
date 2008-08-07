/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetSecLoc.java
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

import java.util.Collection;
import java.util.Iterator;

import org.matsim.basic.v01.BasicActImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithmI;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.world.Zone;

import playground.balmermi.census2000.data.Persons;

public class PersonSetSecLoc extends AbstractPersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String H = "h";
	private static final String L = "l";
	private static final String S = "s";
	private static final String E = "e";
	private static final String EDUCATION = "education";
	private static final String LEISURE = "leisure";
	private static final String SHOP = "shop";
	private static final CoordImpl ZERO = new CoordImpl(0.0,0.0);

	private final Facilities facilities;
	private final Persons persons;

	private QuadTree<Facility> shopFacQuadTree = null;
	private QuadTree<Facility> leisFacQuadTree = null;
	private QuadTree<Facility> educFacQuadTree = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetSecLoc(final Facilities facilities, final Persons persons) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		this.persons = persons;
		this.buildShopFacQuadTree();
		this.buildLeisFacQuadTree();
		this.buildEducFacQuadTree();
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// build methods
	//////////////////////////////////////////////////////////////////////

	private void buildShopFacQuadTree() {
		Gbl.startMeasurement();
		System.out.println("      building shop facility quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Facility f : this.facilities.getFacilities().values()) {
			if (f.getActivity(SHOP) != null) {
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
		this.shopFacQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		for (Facility f : this.facilities.getFacilities().values()) {
			if (f.getActivity(SHOP) != null) {
				this.shopFacQuadTree.put(f.getCenter().getX(),f.getCenter().getY(),f);
			}
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
	}

	private void buildLeisFacQuadTree() {
		Gbl.startMeasurement();
		System.out.println("      building leisure facility quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Facility f : this.facilities.getFacilities().values()) {
			if (f.getActivity(LEISURE) != null) {
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
		this.leisFacQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		for (Facility f : this.facilities.getFacilities().values()) {
			if (f.getActivity(LEISURE) != null) {
				this.leisFacQuadTree.put(f.getCenter().getX(),f.getCenter().getY(),f);
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

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final QuadTree<Facility> getFacilities(String act_type) {
		if (E.equals(act_type)) { return this.educFacQuadTree; }
		else if (S.equals(act_type)) { return this.shopFacQuadTree; }
		else if (L.equals(act_type)) { return this.leisFacQuadTree; }
		else { Gbl.errorMsg("act_type=" + act_type + " not allowed!"); return null; }
	}

	private final String getFacilityActType(String act_type) {
		if (E.equals(act_type)) { return EDUCATION; }
		else if (S.equals(act_type)) { return SHOP; }
		else if (L.equals(act_type)) { return LEISURE; }
		else { Gbl.errorMsg("act_type=" + act_type + " not allowed!"); return null; }
	}

	private final Facility getFacility(Collection<Facility> fs, String act_type) {
		act_type = this.getFacilityActType(act_type);
		int i = 0;
		int[] dist_sum = new int[fs.size()];
		Iterator<Facility> f_it = fs.iterator();
		Facility f = f_it.next();
		dist_sum[i] = f.getActivity(act_type).getCapacity();
		if ((dist_sum[i] == 0) || (dist_sum[i] == Integer.MAX_VALUE)) {
			dist_sum[i] = 1;
			f.getActivity(act_type).setCapacity(1);
		}
		while (f_it.hasNext()) {
			f = f_it.next();
			i++;
			int val = f.getActivity(act_type).getCapacity();
			if ((val == 0) || (val == Integer.MAX_VALUE)) {
				val = 1;
				f.getActivity(act_type).setCapacity(1);
			}
			dist_sum[i] = dist_sum[i-1] + val;
		}

		int r = MatsimRandom.random.nextInt(dist_sum[fs.size()-1]);

		i=-1;
		f_it = fs.iterator();
		while (f_it.hasNext()) {
			f = f_it.next();
			i++;
			if (r < dist_sum[i]) {
				return f;
			}
		}
		Gbl.errorMsg("It should never reach this line!");
		return null;
	}

	private final Facility getFacility(Coord coord, double radius, String act_type) {
		Collection<Facility> fs = this.getFacilities(act_type).get(coord.getX(),coord.getY(),radius);
		if (fs.isEmpty()) {
			if (radius > 200000) { Gbl.errorMsg("radius>200'000 meters and still no facility found!"); }
			return this.getFacility(coord,2.0*radius,act_type);
		}
		return this.getFacility(fs,act_type);
	}

	private final Facility getFacility(CoordImpl coord1, CoordImpl coord2, double radius, String act_type) {
		Collection<Facility> fs = this.getFacilities(act_type).get(coord1.getX(),coord1.getY(),radius);
		fs.addAll(this.getFacilities(act_type).get(coord2.getX(),coord2.getY(),radius));
		if (fs.isEmpty()) {
			if (radius > 200000) { Gbl.errorMsg("radius>200'000 meters and still no facility found!"); }
			return this.getFacility(coord1,coord2,2.0*radius,act_type);
		}
		return this.getFacility(fs,act_type);
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		Coord home_coord = null;
		Coord prim_coord = null;
		Plan plan = person.getSelectedPlan();
		Iterator<BasicActImpl> act_it = plan.getIteratorAct();
		while (act_it.hasNext()) {
			Act act = (Act)act_it.next();
			if (H.equals(act.getType())) {
				if (act.getCoord() == null) { Gbl.errorMsg("Person id=" + person.getId() + " has no home coord!"); }
				if (act.getCoord().equals(ZERO)) { Gbl.errorMsg("Person id=" + person.getId() + " has a ZERO home coord!"); }
				home_coord = act.getCoord();
			}
			else {
				if ((act.getCoord() != null) && (!act.getCoord().equals(ZERO))) { prim_coord = act.getCoord(); }
			}
		}
		if ((prim_coord == null) || (home_coord.equals(prim_coord))) {
			// only one location
			playground.balmermi.census2000.data.Person p = this.persons.getPerson(Integer.parseInt(person.getId().toString()));
			Zone z = p.getHousehold().getMunicipality().getZone();
			double radius = 0.5*Math.sqrt((z.getMax().getX()-z.getMin().getX())*(z.getMax().getY()-z.getMin().getY()));
			act_it = plan.getIteratorAct();
			while (act_it.hasNext()) {
				Act act = (Act)act_it.next();
				if ((act.getCoord() == null) || (act.getCoord().equals(ZERO))) {
					Facility f = this.getFacility(home_coord,radius,act.getType());
					act.setCoord(f.getCenter());
				}
			}
		}
		else {
			// two locations
			//
			//           c1               c2
			//    home ---|---|---|---|---|--- prim
			//             \             /
			//              \ r       r /
			//               \         /
			//
			double dx = prim_coord.getX() - home_coord.getX();
			double dy = prim_coord.getY() - home_coord.getY();
			double radius = Math.sqrt(dx*dx+dy*dy)/3.0;
			dx = dx/6.0;
			dy = dy/6.0;
			CoordImpl coord1 = new CoordImpl(home_coord.getX()+dx,home_coord.getY()+dy);
			CoordImpl coord2 = new CoordImpl(prim_coord.getX()-dx,prim_coord.getY()+dy);
			act_it = plan.getIteratorAct();
			while (act_it.hasNext()) {
				Act act = (Act)act_it.next();
				if ((act.getCoord() == null) || (act.getCoord().equals(ZERO))) {
					Facility f = this.getFacility(coord1,coord2,radius,act.getType());
					act.setCoord(f.getCenter());
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public void run(Plan plan) {
	}
}

