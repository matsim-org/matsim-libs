/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCounts.java
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
 *                            PlansCounts.java                             *
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

import java.util.Iterator;

import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

public class PlansCounts extends PlansAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public PlansCounts() {
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

  @Override
	public void run(final Plans plans) {

		System.out.println("Running 'CountAlgorithm' " +
											 "on plans name = " + plans.getName() +
											 "  ...");

		int person_cnt = 0;
		int home_cnt = 0;
		int travcard_cnt = 0;
		int plan_cnt = 0;
		int act_cnt = 0;
		int leg_cnt = 0;

		Iterator<Person> it = plans.getPersons().values().iterator();
		while (it.hasNext()) {
			Person person = it.next();
			person_cnt++;

			for (int i=0; i<person.getPlans().size(); i++) {
				Plan plan = person.getPlans().get(i);
				plan_cnt++;

				act_cnt = act_cnt + (plan.getActsLegs().size() / 2) + 1;
				leg_cnt = leg_cnt + (plan.getActsLegs().size() / 2);
			}
		}

		System.out.println("  # person     = " + person_cnt);
		System.out.println("  # home       = " + home_cnt);
		System.out.println("  # travelcard = " + travcard_cnt);
		System.out.println("  # plan       = " + plan_cnt);
		System.out.println("  # act        = " + act_cnt);
		System.out.println("  # leg        = " + leg_cnt);

		System.out.println("Done.");

  }
}
