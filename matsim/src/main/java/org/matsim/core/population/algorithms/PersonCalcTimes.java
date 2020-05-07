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

package org.matsim.core.population.algorithms;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Calculates all time informations in all plans of a person<br>
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
public final class PersonCalcTimes extends AbstractPersonAlgorithm {

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
		List<? extends Plan> plans = person.getPlans();
		for (int i=0; i<plans.size(); i++) {
			Plan plan = plans.get(i);

			Activity act = null;
			Leg leg = null;
			int cnt = 0;
			int max = plan.getPlanElements().size();
			for (PlanElement pe : plan.getPlanElements()) {
				cnt++;
				if (pe instanceof Activity) {
					act = (Activity) pe;

					if (cnt == 1) {
						// first activity
						act.setStartTime(0);
						act.setMaximumDuration(act.getEndTime().seconds());
					} else if (cnt == max) {
						double time = leg.getDepartureTime().seconds() + leg.getTravelTime().seconds();
						act.setStartTime(time);
						if (time < 24*3600) {
							time = 24*3600;
						}
						act.setEndTime(time);
						act.setMaximumDuration(time - act.getStartTime().seconds());
					}
					else {
						act.setStartTime(leg.getDepartureTime().seconds() + leg.getTravelTime().seconds());
						act.setEndTime(act.getStartTime().seconds() + act.getMaximumDuration().seconds());
					}
				}
				if (pe instanceof Leg) {
					leg = (Leg) pe;

					leg.setDepartureTime(act.getEndTime().seconds());
					OptionalTime ttime = leg.getTravelTime();
					leg.setTravelTime( leg.getDepartureTime().seconds() + ttime.orElse(0) - leg.getDepartureTime()
							.seconds());
				}
			}
		}
  }
}
