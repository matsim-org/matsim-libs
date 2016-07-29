/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetHomeLoc.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import playground.balmermi.census2000.data.Persons;

public class PersonSetHomeLoc extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String H = "h";
	private static final String HOME = "home";

	private final ActivityFacilities facilities;
	private final Persons persons;
	private QuadTree<ActivityFacility> homeFacQuadTree = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetHomeLoc(final ActivityFacilities facilities, final Persons persons) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		this.persons = persons;
		this.buildHomeFacQuadTree();
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private void buildHomeFacQuadTree() {
		System.out.println("      building home facility quad tree...");
		Gbl.startMeasurement();
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(HOME) != null) {
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
		this.homeFacQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(HOME) != null) {
				this.homeFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
			}
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		Integer p_id = Integer.valueOf(person.getId().toString());
		Coord coord = persons.getPerson(p_id).getHousehold().getCoord();
		ActivityFacility f = this.homeFacQuadTree.getClosest(coord.getX(), coord.getY());
		Plan plan = person.getSelectedPlan();
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if (H.equals(act.getType())) {
					act.setCoord(f.getCoord());
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
