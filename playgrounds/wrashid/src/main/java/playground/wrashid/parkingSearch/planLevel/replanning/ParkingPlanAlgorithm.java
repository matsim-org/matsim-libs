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
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.Reflection;
import playground.wrashid.parkingSearch.planLevel.ParkingRoot;

public class ParkingPlanAlgorithm implements PlanAlgorithm {

	public void run(final Plan plan) {

		//if (GlobalRegistry.isTestingMode==true){
			
		//} else {
		
		ActivityFacilityImpl newParking = (ActivityFacilityImpl) GlobalRegistry.controler.getFacilities().getFacilities().get(new IdImpl("35"));

		replaceParking(plan, 6, newParking, GlobalRegistry.controler, (NetworkLayer) GlobalRegistry.controler.getNetwork());
		
		
		//}
		
		
		
		//assignClosestParking(plan);

		/*
		 * for (PlanElement pe : plan.getPlanElements()) { if (pe instanceof
		 * Activity) { Activity activity = (Activity) pe;
		 * activity.setEndTime(Time.parseTime("06:00:00")); return; // we only
		 * want to change the end time of the very first activity } }
		 */
	}

	/**
	 * 
	 * @param plan
	 */
	/*
	 * private void assignClosestParking(final Plan plan) { for (PlanElement pe
	 * : plan.getPlanElements()) { if (pe instanceof Activity) { Activity
	 * activity = (Activity) pe;
	 * 
	 * ActivityFacilityImpl activityFacility = (ActivityFacilityImpl)
	 * GlobalRegistry.controler.getFacilities()
	 * .getFacilities().get(activity.getFacilityId());
	 * 
	 * ActivityFacilityImpl newParking = ParkingRoot.getClosestParkingMatrix()
	 * .getClosestParkings(activityFacility.getCoord(), 1, 0).get(0);
	 * 
	 * int activityIndex=plan.getPlanElements().indexOf(activity);
	 * 
	 * if (!((Activity) pe).getType().equalsIgnoreCase("parking") &&
	 * activityIndex!=0) { replaceParking(plan, activityIndex, newParking); } }
	 * } }
	 */

	/**
	 * TODO: also handle case, when planElementIndexOfTargetActivity==0
	 * 
	 * replace the parking for the specified activity with the new parking.
	 * 
	 * 
	 * @param plan
	 * @param planElementIndex
	 */
	public static void replaceParking(final Plan plan, int planElementIndexOfTargetActivity, ActivityFacilityImpl newParking,
			Controler controler, NetworkLayer network) {
		Activity fromAct = null;
		Activity toAct = null;

		ActivityImpl newParkingActivity = new ActivityImpl("parking", newParking.getCoord());
		newParkingActivity.setFacilityId(newParking.getId());
		// TODO: one should also be able to set this!!!!
		newParkingActivity.setDuration(60);

		// change the previous and next parking of the activity

		plan.getPlanElements().remove(planElementIndexOfTargetActivity - 2);
		plan.getPlanElements().add(planElementIndexOfTargetActivity - 2, newParkingActivity);

		plan.getPlanElements().remove(planElementIndexOfTargetActivity + 2);
		plan.getPlanElements().add(planElementIndexOfTargetActivity + 2, newParkingActivity);

		// peform rerouting for route previous to parking

		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(0.0);
		leg.setArrivalTime(0.0);

		fromAct = (Activity) plan.getPlanElements().get(planElementIndexOfTargetActivity - 4);
		toAct = (Activity) plan.getPlanElements().get(planElementIndexOfTargetActivity - 2);

		// a hack: set uninitialzed links
		// setLink((ActivityImpl)fromAct,(IdImpl)
		// network.getNearestLink(fromAct.getCoord()).getId());
		// setLink((ActivityImpl)toAct,(IdImpl)
		// network.getNearestLink(toAct.getCoord()).getId());

		PlansCalcRoute router = getRoutingAlgorithm(controler);
		router.handleLeg(plan.getPerson(), leg, fromAct, toAct, fromAct.getEndTime());

		// peform rerouting for route after parking (when the route is
		// completed)

	}

