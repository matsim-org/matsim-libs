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

import java.util.HashSet;
import java.util.Set;

import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.gsv.synPop.ProxyPlan;

/**
 * @author johannes
 * 
 */
public class DeletePlansDestination implements ProxyPersonTask {

	@Override
	public void apply(ProxyPerson person) {
		Set<ProxyPlan> remove = new HashSet<>();

		for (ProxyPlan plan : person.getPlans()) {
			if ("midjourneys".equalsIgnoreCase(plan.getAttribute("datasource"))) {
				for (Element leg : plan.getLegs()) {
					if (!JourneyDestinationHandler.GERMANY.equals(leg.getAttribute(JourneyDestinationHandler.DESTINATION))) {
						remove.add(plan);
					}
				}
			}
		}

		for (ProxyPlan plan : remove) {
			person.getPlans().remove(plan);
		}

	}

}
