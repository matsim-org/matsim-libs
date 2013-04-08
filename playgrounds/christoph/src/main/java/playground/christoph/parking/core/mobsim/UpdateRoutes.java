/* *********************************************************************** *
 * project: org.matsim.*
 * UpdateRoutes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.parking.core.mobsim;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.vehicles.Vehicle;

import playground.christoph.parking.withinday.utils.ParkingRouter;

/**
 * Updates plans created by InsertPlanActivities. There, additional walk legs
 * and parking activities are added to an agent's plan. This class adapts
 * the routes and ensures that they are valid.
 * 
 * Car trips that start and end on the same link are removed as well as
 * the parking activities that belong to them. The walk legs to the parking
 * activities are merged.
 * 
 * @author cdobler
 */
public class UpdateRoutes implements PlanAlgorithm {

	private final ParkingRouter parkingRouter;
	
	public UpdateRoutes(ParkingRouter parkingRouter) {
		this.parkingRouter = parkingRouter;
	}
	
	@Override
	public void run(Plan plan) {
		
		List<PlanElement> planElements = plan.getPlanElements();

		/*
		 * Changes to this plan will be executed but not written to the person
		 */
		List<Integer> carLegIndices = new ArrayList<Integer>();
		List<PlanElement> planElementsToRemove = new ArrayList<PlanElement>();
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
		
		for (int i : carLegIndices) {
			Activity fromActivity = (Activity) plan.getPlanElements().get(i - 3);
			Leg previousWalkLeg = (Leg) plan.getPlanElements().get(i - 2);
			Activity previousParkingActivity = (Activity) plan.getPlanElements().get(i - 1);
			Leg carLeg = (Leg) plan.getPlanElements().get(i);
			Activity nextParkingActivity = (Activity) plan.getPlanElements().get(i + 1);
			Leg nextWalkLeg = (Leg) plan.getPlanElements().get(i + 2);
			Activity toActivity = (Activity) plan.getPlanElements().get(i + 3);
			
			/*
			 * Check whether the car leg ends at the same parking facility as it starts.
			 * If it does, the car trip and the parking activities can be removed from the plan.
			 */
			if (previousParkingActivity.getFacilityId().equals(nextParkingActivity.getFacilityId())) {
				
				planElementsToRemove.add(previousParkingActivity);
				planElementsToRemove.add(carLeg);
				planElementsToRemove.add(nextParkingActivity);
				planElementsToRemove.add(nextWalkLeg);
				
				// update end link of walk route and then update route
				previousWalkLeg.getRoute().setEndLinkId(nextWalkLeg.getRoute().getEndLinkId());
				updateWalkLeg(fromActivity, previousWalkLeg, toActivity, plan.getPerson());
				
			} else {
				updateWalkLeg(fromActivity, previousWalkLeg, previousParkingActivity, plan.getPerson());
				updateCarLeg(previousParkingActivity, carLeg, nextParkingActivity, plan.getPerson());
				updateWalkLeg(nextParkingActivity, nextWalkLeg, toActivity, plan.getPerson());				
			}	
		}
		
		// remove planElements which are not required anymore
		planElements.removeAll(planElementsToRemove);
	}
	
	private void updateCarLeg(Activity fromActivity, Leg carLeg, Activity toActivity, Person person) {
		
		NetworkRoute route = (NetworkRoute) carLeg.getRoute();
		Vehicle vehicle = null;
		this.parkingRouter.adaptStartAndEndOfRoute(route, fromActivity.getLinkId(), toActivity.getLinkId(), 
				carLeg.getDepartureTime(), person, vehicle, TransportMode.car);
	}
	
	private void updateWalkLeg(Activity fromActivity, Leg walkLeg, Activity toActivity, Person person) {
		this.parkingRouter.updateWalkRoute(fromActivity, walkLeg, toActivity, person);
	}

}