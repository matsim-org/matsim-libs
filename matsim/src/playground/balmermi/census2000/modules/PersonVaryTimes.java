/* *********************************************************************** *
 * project: org.matsim.*
 * PersonVaryTimes.java
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

package playground.balmermi.census2000.modules;

import java.util.ArrayList;

import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.misc.Time;

public class PersonVaryTimes extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonVaryTimes() {
		System.out.println("    init " + this.getClass().getName() + " module...");
		MatsimRandom.random.nextInt();
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		ArrayList<Object> acts_legs = person.getSelectedPlan().getActsLegs();

		double bias = MatsimRandom.random.nextInt(3600)-1800.0; // [-1800,1800[

		// draw a new random number until the new end time >= 0.0
		double first_end_time = ((Act)acts_legs.get(0)).getEndTime();
		while (first_end_time+bias < 0.0) {
			bias = MatsimRandom.random.nextInt(3600)-1800.0;
		}
		
		for (int i=0; i<acts_legs.size(); i++) {
			if (i % 2 == 0) {
				Act act = (Act)acts_legs.get(i);
				if (i == 0) { // first act
					act.setStartTime(0.0);
					act.setDur(act.getDur()+bias);
					act.setEndTime(act.getEndTime()+bias);
				}
				else if (i == acts_legs.size()-1) { // last act
					act.setStartTime(act.getStartTime()+bias);
					act.setDur(Time.UNDEFINED_TIME);
					act.setEndTime(Time.UNDEFINED_TIME);
				}
				else { // in between acts
					act.setStartTime(act.getStartTime()+bias);
					act.setEndTime(act.getEndTime()+bias);
				}
			}
			else {
				Leg leg = (Leg)acts_legs.get(i);
				leg.setDepartureTime(leg.getDepartureTime()+bias);
				leg.setArrivalTime(leg.getArrivalTime()+bias);
			}
		} 
	}

	public void run(Plan plan) {
	}
}