	// was a hack...
	// private static void setLink(ActivityImpl activity, IdImpl linkId){
	// Reflection.setField(activity, "linkId", linkId);
	// }

	private static PlansCalcRoute getRoutingAlgorithm(Controler controler) {
		TravelTimeDistanceCostCalculator ttdcc = new TravelTimeDistanceCostCalculator(controler.getTravelTimeCalculator(),
				controler.getConfig().charyparNagelScoring());

		return new PlansCalcRoute(new PlansCalcRouteConfigGroup(), controler.getNetwork(),
				controler.createTravelCostCalculator(), controler.getTravelTimeCalculator(), new DijkstraFactory());
	}

	/**
	 * - possibly remove this. - replace all parkings with facility Id="36" to
	 * parkings with facility Id="2"
	 * 
	 * @param plan
	 */
	private void replaceParking(final Plan plan) {
		System.out.println(plan.getPerson().getId());

		ActivityImpl newParkingActivity = new ActivityImpl("parking", new CoordImpl(0, 1500));
		newParkingActivity.setFacilityId(new IdImpl("2"));
		newParkingActivity.setDuration(60);

		Activity activity = null;
		int index = 0;

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				activity = (Activity) pe;
				if (activity.getFacilityId().equals(new IdImpl("36")) && activity.getType().equalsIgnoreCase("parking")) {
					index = plan.getPlanElements().indexOf(activity);
					break;
				}
			}
		}

		plan.getPlanElements().remove(index);
		plan.getPlanElements().add(index, newParkingActivity);

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				activity = (Activity) pe;
				if (activity.getFacilityId().equals(new IdImpl("36")) && activity.getType().equalsIgnoreCase("parking")) {
					index = plan.getPlanElements().indexOf(activity);
					break;
				}
			}
		}

		plan.getPlanElements().remove(index);
		plan.getPlanElements().add(index, newParkingActivity);

		Controler controler = GlobalRegistry.controler;

		TravelTimeDistanceCostCalculator ttdcc = new TravelTimeDistanceCostCalculator(controler.getTravelTimeCalculator(),
				controler.getConfig().charyparNagelScoring());

		LeastCostPathCalculator lcpc = controler.getLeastCostPathCalculatorFactory().createPathCalculator(controler.getNetwork(),
				ttdcc, controler.getTravelTimeCalculator());

		NetworkFactoryImpl nfi = controler.getNetwork().getFactory();

		PlansCalcRoute dijkstraRouter = new PlansCalcRoute(new PlansCalcRouteConfigGroup(), controler.getNetwork(),
				controler.createTravelCostCalculator(), controler.getTravelTimeCalculator(), new DijkstraFactory());

		// dijkstraRouter.handleLeg(plan.getPerson(),
		// plan.getPlanElements().get(index) , fromAct, toAct, depTime)

		// need to ch

		// cont here!!!!
		// see class PlansCalcRoute, zeilen 254
		// make a method, which does it...

		// TODO: change parking of all people!!
	}

	private void assignNewRoute(final Plan plan, int leg) {

	}

	// remove this code, if not needed any more..
	// for all activities (not the first or last), assign the closest
	// parkings

	/*
	 * for (PlanElement pe : plan.getPlanElements()) { if (pe instanceof
	 * Activity) { Activity activity = (Activity) pe;
	 * 
	 * ActivityFacilityImpl activityFacility = (ActivityFacilityImpl)
	 * sc.getActivityFacilities
	 * ().getFacilities().get(activity.getFacilityId());
	 * 
	 * ActivityFacilityImpl newParking = ParkingRoot.getClosestParkingMatrix()
	 * .getClosestParkings(activityFacility.getCoord(), 1, 0).get(0);
	 * 
	 * int activityIndex=plan.getPlanElements().indexOf(activity);
	 * 
	 * if (!((Activity) pe).getType().equalsIgnoreCase("parking") &&
	 * activityIndex!=0 && activityIndex!=plan.getPlanElements().size()) {
	 * ParkingPlanAlgorithm.replaceParking(plan, activityIndex, newParking); } }
	 * }
	 */

}
