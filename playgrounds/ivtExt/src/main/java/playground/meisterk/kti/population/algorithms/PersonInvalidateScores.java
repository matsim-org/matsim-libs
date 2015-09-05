/* *********************************************************************** *
 * project: org.matsim.*
 * PersonInvalidateScores.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.kti.population.algorithms;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class PersonInvalidateScores extends AbstractPersonAlgorithm {

	@Override
	public void run(Person person) {
		
		for (Plan plan : person.getPlans()) {
			plan.setScore(null);
		}
		
	}

}
