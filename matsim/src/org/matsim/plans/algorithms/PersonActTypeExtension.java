/* *********************************************************************** *
 * project: org.matsim.*
 * PersonActTypeExtension.java
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
 *                       PersonActTypeExtension.java                       *
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

import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

public class PersonActTypeExtension extends PersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonActTypeExtension() {
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		for (int i=0; i<person.getPlans().size(); i++) {
			Plan plan = person.getPlans().get(i);

			int w_cnt = 0;
			int e_cnt = 0;

			for (int j=0; j<plan.getActsLegs().size(); j=j+2) {
				Act act = (Act)plan.getActsLegs().get(j);

				if (act.getType().equals("w")) { w_cnt++; }
				if (act.getType().equals("e")) { e_cnt++; }
			}

			if (w_cnt == 1) {
				for (int j=0; j<plan.getActsLegs().size(); j=j+2) {
					Act act = (Act)plan.getActsLegs().get(j);
					if ((act.getType().equals("w")) && (j>2)) {
						act.setType("w" + "3");
					}
				}
			}

			if (w_cnt > 1) {
				w_cnt = 1;
				for (int j=0; j<plan.getActsLegs().size(); j=j+2) {
					Act act = (Act)plan.getActsLegs().get(j);
					if (act.getType().equals("w")) {
						act.setType("w" + w_cnt);
						w_cnt++;
					}
				}
			}

			if (e_cnt == 1) {
				for (int j=0; j<plan.getActsLegs().size(); j=j+2) {
					Act act = (Act)plan.getActsLegs().get(j);
					if ((act.getType().equals("e")) && (j>2)) {
						act.setType("e" + "3");
					}
				}
			}

			if (e_cnt > 1) {
				e_cnt = 1;
				for (int j=0; j<plan.getActsLegs().size(); j=j+2) {
					Act act = (Act)plan.getActsLegs().get(j);
					if (act.getType().equals("e")) {
						act.setType("e" + e_cnt);
						e_cnt++;
					}
				}
			}
		}
	}
}
