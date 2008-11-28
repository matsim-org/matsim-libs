/* *********************************************************************** *
 * project: org.matsim.*
 * PersonPrintLinks.java
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

import org.matsim.network.Link;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.routes.CarRoute;

public class PersonPrintLinks extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonPrintLinks() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		Plan plan = person.getPlans().get(0);
		Iterator leg_it = plan.getIteratorLeg();
		while (leg_it.hasNext()) {
			Leg leg = (Leg)leg_it.next();
			System.out.println("Person id=" + person.getId() + "; Leg nr=" + leg.getNum());
			CarRoute route = (CarRoute) leg.getRoute();
			Link[] links = route.getLinks();
			for (int i=0; i<links.length; i++) {
				System.out.println(links[i].getOrigId());
			}
		}
	}
}
