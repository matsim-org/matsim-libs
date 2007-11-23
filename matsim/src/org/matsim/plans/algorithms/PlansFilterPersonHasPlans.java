/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFilterPersonHasPlans.java
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

package org.matsim.plans.algorithms;

import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.utils.identifiers.IdI;

/**
 * This algorithm filters out all persons without plans, leaving only persons in the
 * population having at least one plan.<br/>0
 * Useful when other algorithms may remove plans matching some criteria, so we do not
 * have "empty" people in the population.
 */
public class PlansFilterPersonHasPlans extends PlansAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansFilterPersonHasPlans() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Plans plans) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		// first search all persons without plans
		TreeSet<IdI> pid_set = new TreeSet<IdI>();	// ids of persons to remove
		Iterator<IdI> pid_it = plans.getPersons().keySet().iterator();
		while (pid_it.hasNext()) {
			IdI personId = pid_it.next();
			Person person = plans.getPerson(personId);

			if (person.getPlans().isEmpty()) {
				// the person has no plans left. remove the person afterwards (so we do not disrupt the Iterator)
				pid_set.add(personId);
			}
		}

		// okay, now remove in a 2nd step all persons we do no longer need
		pid_it = pid_set.iterator();
		while (pid_it.hasNext()) {
			IdI pid = pid_it.next();
			plans.getPersons().remove(pid);
		}

		System.out.println("    done.");
		System.out.println("Number of persons removed: " + pid_set.size());
	}
}
