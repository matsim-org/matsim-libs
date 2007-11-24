/* *********************************************************************** *
 * project: org.matsim.*
 * PlanReplaceLegModes.java
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

package org.matsim.plans.algorithms;

import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

/**
 * @author marcel
 * 
 * Replaces the leg modes in a single plan or in all plans of a person.
 * One can replace one or more leg modes at a time.
 */
public class PlanReplaceLegModes extends PersonAlgorithm implements PlanAlgorithmI {

	private String[] fromMode;
	private String[] toMode;
	
	public PlanReplaceLegModes(String from, String to) {
		fromMode = new String[1];
		fromMode[0] = from;
		toMode = new String[1];
		toMode[0] = to;
	}
	
	public PlanReplaceLegModes(String[] from, String to[]) {
		fromMode = from;
		toMode = to;
	}

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}

	public void run(Plan plan) {
		for (int i = 1, max = plan.getActsLegs().size(); i < max; i += 2) {
			Leg leg = (Leg)plan.getActsLegs().get(i);
			String mode = leg.getMode();
			for (int idx = 1; idx < fromMode.length; idx++) {
				if (fromMode[idx].equals(mode)) {
					leg.setMode(toMode[idx]);
				}
			}
		}
	}
}
