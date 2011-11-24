/* *********************************************************************** *
 * project: org.matsim.*
 * InsertParkingActivities.java
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;

public class InsertParkingActivities implements PlanAlgorithm {

	private final Scenario scenario;
	private final PersonAlgorithm personPrepareForSim;
	private final ParkingInfrastructure parkingInfrastructure;
	
	/*
	 * use a InitialIdentifierImpl and set handleAllAgents to true
	 */
	public InsertParkingActivities(Scenario scenario, PlanAlgorithm planAlgorithm, ParkingInfrastructure parkingInfrastructure) {
		this.scenario = scenario;		
		this.personPrepareForSim = new PersonPrepareForSim(planAlgorithm, (ScenarioImpl) scenario);
		this.parkingInfrastructure = parkingInfrastructure;
	}
	
	@Override
	public void run(Plan plan)  {
		List<PlanElement> planElements = plan.getPlanElements();
		
		/*
		 * Changes to this plan will be executed but not written to the person
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
		
		// if no car legs are performed, no adaption of the plan is necessary
		if (carLegIndices.size() == 0) return;
		
		for (int i = carLegIndices.size(); i > 0; i--) {
			index = carLegIndices.get(i-1);
			
			PlanElement previousActivity = planElements.get(index - 1);
			PlanElement nextActivity = planElements.get(index + 1);
			
			if (!(previousActivity instanceof Activity)) throw new RuntimeException("Expected an activity before each car leg!");
			if (!(nextActivity instanceof Activity)) throw new RuntimeException("Expected an activity after each car leg!");
			
			planElements.add(index + 1, createWalkLeg());
			planElements.add(index + 1, createParkingActivity(((Activity) nextActivity).getFacilityId()));
			planElements.add(index, createParkingActivity(((Activity) previousActivity).getFacilityId()));
			planElements.add(index, createWalkLeg());
		}
		
		/*
		 * Create initial routes for new legs
		 * 
		 * Since we only want the current plan to be prepared, we
		 * create a dummy person, add the plan, prepare this person
		 * and the reassign the plan to the person it really belongs to.
		 */
		Person person = plan.getPerson();
		Person dummyPerson = this.scenario.getPopulation().getFactory().createPerson(scenario.createId("dummy"));
		dummyPerson.addPlan(plan);
		personPrepareForSim.run(dummyPerson);
		plan.setPerson(person);
	}

	private Activity createParkingActivity(Id facilityId) {

		// get the facility where the activity is performed
		ActivityFacility facility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(facilityId);
		Id parkingFacilityId = this.parkingInfrastructure.getClosestFacilityFromCoord(facility.getCoord());
		
		// get the closest parking facility
		ActivityFacility parkingFacility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(parkingFacilityId);
		
		Id linkId = parkingFacility.getLinkId();
		ActivityImpl activity = (ActivityImpl) this.scenario.getPopulation().getFactory().createActivityFromLinkId("parking", linkId);
		activity.setMaximumDuration(180);
		activity.setCoord(parkingFacility.getCoord());
		return activity;
	}
	
	private Leg createWalkLeg() {
		return this.scenario.getPopulation().getFactory().createLeg(TransportMode.walk);
	}
	
}
