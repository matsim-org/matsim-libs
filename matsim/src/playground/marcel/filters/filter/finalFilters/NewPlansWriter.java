/* *********************************************************************** *
 * project: org.matsim.*
 * NewPlansWriter.java
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

package playground.marcel.filters.filter.finalFilters;

import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;

/**
 * Write a new plans-file(.xml), while reading an old plans-file(.xml). eine neue "abgenommene" PlansDatei(.xml) schreiben.
 * @author  ychen
 */
public class NewPlansWriter extends FinalPersonFilter {
	/**
	 * The underlying PlansWriter of this NewPlansWriter.
	 */
	private PlansWriter plansWriter;

	// ------------------------CONSTRUCTOR----------------------
	/**
	 * initialize a NewPlansWriter: create a new PlansWriter; write the head of
	 * a plans-file(.xml).
	 *
	 * @param plans -
	 *            Parameter for constructor of PlansWriter.
	 */
	public NewPlansWriter(Plans plans) {
		this.plansWriter = new PlansWriter(plans);
		this.plansWriter.writeStartPlans();
	}

	/**
	 * Write person-block (Plan, Act, Leg, Route...) into plans-file(.xml).
	 *
	 * @param person -
	 *            a Person-object transfered from another PersonFilter
	 */
	@Override
	public void run(Person person) {
		if (person != null)
			this.plansWriter.writePerson(person);
	}

	/**
	 * Writes end-block of a plans-file(.xml).
	 */
	public void writeEndPlans() {
		this.plansWriter.writeEndPlans();
	}
}
