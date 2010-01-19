/* *********************************************************************** *
 * project: org.matsim.*
 * PersonFacility2Link
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.balmermi.modules;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonResetCoordAndLink extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private final ActivityFacilities facilities;
	
	public PersonResetCoordAndLink(final ActivityFacilities facilities) {
		this.facilities = facilities;
	}

	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}

	public void run(final Plan plan) {
		for (int i=0; i<plan.getPlanElements().size(); i++) {
			if (i%2==0) {
				ActivityImpl a = (ActivityImpl)plan.getPlanElements().get(i);
				a.setCoord(this.facilities.getFacilities().get(a.getFacilityId()).getCoord());
				a.setLink(((ActivityFacilityImpl) this.facilities.getFacilities().get(a.getFacilityId())).getLink());
			}
			else {
				LegImpl l = (LegImpl)plan.getPlanElements().get(i);
				l.setArrivalTime(l.getDepartureTime());
				l.setTravelTime(0.0);
				l.setRoute(null);
			}
		}
	}
}
