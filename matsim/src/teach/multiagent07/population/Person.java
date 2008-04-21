/* *********************************************************************** *
 * project: org.matsim.*
 * Person.java
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

package teach.multiagent07.population;

import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicPersonImpl;
import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.basic.v01.IdImpl;

public class Person extends BasicPersonImpl<Plan> {

	public Person(IdImpl id) {
		super(id);
	}

	public Person(String id) {
		super(id);
	}

	boolean isTesting = false;

	public void testAndExchange() {
		Plan selected = getSelectedPlan();

		if (isTesting) {
			Plan best = plans.get(plans.size()-1);
			// if this run scored better exchange old best against this plan
			if (selected.getScore() > best.getScore()) {
				plans.remove(plans.size()-1);
				plans.add(new Plan(selected));
			}
			isTesting = false;
			
		} else if (Math.random() < 0.25) { // 25% of 80% == 20% of all!!
			// make copy of original plan on position "plans.size()"
			plans.add(new Plan(selected));
			
			// try another value for starttime
			final double offsetInSec = 600;
			BasicPlanImpl.ActLegIterator it = selected.getIterator() ;
			BasicAct startAct = it.nextAct();
			startAct.setEndTime(startAct.getEndTime() + offsetInSec*(Math.random()-0.5));
			
			isTesting = true;
		}
	}
}
