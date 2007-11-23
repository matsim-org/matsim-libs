/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAlgorithm.java
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
 *                          PersonAlgorithm.java                           *
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

import java.util.Date;
import java.util.Iterator;

import org.matsim.plans.Person;
import org.matsim.plans.Plans;

public abstract class PersonAlgorithm extends PlansAlgorithm implements PersonAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public PersonAlgorithm() {
	}

	//////////////////////////////////////////////////////////////////////
	// abstract methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final void run(Plans plans) {
		System.out.println("    running " + this.getClass().getName() + " algorithm..." + (new Date()));
		long counter = 0;
		long nextMsg = 1;

		Iterator<Person> it = plans.getPersons().values().iterator();
		long startTime = System.currentTimeMillis();
		while (it.hasNext()) {

			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				System.out.println(" person # " + counter + " (elapsed time: "
						+ (System.currentTimeMillis() - startTime)/1000 + " sec)");
			}

			Person p = it.next();
			this.run(p);
		}
		if (counter % nextMsg != 0) {
			System.out.println(" person # " + counter + " (elapsed time: "
					+ (System.currentTimeMillis() - startTime)/1000 + " sec)");
		}
		System.out.println("    done running algorithm. " + (new Date()));
	}

	public abstract void run(Person person);
}
