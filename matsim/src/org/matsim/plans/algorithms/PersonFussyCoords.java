/* *********************************************************************** *
 * project: org.matsim.*
 * PersonFussyCoords.java
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
 *                         PersonFussyCoords.java                          *
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

import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

public class PersonFussyCoords extends PersonAlgorithm {

	private static final int RANGE = 250;

	public PersonFussyCoords() {
	}

	@Override
	public void run(final Person person) {
		for (int i=0; i<person.getPlans().size(); i++) {
			Plan plan = person.getPlans().get(i);

			int h_x = (int)((Act)plan.getActsLegs().get(0)).getCoord().getX()-RANGE+Gbl.random.nextInt(2*RANGE+1);
			int h_y = (int)((Act)plan.getActsLegs().get(0)).getCoord().getY()-RANGE+Gbl.random.nextInt(2*RANGE+1);

			for (int j=0; j<plan.getActsLegs().size(); j=j+2) {
				Act act = (Act)plan.getActsLegs().get(j);

				if (act.getType().equals("h")) {
					act.getCoord().setX(h_x);
					act.getCoord().setY(h_y);
				}
				else {
					act.getCoord().setX(act.getCoord().getX() - RANGE +
															Gbl.random.nextInt(2 * RANGE + 1));
					act.getCoord().setY(act.getCoord().getY() - RANGE +
															Gbl.random.nextInt(2 * RANGE + 1));
				}
			}
		}
	}
}
