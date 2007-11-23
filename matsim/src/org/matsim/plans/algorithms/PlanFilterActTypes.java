/* *********************************************************************** *
 * project: org.matsim.*
 * PlanFilterActTypes.java
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

public class PlanFilterActTypes extends PersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	Set<String> matchingActs = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlanFilterActTypes(Set<String> matchingActs) {
		this.matchingActs = matchingActs;
	}
	
	public PlanFilterActTypes(String[] matchingActs) {
		this.matchingActs = new HashSet<String>(matchingActs.length);
		for (int i = 0; i < matchingActs.length; i++) {
			this.matchingActs.add(matchingActs[i]);
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(Person person) {
		for (Iterator<Plan> iter = person.getPlans().iterator(); iter.hasNext(); ) {
			Plan plan = iter.next();
			boolean match = false;
			List actsLegs = plan.getActsLegs();
			for (int i = 0, max = actsLegs.size(); i < max; i += 2) {
				Act act = (Act) actsLegs.get(i);
				if (this.matchingActs.contains(act.getType())) {
					match = true;
					break;
				}
			}
			if (!match) {
				iter.remove();
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void print() {
	}

}
