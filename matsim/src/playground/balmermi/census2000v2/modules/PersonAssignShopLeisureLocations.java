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

package playground.balmermi.census2000v2.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.world.Zone;

import playground.balmermi.census2000v2.data.CAtts;

public class PersonAssignShopLeisureLocations extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonAssignShopLeisureLocations.class);

	private static final String L = "l";
	private static final String S = "s";

	private final Facilities facilities;

	private QuadTree<Activity> shopActQuadTree = null;
	private QuadTree<Activity> leisActQuadTree = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignShopLeisureLocations(final Facilities facilities) {
		super();
		log.info("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		this.buildShopActQuadTree();
		this.buildLeisActQuadTree();
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// build methods
	//////////////////////////////////////////////////////////////////////

	private void buildShopActQuadTree() {
		log.info("      building shop activity quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		ArrayList<Activity> acts = new ArrayList<Activity>();
		for (Facility f : this.facilities.getFacilities().values()) {
			for (Activity a : f.getActivities().values()) {
				if (a.getType().equals(CAtts.ACT_S1) || a.getType().equals(CAtts.ACT_S2) || a.getType().equals(CAtts.ACT_S3) ||
				    a.getType().equals(CAtts.ACT_S4) || a.getType().equals(CAtts.ACT_S5) || a.getType().equals(CAtts.ACT_SOTHR)) {
					acts.add(a);
					if (f.getCenter().getX() < minx) { minx = f.getCenter().getX(); }
					if (f.getCenter().getY() < miny) { miny = f.getCenter().getY(); }
					if (f.getCenter().getX() > maxx) { maxx = f.getCenter().getX(); }
					if (f.getCenter().getY() > maxy) { maxy = f.getCenter().getY(); }
				}
			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.shopActQuadTree = new QuadTree<Activity>(minx, miny, maxx, maxy);
		for (Activity a : acts) {
			this.shopActQuadTree.put(a.getFacility().getCenter().getX(),a.getFacility().getCenter().getY(),a);
		}
		log.info("      done.");
	}

	private void buildLeisActQuadTree() {
		log.info("      building leisure activity quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		ArrayList<Activity> acts = new ArrayList<Activity>();
		for (Facility f : this.facilities.getFacilities().values()) {
			for (Activity a : f.getActivities().values()) {
				if (a.getType().equals(CAtts.ACT_LC) || a.getType().equals(CAtts.ACT_LG) || a.getType().equals(CAtts.ACT_LS)) {
					acts.add(a);
					if (f.getCenter().getX() < minx) { minx = f.getCenter().getX(); }
					if (f.getCenter().getY() < miny) { miny = f.getCenter().getY(); }
					if (f.getCenter().getX() > maxx) { maxx = f.getCenter().getX(); }
					if (f.getCenter().getY() > maxy) { maxy = f.getCenter().getY(); }
				}
			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.leisActQuadTree = new QuadTree<Activity>(minx, miny, maxx, maxy);
		for (Activity a : acts) {
			this.leisActQuadTree.put(a.getFacility().getCenter().getX(),a.getFacility().getCenter().getY(),a);
		}
		log.info("      done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final Activity getActivity(Collection<Activity> activities, String act_type) {
		ArrayList<String> act_types = new ArrayList<String>();
		if (act_type.startsWith(S)) {
			act_types.add(CAtts.ACT_S1); act_types.add(CAtts.ACT_S2); act_types.add(CAtts.ACT_S3);
			act_types.add(CAtts.ACT_S4); act_types.add(CAtts.ACT_S5); act_types.add(CAtts.ACT_SOTHR);
		}
		else if (act_type.startsWith(L)) {
			act_types.add(CAtts.ACT_LC); act_types.add(CAtts.ACT_LG); act_types.add(CAtts.ACT_LS);
		}
		else {
			Gbl.errorMsg("act_type="+act_type+" not allowed!");
		}
		
		ArrayList<Activity> acts = new ArrayList<Activity>();
		ArrayList<Integer> sum_cap_acts = new ArrayList<Integer>();
		int sum_cap = 0;
		Iterator<Activity> a_it = activities.iterator();
		while (a_it.hasNext()) {
			Activity a = a_it.next();
			sum_cap += a.getCapacity();
			sum_cap_acts.add(sum_cap);
			acts.add(a);
		}
		int r = MatsimRandom.random.nextInt(sum_cap);
		for (int i=0; i<sum_cap_acts.size(); i++) {
			if (r < sum_cap_acts.get(i)) {
				return acts.get(i);
			}
		}
		Gbl.errorMsg("It should never reach this line!");
		return null;
	}

	//////////////////////////////////////////////////////////////////////

	private final QuadTree<Activity> getActivities(String act_type) {
		if (act_type.startsWith(S)) { return this.shopActQuadTree; }
		else if (act_type.startsWith(L)) { return this.leisActQuadTree; }
		else { Gbl.errorMsg("act_type=" + act_type + " not allowed!"); return null; }
	}

	//////////////////////////////////////////////////////////////////////

	private final Activity getActivity(Coord coord, double radius, String act_type) {
		Collection<Activity> acts = this.getActivities(act_type).get(coord.getX(),coord.getY(),radius);
		if (acts.isEmpty()) {
			if (radius > 200000.0) { Gbl.errorMsg("radius>200'000 meters and still no facility found!"); }
			return this.getActivity(coord,2.0*radius,act_type);
		}
		return this.getActivity(acts,act_type);
	}

	private final Activity getActivity(Coord coord1, Coord coord2, double radius, String act_type) {
		Collection<Activity> acts = this.getActivities(act_type).get(coord1.getX(),coord1.getY(),radius);
		acts.addAll(this.getActivities(act_type).get(coord2.getX(),coord2.getY(),radius));
		if (acts.isEmpty()) {
			if (radius > 200000.0) { Gbl.errorMsg("radius>200'000 meters and still no facility found!"); }
			return this.getActivity(coord1,coord2,2.0*radius,act_type);
		}
		return this.getActivity(acts,act_type);
	}

	//////////////////////////////////////////////////////////////////////

	private final void assignRemainingLocations(Act act, Facility start, Facility end) {
		Coord c_start = start.getCenter();
		Coord c_end   = end.getCenter();

		double dx = c_end.getX() - c_start.getX();
		double dy = c_end.getX() - c_start.getX();
		if ((dx == 0.0) && (dy == 0.0)) {
			// c_start and c_end equal
			Zone z = (Zone)start.getUpMapping().values().iterator().next();
			double r = 0.5*Math.sqrt((z.getMax().getX()-z.getMin().getX())*(z.getMax().getY()-z.getMin().getY()));
			Activity activity = this.getActivity(c_start,r,act.getType());
			act.setType(activity.getType());
			act.setFacility(activity.getFacility());
			act.setCoord(null);
		}
		else {
			// c_start and c_end different
			double r = Math.sqrt(dx*dx+dy*dy)/3.0;
			dx = dx/6.0;
			dy = dy/6.0;
			Coord c1 = new CoordImpl(c_start.getX()+dx,c_start.getY()+dy);
			Coord c2 = new CoordImpl(c_end.getX()-dx,c_end.getY()+dy);
			Activity activity = this.getActivity(c1,c2,r,act.getType());
			act.setType(activity.getType());
			act.setFacility(activity.getFacility());
			act.setCoord(null);
		}
	}
	
	private final void assignRemainingLocations(Plan plan, int start, int end) {
		Coord c_start = ((Act)plan.getActsLegs().get(start)).getFacility().getCenter();
		Coord c_end   = ((Act)plan.getActsLegs().get(end)).getFacility().getCenter();
		
		double dx = c_end.getX() - c_start.getX();
		double dy = c_end.getX() - c_start.getX();
		if ((dx == 0.0) && (dy == 0.0)) {
			// c_start and c_end equal
			Zone z = (Zone)((Act)plan.getActsLegs().get(start)).getFacility().getUpMapping().values().iterator().next();
			double r = 0.5*Math.sqrt((z.getMax().getX()-z.getMin().getX())*(z.getMax().getY()-z.getMin().getY()));
			for (int i=start+2; i<end; i=i+2) {
				Act act = (Act)plan.getActsLegs().get(i);
				Activity activity = this.getActivity(c_start,r,act.getType());
				act.setType(activity.getType());
				act.setFacility(activity.getFacility());
				act.setCoord(null);
			}
		}
		else {
			// c_start and c_end different
			double r = Math.sqrt(dx*dx+dy*dy)/3.0;
			dx = dx/6.0;
			dy = dy/6.0;
			Coord c1 = new CoordImpl(c_start.getX()+dx,c_start.getY()+dy);
			Coord c2 = new CoordImpl(c_end.getX()-dx,c_end.getY()+dy);
			for (int i=start+2; i<end; i=i+2) {
				Act act = (Act)plan.getActsLegs().get(i);
				Activity activity = this.getActivity(c1,c2,r,act.getType());
				act.setType(activity.getType());
				act.setFacility(activity.getFacility());
				act.setCoord(null);
			}
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		if (person.getPlans().size() != 1) { Gbl.errorMsg("pid="+person.getId()+": There must be exactly one plan."); }
		Plan plan = person.getSelectedPlan();
		this.run(plan);
	}

	public void run(Plan plan) {
		for (int i=0; i<plan.getActsLegs().size(); i=i+2) {
			Act act = (Act)plan.getActsLegs().get(i);
			if (act.getFacility() == null) {
				// get the prev act with a facility
				Facility start = null;
				for (int b=i-2; b>=0; b=b-2) {
					Act b_act = (Act)plan.getActsLegs().get(b);
					if (b_act.getFacility() != null) { start = b_act.getFacility(); break; }
				}
				// get the next act with a facility
				Facility end = null;
				for (int a=i+2; a<plan.getActsLegs().size(); a=a+2) {
					Act a_act = (Act)plan.getActsLegs().get(a);
					if (a_act.getFacility() != null) { end = a_act.getFacility(); break; }
				}
				if ((start == null) || (end == null)) { Gbl.errorMsg("That should not happen!"); }
				this.assignRemainingLocations(act,start,end);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////
}

