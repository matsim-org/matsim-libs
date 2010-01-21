/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveWorstPlanSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.yu.replanning.selectors;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author yu
 * 
 */
public class RemoveWorstPlanSelector implements PlanSelector {
	private PlanSelector removalPlanSelector = new WorstPlanForRemovalSelector();
	private List<Tuple<Id, Plan>> removeds = new ArrayList<Tuple<Id, Plan>>();

	public List<Tuple<Id, Plan>> getRemoveds() {
		// System.out.println(">>>>>\tremoveds:\t" + removeds);
		return removeds;
	}

	public Plan selectPlan(Person person) {
		Plan worstPlan = this.removalPlanSelector.selectPlan(person);
		removeds.add(new Tuple<Id, Plan>(person.getId(), worstPlan));
		return worstPlan;
	}
}
