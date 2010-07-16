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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.Reflection;
import playground.wrashid.parkingSearch.planLevel.ParkingGeneralLib;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.scoring.OrderedFacility;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingTimeInfo;

public class ParkingPlanAlgorithm implements PlanAlgorithm {

	public void run(final Plan plan) {

		// identify which parking improvement could render the most gain.
		// first avoid capacity constraint violations
		// thereafter try improvements on the other levels.

		// ===========select targetActivity for which the parking should be
		// changed==============
		// first select such, for which the capacity violation happened
		// if this is not the case select at random for improvement
		ActivityImpl targetActivity = null;

		LinkedList<ActivityImpl> targetActivitiesWithParkingCapacityViolations = ParkingRoot.getParkingOccupancyMaintainer()
				.getActivitiesWithParkingConstraintViolations(plan);

		if (targetActivitiesWithParkingCapacityViolations.size() != 0) {
			// if some parking capacities were violated by current plan, change
			// one of the parkings which
			// violated the plan.

			// just select one random parking to be changed.
			int index = MatsimRandom.getRandom().nextInt(targetActivitiesWithParkingCapacityViolations.size());
			targetActivity = targetActivitiesWithParkingCapacityViolations.get(index);
		} else {
			// if no parking capacity violation happened, then select a random
			// parking and try to improve it.

			LinkedList<ActivityImpl> parkingTargetActivities = ParkingGeneralLib.getParkingTargetActivities(plan);
			int index = MatsimRandom.getRandom().nextInt(parkingTargetActivities.size());
			targetActivity = parkingTargetActivities.get(index);
		}

		// =========for the selected target activity, score the parkings close
		// to the target ==========

		ArrayList<ActivityFacilityImpl> parkingSortedAccordingToScore = ParkingRoot.getRanking()
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
		ActivityFacilityImpl newParking = parkingSortedAccordingToScore.get(0);

		// =============================replaceParking===============

		replaceParking(plan, targetActivity, newParking, GlobalRegistry.controler,
				(NetworkLayer) GlobalRegistry.controler.getNetwork());
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
	public static void replaceParking(final Plan plan, ActivityImpl targetActivity, ActivityFacilityImpl newParking,
			Controler controler, NetworkLayer network) {
		// make new parking activity activity

		ActivityImpl newParkingActivity = createNewParkingActivity(newParking, network, ParkingRoot.getParkingActivityDuration()
				.getActivityDuration(newParking.getId(), plan.getPerson().getId()));

		// change the previous and next parking of the targetActivity

		if (isFirstOrLastActivity(plan, targetActivity)) {
			// for the first/last activity (usually home)

			ActivityImpl firstActivity = (ActivityImpl) plan.getPlanElements().get(0);
			ActivityImpl lastActivity = (ActivityImpl) plan.getPlanElements().get(plan.getPlanElements().size() - 1);

			// if first and last activity are not at the same location,
			// assumption made by parking is wrong.
			if (!firstActivity.getFacilityId().toString().equalsIgnoreCase(lastActivity.getFacilityId().toString())) {
				throw new Error("first and last activity must be at the same location.");
			}

			changeNextParking(plan, firstActivity, newParkingActivity);
			changePreviousParking(plan, lastActivity, newParkingActivity);
		} else {
			// for activities in between
			changePreviousParking(plan, targetActivity, newParkingActivity);
			changeNextParking(plan, targetActivity, newParkingActivity);
		}

		// peform rerouting

		PlansCalcRoute router = getRoutingAlgorithm(controler);
		router = getRoutingAlgorithm(controler);
		router.run(plan);
	}

	private static boolean isFirstOrLastActivity(final Plan plan, ActivityImpl targetActivity) {
		List<PlanElement> pe = plan.getPlanElements();
		return pe.indexOf(targetActivity) == 0 || pe.indexOf(targetActivity) == pe.size() - 1;
	}

	private static void changePreviousParking(final Plan plan, ActivityImpl targetActivity, ActivityImpl newParkingActivity) {
		int indexOfArrivalParkingAct = ParkingGeneralLib.getArrivalParkingActIndex(plan, targetActivity);
		plan.getPlanElements().remove(indexOfArrivalParkingAct);
		plan.getPlanElements().add(indexOfArrivalParkingAct, newParkingActivity);
	}

	private static void changeNextParking(final Plan plan, ActivityImpl targetActivity, ActivityImpl newParkingActivity) {
		int indexOfDepartingParkingAct = ParkingGeneralLib.getDepartureParkingActIndex(plan, targetActivity);
		plan.getPlanElements().remove(indexOfDepartingParkingAct);
		plan.getPlanElements().add(indexOfDepartingParkingAct, newParkingActivity);
	}

	private static ActivityImpl createNewParkingActivity(ActivityFacilityImpl newParking, NetworkLayer network,
			double parkingActivityDuration) {
		ActivityImpl newParkingActivity = new ActivityImpl("parking", newParking.getCoord());
		newParkingActivity.setFacilityId(newParking.getId());
		newParkingActivity.setLinkId(network.getNearestLink(newParking.getCoord()).getId());
		newParkingActivity.setDuration(parkingActivityDuration);

		return newParkingActivity;
	}

	private static PlansCalcRoute getRoutingAlgorithm(Controler controler) {
		TravelTime travelTime = controler.getTravelTimeCalculator();
		PersonalizableTravelCost travelCost = controler.getTravelCostCalculatorFactory().createTravelCostCalculator(travelTime,
				controler.getConfig().charyparNagelScoring());

		LeastCostPathCalculatorFactory leastCostFactory = controler.getLeastCostPathCalculatorFactory();

		return new PlansCalcRoute(controler.getConfig().plansCalcRoute(), controler.getNetwork(), travelCost, travelTime,
				leastCostFactory);
	}

}
