/* *********************************************************************** *
 * project: org.matsim.*
 * MyPlansSelector.java
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
package tutorial.programming.example11PluggablePlanStrategyInCode;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author nagel
 */
class MyPlanSelector implements PlanSelector<Plan, Person>, ActivityEndEventHandler
{
	private static final Logger log = Logger.getLogger(MyPlanSelector.class);

	@Override
	public Plan selectPlan(HasPlansAndId<Plan, Person> person) {
		log.error("calling selectPlan: PesonId: " + person.getId() + " Returning first Plan") ;
		//returning first plan available. In real live the selection will be more sophisticated
		return person.getPlans().get(0);
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		//log.error("calling handleEvent for an ActivityEndEvent") ;
	}

	@Override
	public void reset(int iteration) {
		log.error("calling reset") ;
	}

}
