/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.mid;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.synpop.data.Episode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class AddReturnPlan implements ProxyPersonTask {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.johannes.gsv.synPop.ProxyPersonTask#apply(playground.johannes
	 * .gsv.synPop.ProxyPerson)
	 */
	@Override
	public void apply(ProxyPerson person) {
		Set<Episode> journeys = new HashSet<>();
		for (Episode p : person.getPlans()) {
			if ("midjourneys".equalsIgnoreCase(p.getAttribute("datasource"))) {
				journeys.add(p);
			}
		}

		for(Episode plan : journeys) {
			Episode returnPlan = ((ProxyPlan)plan).clone();
			Collections.reverse(returnPlan.getActivities());
			Collections.reverse(returnPlan.getLegs());

			person.addPlan(returnPlan);
		}
	}

}
