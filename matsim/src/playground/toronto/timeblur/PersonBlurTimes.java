/* *********************************************************************** *
 * project: org.matsim.*
 * PersonBlurTimes.java
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

package playground.toronto.timeblur;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonBlurTimes extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final PlanAlgorithm pa;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonBlurTimes(Config config, int mutationRange) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		TimeAllocationMutator tam = new TimeAllocationMutator(config, mutationRange);
		tam.prepareReplanning();
		pa = tam.getPlanAlgoInstance();
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		for (Plan p : person.getPlans()) {
			this.run(p);
		}
	}

	public void run(Plan plan) {
		pa.run(plan);
	}
}
