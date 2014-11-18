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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.gsv.synPop.ProxyPlan;

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
		Set<ProxyPlan> journeys = new HashSet<>();
		for (ProxyPlan p : person.getPlans()) {
			if ("midjourneys".equalsIgnoreCase(p.getAttribute("datasource"))) {
				journeys.add(p);
			}
		}

		for(ProxyPlan plan : journeys) {
			ProxyPlan returnPlan = plan.clone();
			Collections.reverse(returnPlan.getActivities());
			Collections.reverse(returnPlan.getLegs());

			person.addPlan(returnPlan);
		}
	}

}
