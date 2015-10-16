/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveUnselectedPlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.population;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class RemoveUnselectedPlans extends AbstractPersonAlgorithm {

	@Override
	public void run(Person person) {
		if (person instanceof PersonImpl) {
			PersonUtils.removeUnselectedPlans(((PersonImpl) person));
		}
	}

}
