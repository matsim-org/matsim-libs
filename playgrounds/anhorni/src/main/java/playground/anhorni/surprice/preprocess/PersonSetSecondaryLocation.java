/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice.preprocess;

import java.util.Collection;
import java.util.Iterator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonSetSecondaryLocation extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String H = "home";
	private static final String L = "leisure";
	private static final String S = "shop";
	private static final String E = "education";
	private static final String B = "business";
	private static final String EDUCATION = "education";
	private static final String LEISURE = "leisure";
	private static final String SHOP = "shop";
	private static final String BUSINESS = "business";
	private static final Coord ZERO = new Coord(0.0, 0.0);

	private final ActivityFacilities facilities;

	private QuadTree<ActivityFacility> shopFacQuadTree = null;
	private QuadTree<ActivityFacility> leisFacQuadTree = null;
	private QuadTree<ActivityFacility> educFacQuadTree = null;
	private QuadTree<ActivityFacility> businessFacQuadTree = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetSecondaryLocation(final ActivityFacilities facilities) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		this.buildShopFacQuadTree();
		this.buildLeisFacQuadTree();
		this.buildEducFacQuadTree();
		this.buildBusinessFacQuadTree();
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
		this.shopFacQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(SHOP) != null) {
				this.shopFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
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
		this.leisFacQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(LEISURE) != null) {
				this.leisFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
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
		this.educFacQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(EDUCATION) != null) {
				this.educFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
			}
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
	}
	
	private void buildBusinessFacQuadTree() {
		Gbl.startMeasurement();
		System.out.println("      building other facility quad tree...");
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
		this.businessFacQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(BUSINESS) != null) {
				this.businessFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
			}
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final QuadTree<ActivityFacility> getFacilities(String act_type) {
		if (E.equals(act_type)) { return this.educFacQuadTree; }
		else if (S.equals(act_type)) { return this.shopFacQuadTree; }
		else if (L.equals(act_type)) { return this.leisFacQuadTree; }
		else if (B.equals(act_type)) { return this.businessFacQuadTree; }
		else { throw new RuntimeException("act_type=" + act_type + " not allowed!"); }
	}

	private final String getFacilityActType(String act_type) {
		if (E.equals(act_type)) { return EDUCATION; }
		else if (S.equals(act_type)) { return SHOP; }
		else if (L.equals(act_type)) { return LEISURE; }
		else if (B.equals(act_type)) { return BUSINESS; }
		else { throw new RuntimeException("act_type=" + act_type + " not allowed!"); }
	}

	private final ActivityFacility getFacility(Collection<ActivityFacility> fs, String act_type) {
		act_type = this.getFacilityActType(act_type);
		int i = 0;
		int[] dist_sum = new int[fs.size()];
		Iterator<ActivityFacility> f_it = fs.iterator();
		ActivityFacility f = f_it.next();
		ActivityOptionImpl activityOption = (ActivityOptionImpl) f.getActivityOptions().get(act_type);
		dist_sum[i] = (int) activityOption.getCapacity();
		if ((dist_sum[i] == 0) || (dist_sum[i] == Integer.MAX_VALUE)) {
			dist_sum[i] = 1;
			activityOption.setCapacity(1);
		}
		while (f_it.hasNext()) {
			f = f_it.next();
			i++;
			int val = (int) activityOption.getCapacity();
			if ((val == 0) || (val == Integer.MAX_VALUE)) {
				val = 1;
				activityOption.setCapacity(1);
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
		throw new RuntimeException("It should never reach this line!");
	}

	private final ActivityFacility getFacility(Coord coord, double radius, String act_type) {
		Collection<ActivityFacility> fs = this.getFacilities(act_type).getDisk(coord.getX(), coord.getY(), radius);
		if (fs.isEmpty()) {
			if (radius > 200000) { throw new RuntimeException("radius>200'000 meters and still no facility found!"); }
			return this.getFacility(coord,2.0*radius,act_type);
		}
		return this.getFacility(fs,act_type);
	}

	private final ActivityFacility getFacility(Coord coord1, Coord coord2, double radius, String act_type) {
		Collection<ActivityFacility> fs = this.getFacilities(act_type).getDisk(coord1.getX(), coord1.getY(), radius);
		fs.addAll(this.getFacilities(act_type).getDisk(coord2.getX(), coord2.getY(), radius));
		if (fs.isEmpty()) {
			if (radius > 200000) { throw new RuntimeException(act_type + " radius>200'000 meters and still no facility found!"); 
			}
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
					if (act.getCoord() == null) {
						throw new RuntimeException("Person id=" + person.getId() + " has no home coord!");
					}
					if (act.getCoord().equals(ZERO)) {
						throw new RuntimeException("Person id=" + person.getId() + " has a ZERO home coord!");
					}
					home_coord = act.getCoord();
				} else {
					if ((act.getCoord() != null) && (!act.getCoord().equals(ZERO))) {
						prim_coord = act.getCoord();
					}
				}
			}
		}
		if ((prim_coord == null) || (home_coord.equals(prim_coord))) {
			// only one location
			double radius = 5000.0; // arbitrarily set to 5km -> TODO: refine
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					if ((act.getCoord() == null) || (act.getCoord().equals(ZERO))) {
						ActivityFacility f = this.getFacility(home_coord,radius,act.getType());
						act.setCoord(f.getCoord());
						act.setFacilityId(f.getId());
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
			Coord coord1 = new Coord(home_coord.getX() + dx, home_coord.getY() + dy);
			Coord coord2 = new Coord(prim_coord.getX() - dx, prim_coord.getY() + dy);
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					if ((act.getCoord() == null) || (act.getCoord().equals(ZERO))) {
						ActivityFacility f = this.getFacility(coord1,coord2,radius,act.getType());
						act.setCoord(f.getCoord());
						act.setFacilityId(f.getId());
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

