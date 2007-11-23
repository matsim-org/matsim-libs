/* *********************************************************************** *
 * project: org.matsim.*
 * PersonActChainGrouping.java
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
 *                       PersonActChainGrouping.java                       *
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
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.utils.identifiers.IdI;

public class PersonActChainGrouping extends PersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final TreeMap<String,TreeSet<IdI>> chaingroups = new TreeMap<String,TreeSet<IdI>>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonActChainGrouping() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(Person person) {

		if (person.getPlans().size() != 1) {
			Gbl.errorMsg("person id=" + person.getId() +
			 " does not have exactly one plan.");
		}

		Plan plan = person.getPlans().get(0);
		String chain = new String();
		for (int j = 0; j < plan.getActsLegs().size(); j = j + 2) {
			Act act = (Act)plan.getActsLegs().get(j);
			chain = chain.concat(act.getType().substring(0, 1));
		}

		if (!this.chaingroups.containsKey(chain)) {
			this.chaingroups.put(chain, new TreeSet<IdI>());
		}

		TreeSet<IdI> ts = this.chaingroups.get(chain);
		if (!ts.add(person.getId())) {
			Gbl.errorMsg("person id=" +
			             person.getId() + " is already in that TreeSet.");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void print() {
		Iterator<String> chain_it = this.chaingroups.keySet().iterator();
		System.out.println("----------------------------------------");
		while (chain_it.hasNext()) {
			String chain = chain_it.next();
			System.out.println(chain);

			TreeSet<IdI> ts = this.chaingroups.get(chain);
			Iterator<IdI> ts_it = ts.iterator();
			while (ts_it.hasNext()) {
				IdI id = ts_it.next();
				System.out.println(id);
			}
		}
		System.out.println("----------------------------------------");
	}
}
