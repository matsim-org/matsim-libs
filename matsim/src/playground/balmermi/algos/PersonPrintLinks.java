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

import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

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
		int counter = 0;
		while (leg_it.hasNext()) {
			Leg leg = (Leg)leg_it.next();
			System.out.println("Person id=" + person.getId() + "; Leg nr=" + counter);
			counter++;
			CarRoute route = (CarRoute) leg.getRoute();
			for (Link link : route.getLinks()) {
				System.out.println(link.getOrigId());
			}
		}
	}
}
