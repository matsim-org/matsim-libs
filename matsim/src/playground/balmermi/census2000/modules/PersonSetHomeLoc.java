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

import java.util.Iterator;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithmI;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.shared.Coord;

import playground.balmermi.census2000.data.Persons;

public class PersonSetHomeLoc extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String H = "h";
	private static final String HOME = "home";

	private final Facilities facilities;
	private final Persons persons;
	private QuadTree<Facility> homeFacQuadTree = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetHomeLoc(final Facilities facilities, final Persons persons) {
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
		for (Facility f : this.facilities.getFacilities().values()) {
			if (f.getActivity(HOME) != null) {
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
		this.homeFacQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		for (Facility f : this.facilities.getFacilities().values()) {
			if (f.getActivity(HOME) != null) {
				this.homeFacQuadTree.put(f.getCenter().getX(),f.getCenter().getY(),f);
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
		Facility f = this.homeFacQuadTree.get(coord.getX(),coord.getY());
		Plan plan = person.getSelectedPlan();
		Iterator act_it = plan.getIteratorAct();
		while (act_it.hasNext()) {
			Act act = (Act)act_it.next();
			if (H.equals(act.getType())) {
				act.setCoord(f.getCenter());
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public void run(Plan plan) {
	}
}
