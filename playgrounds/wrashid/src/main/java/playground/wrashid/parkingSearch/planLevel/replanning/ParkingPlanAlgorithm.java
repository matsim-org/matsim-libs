/* *********************************************************************** *
 * project: org.matsim.*
 * TemplatePlanAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.planLevel.replanning;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.PlanRouter;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.parkingSearch.planLevel.ParkingGeneralLib;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ParkingPlanAlgorithm implements PlanAlgorithm {

	@Override
	public void run(final Plan plan) {
		
		// reason, that we do this: all the statistics regarding parking are based on the previous iteration
		// if the activity chain structure of the given plan in this method and the plan of the last iteration
		// is fundamentally different, the algorithm here will not work.
		// TODO: put somewhere the last plan!!!
		
		
		
		Plan lastIterationPlan=ParkingRoot.getParkingOccupancyMaintainer().getLastSelectedPlan().get(plan.getPerson().getId()); // load plan...

		// Replace the plan elements of proposed plan
		plan.getPlanElements().clear();
		plan.getPlanElements().addAll(lastIterationPlan.getPlanElements());
		
		// probably it makes since for the parking replanning, only to chang the parking and not too much...
		// 
		
		
		// I think taking the same plan as in previous iteration and changing it makes most since, because the structural changes
		// can happen, with the other strategy modules anyway!
		
		
		
		// TODO: use this lastIterationPlan instead of just plan variable in this method!!!!
		
		// TODO: set the pe elements of this into the plan method!!!
		
		
		// TODO: alternative: if we find out, that the structure of the plan has changed fundamentally, 
		// we do nothing for parking and else if the strcture is similar to the last one, we do something. 
		// 
		
		
		// identify which parking improvement could render the most gain.
		// first avoid capacity constraint violations
		// thereafter try improvements on the other levels.

		// ===========select targetActivity for which the parking should be
		// changed==============
		// first select such, for which the capacity violation happened
		// if this is not the case select at random for improvement
		Activity targetActivity = null;

		LinkedList<Activity> targetActivitiesWithParkingCapacityViolations = ParkingRoot.getParkingOccupancyMaintainer()
				.getActivitiesWithParkingConstraintViolations(plan);

		if (targetActivitiesWithParkingCapacityViolations.size() != 0) {
		//if (targetActivitiesWithParkingCapacityViolations.size() < 0) {
			// if some parking capacities were violated by current plan, change
			// one of the parkings which
			// violated the plan.

			// just select one random parking to be changed.
			int index = MatsimRandom.getRandom().nextInt(targetActivitiesWithParkingCapacityViolations.size());
			targetActivity = targetActivitiesWithParkingCapacityViolations.get(index);
		} else {
			// if no parking capacity violation happened, then select a random
			// parking and try to improve it.

			LinkedList<Activity> parkingTargetActivities = ParkingGeneralLib.getParkingTargetActivities(plan);
			int index = MatsimRandom.getRandom().nextInt(parkingTargetActivities.size());
			targetActivity = parkingTargetActivities.get(index);
		}

		// =========for the selected target activity, score the parkings close
		// to the target ==========

		ArrayList<ActivityFacilityImpl> parkingSortedAccordingToScore = ParkingRoot.getParkingScoringFunction()
				.getParkingsOrderedAccordingToScore(targetActivity, plan);
		//

		// ===============then we select the parking (best or
		// probability)================
		// we could either just select the best parking or
		// we can select the parkings according to probabilities (the higher the
		// score the higher the
		// probabilities, we can argue, that a person does not just take the
		// best parking but one among the best
		// therefore we have such an uncertainty.
		// we could compare the two results(by implementing both variants).

		// TODO-intime: also add the probability method.

		// select parking with highest score
		//if (parkingSortedAccordingToScore.size()==0){
			//System.out.println();
		//}
		ActivityFacilityImpl newParking = parkingSortedAccordingToScore.get(0);

		// =============================replaceParking===============		
		
		//ParkingGeneralLib.printAllParkingFacilityIds(plan);

        replaceParking(plan, targetActivity, newParking, GlobalRegistry.controler,
				(Network) GlobalRegistry.controler.getScenario().getNetwork());
		
		//ParkingGeneralLib.printAllParkingFacilityIds(plan);
		
				
	}

	/**
	 * 
	 * replace the parking for the specified activity with the new parking. (the
	 * first/last activity location in the plan must be at the same faciliy).
	 * 
	 * @param plan
	 * @param targetActivity
	 * @param newParking
	 * @param controler
	 * @param network
	 */
	public static void replaceParking(final Plan plan, Activity targetActivity, ActivityFacilityImpl newParking,
			MatsimServices controler, Network network) {
		// make new parking activity activity

		Activity newParkingActivity = createNewParkingActivity(newParking, network, ParkingRoot.getParkingActivityDuration()
				.getActivityDuration(newParking.getId(), plan.getPerson().getId()));

		// change the previous and next parking of the targetActivity

		if (isFirstOrLastActivity(plan, targetActivity)) {
			// for the first/last activity (usually home)

			Activity firstActivity = (Activity) plan.getPlanElements().get(0);
			Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);

			// if the plan intended to be used without facilities, the facility Id of the activities will be 
			// missing, therefore it is set here.
			//if (firstActivity==null || lastActivity==null || firstActivity.getFacilityId()==null || lastActivity.getFacilityId()==null){
				//firstActivity.setFacilityId(facilityId)
			//}
			
			// if first and last activity are not at the same location,
			// assumption made by parking is wrong.
			if (!firstActivity.getLinkId().toString().equalsIgnoreCase(lastActivity.getLinkId().toString())) {
				throw new Error("first and last activity must be at the same location.");
			}

			changeNextParking(plan, firstActivity, newParkingActivity);
			
			newParkingActivity = createNewParkingActivity(newParking, network, ParkingRoot.getParkingActivityDuration()
					.getActivityDuration(newParking.getId(), plan.getPerson().getId()));
			
			changePreviousParking(plan, lastActivity, newParkingActivity);
		} else {
			// for activities in between
			changePreviousParking(plan, targetActivity, newParkingActivity);
			
			newParkingActivity = createNewParkingActivity(newParking, network, ParkingRoot.getParkingActivityDuration()
					.getActivityDuration(newParking.getId(), plan.getPerson().getId()));
			
			changeNextParking(plan, targetActivity, newParkingActivity);
		}

		// peform rerouting

		PlanAlgorithm router = getRoutingAlgorithm(controler);
		router.run(plan);
	}

	private static boolean isFirstOrLastActivity(final Plan plan, Activity targetActivity) {
		List<PlanElement> pe = plan.getPlanElements();
		return pe.indexOf(targetActivity) == 0 || pe.indexOf(targetActivity) == pe.size() - 1;
	}

	private static void changePreviousParking(final Plan plan, Activity targetActivity, Activity newParkingActivity) {
		int indexOfArrivalParkingAct = ParkingGeneralLib.getArrivalParkingActIndex(plan, targetActivity);
		plan.getPlanElements().remove(indexOfArrivalParkingAct);
		plan.getPlanElements().add(indexOfArrivalParkingAct, newParkingActivity);
	}

	private static void changeNextParking(final Plan plan, Activity targetActivity, Activity newParkingActivity) {
		int indexOfDepartingParkingAct = ParkingGeneralLib.getDepartureParkingActIndex(plan, targetActivity);
		plan.getPlanElements().remove(indexOfDepartingParkingAct);
		plan.getPlanElements().add(indexOfDepartingParkingAct, newParkingActivity);
	}

	private static Activity createNewParkingActivity(ActivityFacilityImpl newParking, Network network,
			double parkingActivityDuration) {
		Activity newParkingActivity = PopulationUtils.createActivityFromCoord("parking", newParking.getCoord());
		newParkingActivity.setFacilityId(newParking.getId());
		newParkingActivity.setLinkId(NetworkUtils.getNearestLink(network, newParking.getCoord()).getId());
		newParkingActivity.setMaximumDuration(parkingActivityDuration);

		return newParkingActivity;
	}

	private static PlanAlgorithm getRoutingAlgorithm(MatsimServices controler) {
		return new PlanRouter( controler.getTripRouterProvider().get() );
	}

}
