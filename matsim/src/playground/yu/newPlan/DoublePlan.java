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

package playground.yu.newPlan;

import org.matsim.plans.Person;
import org.matsim.plans.Plans;


/**
 * increases the amount of Agents in a new MATSim plansfile, by copying the old
 * agents in the file and change only the Ids.
 * 
 * @author ychen
 * 
 */
public class DoublePlan extends NewAgentPtPlan {
	private int newPersonId;
	/**
	 * Construcktor
	 * 
	 * @param plans -
	 *            a Plans Object, which derives from MATSim plansfile
	 */
	public DoublePlan(Plans plans) {
		super(plans);
	}

	/**
	 * writes an old Person and also new Persons in new plansfile.
	 * 
	 * @see org.matsim.plans.algorithms.PersonAlgorithm#run(org.matsim.plans.Person)
	 */
	@Override
	public void run(Person person) {
		pw.writePerson(person);
		// produce new Person with bigger Id
		for (int i = 1; i <= 99; i++) {
			newPersonId = Integer.parseInt(person.getId().toString()) + 1000;
			person.setId(Integer.toString(newPersonId));
			pw.writePerson(person);
		}
	}
}
