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

import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonCalcTripDistances extends AbstractPersonAlgorithm implements PlanAlgorithm {

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
		if (plan == null) { throw new RuntimeException("Person id=" + person.getId() + "does not have a selected plan assigned!"); }
		for (int i=1; i<plan.getActsLegs().size()-1; i=i+2) {
			Act prev = (Act)plan.getActsLegs().get(i-1);
			Leg leg = (Leg)plan.getActsLegs().get(i);
			Act next = (Act)plan.getActsLegs().get(i+1);

			if (prev.getLinkId().equals(next.getLinkId())) {
				if (!((CarRoute) leg.getRoute()).getNodes().isEmpty()) { throw new RuntimeException("Person id=" + person.getId() + ": route should be empty!"); }
				leg.getRoute().setDist(0.0);
			}
			else {
				if (((CarRoute) leg.getRoute()).getNodes().isEmpty()) { leg.getRoute().setDist(next.getCoord().calcDistance(prev.getCoord())); }
				else {
					double dist = prev.getLink().getLength();
					for (Link link : ((CarRoute) leg.getRoute()).getLinks()) {
						dist += link.getLength();
					}
					leg.getRoute().setDist(dist);
				}
			}
		}
	}

	public void run(Plan plan) {
	}
}
