/* *********************************************************************** *
 * project: org.matsim.*
 * SelectRiskyPlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.itsc08;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author illenberger
 *
 */
public class ForceSelectPlan implements PlanSelector {

	private Id linkId;
	
	public ForceSelectPlan(Id linkId) {
		this.linkId = linkId;
	}

	public Plan selectPlan(Person person) {
		Plan plan = null;
		for(Plan p : person.getPlans()) {
			LegImpl leg = (LegImpl) p.getPlanElements().get(1);
			if(((NetworkRouteWRefs) leg.getRoute()).getLinkIds().contains(linkId)) {
				plan = p;
				break;
			}
		}
		return plan;
	}

}
