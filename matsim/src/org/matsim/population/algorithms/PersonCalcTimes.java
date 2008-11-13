/* *********************************************************************** *
 * project: org.matsim.*
 * PersonCalcTimes.java
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

package org.matsim.population.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.utils.misc.Time;

/**
 * Calculates all time informations in all plans of a person<br/>
 * The algorithm does the following steps:
 * <ul>
 * <li>set start-time of first act to 00:00</li>
 * <li>use end-time of first act as "current" time</li>
 * <li>sets start-time, duration, end-time of acts and departure-time,
 * travel-time, arrival-time of legs by continually adding duration of acts
 * and travel-time of legs to the current time.</li>
 * <li>the last act is extended until 24:00 if it ends before this time.
 * Otherwise, a duration of 0 is assumed for the last act.</li>
 * </ul>
 * If a leg has no travel-time set, 0 will be used instead.
 */
public class PersonCalcTimes extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonCalcTimes() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

  @Override
	public void run(Person person) {
		List<Plan> plans = person.getPlans();
		for (int i=0; i<plans.size(); i++) {
			Plan plan = plans.get(i);

			ArrayList acts_legs = plan.getActsLegs();
			Act act = null;
			Leg leg = null;
			for (int j=0; j<acts_legs.size(); j++) {
				if (j % 2 == 0) {
					act = (Act)acts_legs.get(j);

					if (j == 0) {
						act.setStartTime(0);
						act.setDuration(act.getEndTime());
					}
					else if (j == acts_legs.size()-1) {
						double time = leg.getArrivalTime();
						act.setStartTime(time);
						if (time < 24*3600) {
							time = 24*3600;
						}
						act.setEndTime(time);
						act.setDuration(time - act.getStartTime());
					}
					else {
						act.setStartTime(leg.getArrivalTime());
						act.setEndTime(act.getStartTime()+act.getDuration());
					}
				}
				else {
					leg = (Leg)acts_legs.get(j);

					leg.setDepartureTime(act.getEndTime());
					double ttime = leg.getTravelTime();
					if (ttime == Time.UNDEFINED_TIME) {
						ttime = 0;
					}
					leg.setArrivalTime(leg.getDepartureTime() + ttime);
				}
			}
		}
  }
}
