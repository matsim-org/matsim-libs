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
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class ParkingPlanAlgorithm implements PlanAlgorithm {

	public void run(final Plan plan) {

		// assign new parking at the work activity and change the plan accordingly (replace the parking activity before and after the parking)
		
		ActivityFacilityImpl newParking = (ActivityFacilityImpl) GlobalRegistry.controler.getFacilities().getFacilities().get(new IdImpl("35"));

		replaceParking(plan, 6, newParking, GlobalRegistry.controler, (NetworkLayer) GlobalRegistry.controler.getNetwork());
		
	
	}

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

		// make new parking activity activity
		
		ActivityImpl newParkingActivity = new ActivityImpl("parking", newParking.getCoord());
		newParkingActivity.setFacilityId(newParking.getId());
		newParkingActivity.setLinkId(network.getNearestLink(newParking.getCoord()).getId());
		// TODO: one should also be able to set this parameter from outside!!!!
		newParkingActivity.setDuration(60);

		// change the previous and next parking of the targetActivity

		plan.getPlanElements().remove(planElementIndexOfTargetActivity - 2);
		plan.getPlanElements().add(planElementIndexOfTargetActivity - 2, newParkingActivity);

		plan.getPlanElements().remove(planElementIndexOfTargetActivity + 2);
		plan.getPlanElements().add(planElementIndexOfTargetActivity + 2, newParkingActivity);

		// peform rerouting: from previous activity to parking

		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(0.0);
		leg.setArrivalTime(0.0);

		fromAct = (Activity) plan.getPlanElements().get(planElementIndexOfTargetActivity - 4);
		toAct = (Activity) plan.getPlanElements().get(planElementIndexOfTargetActivity - 2);

		PlansCalcRoute router = getRoutingAlgorithm(controler);
		router.handleLeg(plan.getPerson(), leg, fromAct, toAct, fromAct.getEndTime());

		// peform rerouting: from parking to next activity (after completion of targetActivity)
		
		fromAct = (Activity) plan.getPlanElements().get(planElementIndexOfTargetActivity + 2);
		toAct = (Activity) plan.getPlanElements().get(planElementIndexOfTargetActivity + 4);
		
		router = getRoutingAlgorithm(controler);
		router.handleLeg(plan.getPerson(), leg, fromAct, toAct, fromAct.getEndTime());

	}


	private static PlansCalcRoute getRoutingAlgorithm(Controler controler) {
		TravelTimeDistanceCostCalculator ttdcc = new TravelTimeDistanceCostCalculator(controler.getTravelTimeCalculator(),
				controler.getConfig().charyparNagelScoring());

		return new PlansCalcRoute(new PlansCalcRouteConfigGroup(), controler.getNetwork(),
				controler.createTravelCostCalculator(), controler.getTravelTimeCalculator(), new DijkstraFactory());
	}

}
