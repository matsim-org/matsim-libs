/* *********************************************************************** *
 * project: org.matsim.*
 * CurrentLegReplanner.java
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

package playground.christoph.withinday.replanning.replanners;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.ptproject.qsim.agents.DefaultPersonDriverAgent;

import playground.christoph.withinday.replanning.replanners.CurrentLegReplanner;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import playground.christoph.withinday.utils.EditRoutes;

/*
 * The CurrentLegReplanner can be used while an Agent travels from
 * one Activity to another one.
 *
 * MATSim Routers use Plan as Input Data. To be able to use them, we have to create
 * a new Plan from the current Position to the location of the next Activity.
 *
 * This Replanner is called, if a person is somewhere on a Route between two Activities.
 * First the current Route is splitted into two parts - the already passed links and
 * the ones which are still to go.
 * Next a new Plan is created with an Activity at the current Position and an Endposition
 * that is equal to the one from the original plan.
 * This Plan is handed over to the Router and finally the new route is merged with the
 * Links that already have been passed by the Person.
 */
public class CurrentLegReplanner extends WithinDayDuringLegReplanner {

	/*package*/ CurrentLegReplanner(Id id, Scenario scenario) {
		super(id, scenario);
	}

	/*
	 * Replan Route every time the End of a Link is reached.
	 *
	 * Idea:
	 * - create a new Activity at the current Location
	 * - create a new Route from the current Location to the Destination
	 * - merge already passed parts of the current Route with the new created Route
	 */
	@Override
	public boolean doReplanning(PersonAgent personAgent) {
		
		// If we don't have a valid Replanner.
		if (this.routeAlgo == null) return false;

		// If we don't have a valid PersonAgent
		if (personAgent == null) return false;

		Person person = personAgent.getPerson();
		Plan selectedPlan = person.getSelectedPlan();

		// If we don't have a selected plan
		if (selectedPlan == null) return false;

		Leg currentLeg = personAgent.getCurrentLeg();

		// If it is not a car Leg we don't replan it.
//		if (!currentLeg.getMode().equals(TransportMode.car)) return false;

		int currentNodeIndex = -1;
		if (personAgent instanceof DefaultPersonDriverAgent) {
			currentNodeIndex = ((DefaultPersonDriverAgent) personAgent).getCurrentNodeIndex();
		} else return false;
		
		// new Route for current Leg
		new EditRoutes().replanCurrentLegRoute(selectedPlan, currentLeg, currentNodeIndex, routeAlgo, scenario.getNetwork(), time);

		// Finally reset the cached Values of the PersonAgent - they may have changed!
		personAgent.resetCaches();
		
//		// create ReplanningEvent
//		QSim.getEvents().processEvent(new ExtendedAgentReplanEventImpl(time, person.getId(), newRoute, route));

		return true;
	}

}