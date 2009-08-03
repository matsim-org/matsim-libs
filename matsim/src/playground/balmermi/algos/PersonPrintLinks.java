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

import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
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
	public void run(PersonImpl person) {
		PlanImpl plan = person.getPlans().get(0);
		int counter = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				System.out.println("Person id=" + person.getId() + "; Leg nr=" + counter);
				counter++;
				NetworkRoute route = (NetworkRoute) leg.getRoute();
				for (Link link : route.getLinks()) {
					System.out.println(((LinkImpl)link).getOrigId());
				}
			}
		}
	}
}
