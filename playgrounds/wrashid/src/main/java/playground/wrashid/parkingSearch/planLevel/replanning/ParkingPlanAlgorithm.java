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

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.wrashid.lib.GlobalRegistry;

public class ParkingPlanAlgorithm implements PlanAlgorithm {

	public void run(final Plan plan) {
		
		replaceParking(plan);
		
		/*
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity activity = (Activity) pe;
				activity.setEndTime(Time.parseTime("06:00:00"));
				return; // we only want to change the end time of the very first activity
			}
		}
		*/
	}
	
	
	/**
	 * - replace all parkings with facility Id="36" to parkings with facility Id="2"
	 * 
	 * @param plan
	 */
	private void replaceParking(final Plan plan){
		System.out.println(plan.getPerson().getId());
		
		
		ActivityImpl newParkingActivity=new ActivityImpl("parking",new CoordImpl(0, 1500));
		newParkingActivity.setFacilityId(new IdImpl("2"));
		newParkingActivity.setDuration(60);
		
		Activity activity = null;
		int index=0;
		
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				activity = (Activity) pe;
				if (activity.getFacilityId().equals(new IdImpl("36")) && activity.getType().equalsIgnoreCase("parking")){
					index=plan.getPlanElements().indexOf(activity);
					break;
				}
			}
		}
		
		plan.getPlanElements().remove(index);
		plan.getPlanElements().add(index,newParkingActivity);
		
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				activity = (Activity) pe;
				if (activity.getFacilityId().equals(new IdImpl("36")) && activity.getType().equalsIgnoreCase("parking")){
					index=plan.getPlanElements().indexOf(activity);
					break;
				}
			}
		}
		
		plan.getPlanElements().remove(index);
		plan.getPlanElements().add(index,newParkingActivity);
		
		Controler controler=GlobalRegistry.controler;
		
		TravelTimeDistanceCostCalculator ttdcc=new TravelTimeDistanceCostCalculator(controler.getTravelTimeCalculator(),controler.getConfig().charyparNagelScoring());
		
		LeastCostPathCalculator lcpc= controler.getLeastCostPathCalculatorFactory().createPathCalculator(controler.getNetwork(), ttdcc , controler.getTravelTimeCalculator());
		
		NetworkFactoryImpl nfi=controler.getNetwork().getFactory();
		
		
		PlansCalcRoute dijkstraRouter = new PlansCalcRoute(new PlansCalcRouteConfigGroup(), controler.getNetwork(), controler.createTravelCostCalculator(), controler.getTravelTimeCalculator(), new DijkstraFactory());
		
		//dijkstraRouter.handleLeg(plan.getPerson(), plan.getPlanElements().get(index) , fromAct, toAct, depTime)
		
		// need to ch
		
		
		// cont here!!!!
		// see class PlansCalcRoute, zeilen 254
		// make a method, which does it...
		
		
		// TODO: change parking of all people!!
	}
	
	private void assignNewRoute(final Plan plan, int leg){
		
	}
	

}
