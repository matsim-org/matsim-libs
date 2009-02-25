/* *********************************************************************** *
 * project: org.matsim.*
 * DoublePtPlan.java
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

package playground.yu.newPlans;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.population.Population;

/**
 * increases the amount of Agents in a new MATSim plansfile, by copying the old
 * agents in the file and change only the Ids.
 * 
 * @author ychen
 * 
 */
public class DoublePlan extends NewPlan {
	private int newPersonId;

	/**
	 * Construcktor
	 * 
	 * @param plans -
	 *            a Plans Object, which derives from MATSim plansfile
	 */
	public DoublePlan(Population plans) {
		super(plans);
	}

	/**
	 * writes an old Person and also new Persons in new plansfile.
	 * 
	 * @see org.matsim.population.algorithms.AbstractPersonAlgorithm#run(org.matsim.interfaces.core.v01.Person)
	 */
	@Override
	public void run(Person person) {
		pw.writePerson(person);
		// produce new Person with bigger Id
		for (int i = 0; i < 17; i++) {
			newPersonId = Integer.parseInt(person.getId().toString()) + 1000;
			person.setId(new IdImpl(newPersonId));
			pw.writePerson(person);
		}
	}
}
