/* *********************************************************************** *
 * project: org.matsim.*
 * InsertParkingActivitiesReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withinday;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;

public class InsertParkingActivitiesReplanner extends WithinDayInitialReplanner {

	private PlanAlgorithm planAlgorithm;
	
	/*
	 * use a InitialIdentifierImpl and set handleAllAgents to true
	 */
	public InsertParkingActivitiesReplanner(Id id, Scenario scenario, PlanAlgorithm planAlgorithm) {
		super(id, scenario);
		
		this.planAlgorithm = planAlgorithm;
	}

	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {
		Plan executedPlan = withinDayAgent.getSelectedPlan();
		List<PlanElement> planElements = executedPlan.getPlanElements();
		
		/*
		 * TODO:
		 * changes to this plan will be executed but not written to the person
		 *  
		 * - select parking facility (e.g. nearest to origin and destination, assigned to location, ...)
		 * - reroute car leg
		 * - recalculate walk leg distance
		 */
		List<Integer> carLegIndices = new ArrayList<Integer>();
		int index = 0;
		for (PlanElement planElement : planElements) {
			if (planElement instanceof Leg) {
				if (((Leg) planElement).getMode().equals(TransportMode.car)) {
					carLegIndices.add(index);
				}
			}
			index++;
		}
		
		for (int i = carLegIndices.size(); i > 0; i--) {
			index = carLegIndices.get(i-1);
			
			PlanElement previousActivity = planElements.get(index - 1);
			PlanElement nextActivity = planElements.get(index + 1);
			
			if (!(previousActivity instanceof Activity)) throw new RuntimeException("Expected an activity before each car leg!");
			if (!(nextActivity instanceof Activity)) throw new RuntimeException("Expected an activity after each car leg!");
		
			planElements.add(index + 1, createWalkLeg());
			planElements.add(index + 1, createParkingActivity(((Activity) nextActivity).getLinkId()));
			planElements.add(index, createParkingActivity(((Activity) previousActivity).getLinkId()));
			planElements.add(index, createWalkLeg());
		}
		
		planAlgorithm.run(executedPlan);
		
		return true;
	}

	private Activity createParkingActivity(Id linkId) {
		ActivityImpl activity = (ActivityImpl) this.scenario.getPopulation().getFactory().createActivityFromLinkId("parking", linkId);
		activity.setMaximumDuration(0);
		activity.setCoord(this.scenario.getNetwork().getLinks().get(linkId).getCoord());
		return activity;
	}
	
	private Leg createWalkLeg() {
		return this.scenario.getPopulation().getFactory().createLeg(TransportMode.walk);
	}
}
