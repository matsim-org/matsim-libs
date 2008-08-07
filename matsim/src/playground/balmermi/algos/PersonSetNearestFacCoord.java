/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetNearestFacCoord.java
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

package playground.balmermi.algos;

import java.util.Iterator;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithmI;
import org.matsim.utils.geometry.CoordI;

public class PersonSetNearestFacCoord extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String H = "h";
	private static final String W = "w";
	private static final String L = "l";
	private static final String S = "s";
	private static final String E = "e";
	private static final String HOME = "home";
	private static final String WORK = "work";
	private static final String EDUCATION = "education";
	private static final String LEISURE = "leisure";
	private static final String SHOP = "shop";

	private final Facilities facilities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetNearestFacCoord(final Facilities facilities) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// build methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final String getFacilityActType(String act_type) {
		if (act_type.startsWith(E)) { return EDUCATION; }
		else if (act_type.startsWith(S)) { return SHOP; }
		else if (act_type.startsWith(L)) { return LEISURE; }
		else if (act_type.startsWith(H)) { return HOME; }
		else if (act_type.startsWith(W)) { return WORK; }
		else { Gbl.errorMsg("act_type=" + act_type + " not allowed!"); return null; }
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		if (person.getPlans().size() != 1) { Gbl.errorMsg("Each person must have one plan!"); }
		Plan plan = person.getPlans().get(0);
		this.run(plan);
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public void run(Plan plan) {
		Iterator<?> act_it = plan.getIteratorAct();
		while (act_it.hasNext()) {
			Act act = (Act)act_it.next();
			String type = this.getFacilityActType(act.getType());
			CoordI coord = act.getCoord();
			if (coord == null) { Gbl.errorMsg("Each act must have a coord!"); }
			double nearest_dist = Double.MAX_VALUE;
			Facility nearest_f = null;
			Iterator<? extends Facility> f_it = this.facilities.getFacilities().values().iterator();
			while (f_it.hasNext()) {
				Facility f = f_it.next();
				if (f.getActivities().containsKey(type)) {
					double dist = f.calcDistance(coord);
					if (dist < nearest_dist) {
						nearest_dist = dist;
						nearest_f = f;
					}
				}
			}
			if (nearest_f == null) {
				Gbl.errorMsg("p_id=" + plan.getPerson().getId() + ": no facility found for act=" + act);
			}
			act.setCoord(nearest_f.getCenter());
			System.out.println("  p_id=" + plan.getPerson().getId() + ", act=" + act.getType() + ": nearest dist=" + nearest_dist);
		}
	}
}

