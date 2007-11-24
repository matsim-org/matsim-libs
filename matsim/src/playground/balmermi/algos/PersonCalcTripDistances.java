/* *********************************************************************** *
 * project: org.matsim.*
 * PersonCalcTripDistances.java
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

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;

public class PersonCalcTripDistances extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonCalcTripDistances() {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		if (plan == null) { Gbl.errorMsg("Person id=" + person.getId() + "does not have a selected plan assigned!"); }
		for (int i=1; i<plan.getActsLegs().size()-1; i=i+2) {
			Act prev = (Act)plan.getActsLegs().get(i-1);
			Leg leg = (Leg)plan.getActsLegs().get(i);
			Act next = (Act)plan.getActsLegs().get(i+1);
			
			if (prev.getLinkId().equals(next.getLinkId())) {
				if (!leg.getRoute().getRoute().isEmpty()) { Gbl.errorMsg("Person id=" + person.getId() + ", leg nr=" + leg.getNum() + ": route should be empty!"); }
				else { leg.getRoute().setDist(0.0); }
			}
			else {
				if (leg.getRoute().getRoute().isEmpty()) { leg.getRoute().setDist(next.getCoord().calcDistance(prev.getCoord())); }
				else {
					Link [] links = leg.getRoute().getLinkRoute();
					double dist = prev.getLink().getLength();
					for (int j=0; j<links.length; j++) { dist += links[j].getLength(); }
					leg.getRoute().setDist(dist);
				}
			}
		}
	}

	public void run(Plan plan) {
	}
}
