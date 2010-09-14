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

import org.matsim.api.core.v01.Coord;
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
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.world.Zone;

import playground.balmermi.census2000.data.Persons;

public class PersonSetSecLoc extends AbstractPersonAlgorithm implements PlanAlgorithm {

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

	private final ActivityFacilitiesImpl facilities;
	private final Persons persons;

	private QuadTree<ActivityFacilityImpl> shopFacQuadTree = null;
	private QuadTree<ActivityFacilityImpl> leisFacQuadTree = null;
	private QuadTree<ActivityFacilityImpl> educFacQuadTree = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetSecLoc(final ActivityFacilitiesImpl facilities, final Persons persons) {
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
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(SHOP) != null) {
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
		this.shopFacQuadTree = new QuadTree<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(SHOP) != null) {
				this.shopFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacilityImpl) f);
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
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(LEISURE) != null) {
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
		this.leisFacQuadTree = new QuadTree<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(LEISURE) != null) {
				this.leisFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacilityImpl) f);
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

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final QuadTree<ActivityFacilityImpl> getFacilities(String act_type) {
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

	private final ActivityFacilityImpl getFacility(Collection<ActivityFacilityImpl> fs, String act_type) {
		act_type = this.getFacilityActType(act_type);
		int i = 0;
		int[] dist_sum = new int[fs.size()];
		Iterator<ActivityFacilityImpl> f_it = fs.iterator();
		ActivityFacilityImpl f = f_it.next();
		ActivityOptionImpl activityOption = (ActivityOptionImpl) f.getActivityOptions().get(act_type);
		dist_sum[i] = activityOption.getCapacity().intValue();
		if ((dist_sum[i] == 0) || (dist_sum[i] == Integer.MAX_VALUE)) {
			dist_sum[i] = 1;
			activityOption.setCapacity((double) 1);
		}
		while (f_it.hasNext()) {
			f = f_it.next();
			i++;
			int val = activityOption.getCapacity().intValue();
			if ((val == 0) || (val == Integer.MAX_VALUE)) {
				val = 1;
				activityOption.setCapacity((double) 1);
			}
			dist_sum[i] = dist_sum[i-1] + val;
		}

		int r = MatsimRandom.getRandom().nextInt(dist_sum[fs.size()-1]);

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

	private final ActivityFacilityImpl getFacility(Coord coord, double radius, String act_type) {
		Collection<ActivityFacilityImpl> fs = this.getFacilities(act_type).get(coord.getX(),coord.getY(),radius);
		if (fs.isEmpty()) {
			if (radius > 200000) { Gbl.errorMsg("radius>200'000 meters and still no facility found!"); }
			return this.getFacility(coord,2.0*radius,act_type);
		}
		return this.getFacility(fs,act_type);
	}

	private final ActivityFacilityImpl getFacility(CoordImpl coord1, CoordImpl coord2, double radius, String act_type) {
		Collection<ActivityFacilityImpl> fs = this.getFacilities(act_type).get(coord1.getX(),coord1.getY(),radius);
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
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				if (H.equals(act.getType())) {
					if (act.getCoord() == null) { Gbl.errorMsg("Person id=" + person.getId() + " has no home coord!"); }
					if (act.getCoord().equals(ZERO)) { Gbl.errorMsg("Person id=" + person.getId() + " has a ZERO home coord!"); }
					home_coord = act.getCoord();
				} else {
					if ((act.getCoord() != null) && (!act.getCoord().equals(ZERO))) { prim_coord = act.getCoord(); }
				}
			}
		}
		if ((prim_coord == null) || (home_coord.equals(prim_coord))) {
			// only one location
			playground.balmermi.census2000.data.MyPerson p = this.persons.getPerson(Integer.parseInt(person.getId().toString()));
			Zone z = p.getHousehold().getMunicipality().getZone();
			double radius = 0.5*Math.sqrt((z.getMax().getX()-z.getMin().getX())*(z.getMax().getY()-z.getMin().getY()));
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					if ((act.getCoord() == null) || (act.getCoord().equals(ZERO))) {
						ActivityFacilityImpl f = this.getFacility(home_coord,radius,act.getType());
						act.setCoord(f.getCoord());
					}
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
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					if ((act.getCoord() == null) || (act.getCoord().equals(ZERO))) {
						ActivityFacilityImpl f = this.getFacility(coord1,coord2,radius,act.getType());
						act.setCoord(f.getCoord());
					}
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

