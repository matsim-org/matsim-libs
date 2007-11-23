/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetPrimAct.java
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
 *                          PersonSetPrimAct.java                          *
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

import java.util.TreeMap;

import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

public class PersonSetPrimAct extends PersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// memeber variables
	//////////////////////////////////////////////////////////////////////

	private static final TreeMap<String, Integer> order = new TreeMap<String, Integer>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetPrimAct() {
		super();
		order.put("w", Integer.valueOf(1));
		order.put("e", Integer.valueOf(2));
		order.put("l", Integer.valueOf(3));
		order.put("s", Integer.valueOf(4));
		order.put("h", Integer.valueOf(5)); // used as dummy
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		for (int i=0; i<person.getPlans().size(); i++) {
			Plan plan = person.getPlans().get(i);

//			int keep = -1;
			String currprim = "h";
			for (int j=0; j<plan.getActsLegs().size(); j=j+2) {
				Act act = (Act)plan.getActsLegs().get(j);
//				act.setPrimary(false);
				String type = act.getType();
				if (((order.get(type))).intValue() <
						((order.get(currprim))).intValue()) {
					currprim = type;
//					keep = j;
				}
			}
//			((Act)plan.getActsLegs().get(keep)).setPrimary(true);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void print() {
	}
}
