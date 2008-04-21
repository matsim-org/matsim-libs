/* *********************************************************************** *
 * project: org.matsim.*
 * PlansReduction.java
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


/* *********************************************************************** *
 *               org.matsim.demandmodeling.plans.algorithms                *
 *                           PlansReduction.java                           *
 *                          ---------------------                          *
 * copyright       : (C) 2006 by                                           *
 *                   Michael Balmer, Konrad Meister, Marcel Rieser,        *
 *                   David Strippgen, Kai Nagel, Kay W. Axhausen,          *
 *                   Technische Universitaet Berlin (TU-Berlin) and        *
 *                   Swiss Federal Institute of Technology Zurich (ETHZ)   *
 * email           : balmermi at gmail dot com                             *
 *                 : rieser at gmail dot com                               *
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

import java.util.TreeSet;

import org.matsim.basic.v01.Id;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

public class PlansReduction extends PlansAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansReduction() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Plans plans) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		TreeSet<Id> pid_set = new TreeSet<Id>();
		for (Id pid : plans.getPersons().keySet()) {
			Person p = plans.getPerson(pid);

			for (int i=p.getPlans().size()-1; i>=0; i--) {
				Plan plan = p.getPlans().get(i);

				for (int j=0; j<plan.getActsLegs().size(); j+=2) {
					Act act = (Act)plan.getActsLegs().get(j);
					if (act.getType().equals("m")) {
						p.getPlans().remove(i);
						break;
					}
				}
			}
			if (p.getPlans().isEmpty()) {
				pid_set.add(pid);
			}
			else {
			}
		}

		for (Id pid : pid_set) {
			plans.getPersons().remove(pid);
		}

		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////
}
