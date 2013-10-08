/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.taste_variations;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.ExpTransRouteUtils;

public class IndividualPreferencesLegScoring  implements BasicScoring, ScoringFunction {
	private final Network network;
	private final IndividualPreferences svdValues;
	protected double score;
	//private static final double INITIAL_SCORE = 0.0;
	private Plan plan; 
	private final TransitSchedule schedule;
	private double scoreWeight =1.0;
	
	public IndividualPreferencesLegScoring(final Plan plan, final IndividualPreferences svdValues, final Network network, final TransitSchedule schedule, final double scoreWeight) {
		this.plan = plan;
		this.schedule = schedule;
		this.network = network;
		this.svdValues = svdValues;
		this.scoreWeight = scoreWeight;
	}
	
	@Override
	public void finish() {
	}

	@Override
	public double getScore() {
		double tmpScore =0.0;
		String lastActType=null;
		String lastLegMode = null;

		for (PlanElement pe : this.plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				
				if (TransportMode.pt.equals(leg.getMode())) {  
					if (leg.getRoute()!= null && (leg.getRoute() instanceof org.matsim.api.core.v01.population.Route)){
						ExperimentalTransitRoute expRoute = (ExperimentalTransitRoute)leg.getRoute();
						ExpTransRouteUtils routUtil = new ExpTransRouteUtils(network, schedule, expRoute);
						double distance = routUtil.getExpRouteDistance();
						//double trTime = routUtil.getScheduledTravelTime(); <- CAUTION: travel time based on scheduled travel time!
						double trTime =	leg.getTravelTime();  //according to playground.mmoyo.analysis.tools. this might include the waiting time at stop						   
						trTime= routUtil.getScheduledTravelTime();  //with this version was svd-scored the first time
						
						tmpScore += distance *  svdValues.getWeight_trDistance();
						tmpScore += trTime *  svdValues.getWeight_trTime();

					}
				}else if (TransportMode.transit_walk.equals(leg.getMode())) {
					double trWalkTime = leg.getTravelTime();

					tmpScore += trWalkTime *  svdValues.getWeight_trWalkTime();
					
				}
				lastLegMode = leg.getMode();
			
			}else	if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if (act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE) && lastActType.equals(PtConstants.TRANSIT_ACTIVITY_TYPE) && lastLegMode.equals(TransportMode.transit_walk)){
					
					tmpScore += (1 *  svdValues.getWeight_changes());
				
				}
				lastActType=act.getType();
			}
		}
		score = tmpScore * scoreWeight;
		return score;
		
	}

	@Override
	public void reset() {
		this.score = 0.0;
	}

	
	//////////////////////////////// 	ScoringFunction methods  ///////////////////////
	@Override
	public void handleActivity(Activity activity) {
	}

	@Override
	public void handleLeg(Leg leg) {
	}

	@Override
	public void agentStuck(double time) {
	}

	@Override
	public void addMoney(double amount) {
	}

	@Override
	public void handleEvent(Event event) {
	}
	
}