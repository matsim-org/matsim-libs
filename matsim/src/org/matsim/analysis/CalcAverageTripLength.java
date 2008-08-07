/* *********************************************************************** *
 * project: org.matsim.*
 * CalcAverageTripLength.java
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

package org.matsim.analysis;

import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Route;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithmI;

public class CalcAverageTripLength extends PersonAlgorithm implements PlanAlgorithmI {

	private double sumLength = 0.0;
	private int cntTrips = 0;

	@Override
	public void run(final Person person) {
		this.run(person.getSelectedPlan());
	}

	public void run(final Plan plan) {
		for (int i = 2, max = plan.getActsLegs().size(); i < max; i += 2) {
			Act act = (Act) plan.getActsLegs().get(i);
			Leg leg = (Leg) plan.getActsLegs().get(i - 1);
			Route route = leg.getRoute();
			if (route != null) {
				double dist = route.getDist();
				if (act.getLink() != null) {
					dist += act.getLink().getLength();
				}
				this.sumLength += dist;
				this.cntTrips++;
			}
		}
	}

	public double getAverageTripLength() {
		if (this.cntTrips == 0) {
			return 0;
		}
		return (this.sumLength / this.cntTrips);
	}
}
