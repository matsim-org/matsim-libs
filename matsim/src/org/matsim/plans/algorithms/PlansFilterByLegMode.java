/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFilterByLegMode.java
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

import org.matsim.basic.v01.Id;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

/**
 * This algorithm filters out all persons having plans with legs with a certain leg mode.
 * There are two modes how the filter works: If <em>exclusive filtering</em> is used,
 * only plans are kept where persons travel exclusively with the specified leg mode. If
 * the <em>non-exclusive filtering</em> is used, all plans with at least one leg of the
 * specified leg mode are kept.<br/>
 * Plans which do not fulfill the filter criteria are removed from a person, Persons with
 * no plans are removed from the population.
 */
public class PlansFilterByLegMode extends PlansAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	private String legMode;
	private boolean exclusiveFilter;

	// optimization: instead of doing a String.equals() every time, we do it once and store the result
	private boolean legModeIsCar;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansFilterByLegMode(final String legMode, final boolean exclusiveFilter) {
		super();
		this.legMode = legMode;
		this.exclusiveFilter = exclusiveFilter;
		this.legModeIsCar = legMode.equals("car");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Plans plans) {
		int planCount = 0;
		System.out.println("    running " + this.getClass().getName() + " algorithm...");
		String never = "never";

		TreeSet<Id> pid_set = new TreeSet<Id>();	// ids of persons to remove
		Iterator<Id> pid_it = plans.getPersons().keySet().iterator();
		while (pid_it.hasNext()) {
			Id personId = pid_it.next();
			Person person = plans.getPerson(personId);

			for (int i=person.getPlans().size()-1; i>=0; i--) {
				Plan plan = person.getPlans().get(i);
				boolean hasSearchedLegMode = false;
				boolean hasOtherLegMode = false;

				for (int j=1; j<plan.getActsLegs().size(); j+=2) {
					Leg leg = (Leg)plan.getActsLegs().get(j);
					if (leg.getMode().equals(this.legMode)) {
						hasSearchedLegMode = true;
					} else {
						hasOtherLegMode = true;
					}
				}
				if (this.legModeIsCar && never.equals(person.getCarAvail())) {
					// person cannot drive car if she has no car. this means, the person was given a lift by someone else
					// --> do not include this person, as we're only interested in the driver
					hasSearchedLegMode = false;
				}
				if ((!hasSearchedLegMode) || (hasOtherLegMode && this.exclusiveFilter)) {
					person.getPlans().remove(i);
					i--;	//otherwise, we would skip one plan
					planCount++;
				}
			}
			if (person.getPlans().isEmpty()) {
				// the person has no plans left. remove the person afterwards (so we do not disrupt the Iterator)
				pid_set.add(personId);
			}
			else {
			}
		}

		// okay, now remove in a 2nd step all persons we do no longer need
		pid_it = pid_set.iterator();
		while (pid_it.hasNext()) {
			Id pid = pid_it.next();
			plans.getPersons().remove(pid);
		}

		System.out.println("    done.");
		System.out.println("Number of plans removed:   " + planCount);
		System.out.println("Number of persons removed: " + pid_set.size());
	}
}
