/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.locationchoice.bestresponse;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.locationchoice.timegeography.RecursiveLocationMutator;
import org.matsim.locationchoice.utils.ActTypeConverter;
import org.matsim.locationchoice.utils.PlanUtils;
import org.matsim.locationchoice.utils.QuadTreeRing;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class BestResponseLocationMutator extends RecursiveLocationMutator {
	private ActivityFacilitiesImpl facilities;
	private Network network;
	private ObjectAttributes personsMaxEpsUnscaled;
	private ScaleEpsilon scaleEpsilon;
	private ActTypeConverter actTypeConverter;
	private DestinationSampler sampler;
	
	/* TODO: Use Singleton pattern or encapsulating object for the many constructor objects
	 * Similar to Scenario object
	 */
	public BestResponseLocationMutator(final Network network, Controler controler,
			TreeMap<String, QuadTreeRing<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacilityImpl []> facilities_of_type,
			ObjectAttributes personsMaxEpsUnscaled, ScaleEpsilon scaleEpsilon,
			ActTypeConverter actTypeConverter, DestinationSampler sampler) {
		super(network, controler, quad_trees, facilities_of_type, null);
		facilities = (ActivityFacilitiesImpl) super.controler.getFacilities();
		this.network = network;
		this.personsMaxEpsUnscaled = personsMaxEpsUnscaled;
		this.scaleEpsilon = scaleEpsilon;
		this.actTypeConverter = actTypeConverter;
		this.sampler = sampler;
	}
	
	@Override
	public void handlePlan(final Plan plan){
		// if person is not in the analysis population
		if (Integer.parseInt(plan.getPerson().getId().toString()) > 
			Integer.parseInt(super.controler.getConfig().locationchoice().getIdExclusion())) return;
				
		Plan bestPlan = new PlanImpl(plan.getPerson());	
		// make sure there is a valid plan in bestPlan
		((PlanImpl)bestPlan).copyPlan(plan);
		
		this.handleActivities(plan, bestPlan);
		
		// copy the best plan into replanned plan
		// making a deep copy
		PlanUtils.copyPlanFields((PlanImpl)plan, (PlanImpl)bestPlan);
		super.resetRoutes(plan);
	}
				
	private void handleActivities(Plan plan, Plan bestPlan) {
		int travelTimeApproximationLevel = Integer.parseInt(
				this.controler.getConfig().locationchoice().getTravelTimeApproximationLevel());
		
		// somehow getting it from the controler causes problems for parallel runs.
		// -> create a new (but identical) scorer here
		//ScoringFunctionAccumulator scorer = (ScoringFunctionAccumulator) this.controler.getPlansScoring().getPlanScorer().getScoringFunctionForAgent(plan.getPerson().getId());
		ScoringFunctionAccumulator scoringFunction = (ScoringFunctionAccumulator) this.controler.getScoringFunctionFactory().createNewScoringFunction(plan); 
		
		int actlegIndex = -1;
		for (PlanElement pe : plan.getPlanElements()) {
			actlegIndex++;
			if (pe instanceof Activity) {
				String actType = ((ActivityImpl)plan.getPlanElements().get(actlegIndex)).getType();
				if (actlegIndex > 0 && this.scaleEpsilon.isFlexibleType(actType)) {
					
					List<? extends PlanElement> actslegs = plan.getPlanElements();
					final Activity actToMove = (Activity)pe;
					final Activity actPre = (Activity)actslegs.get(actlegIndex - 2);
										
					final Activity actPost = (Activity)actslegs.get(actlegIndex + 2);					
					double distanceDirect = ((CoordImpl)actPre.getCoord()).calcDistance(actPost.getCoord());
					double distanceFromEpsilon = this.getMaximumDistanceFromEpsilon((PersonImpl)plan.getPerson(), actToMove.getType());
					double maxRadius = (distanceDirect +  distanceFromEpsilon) / 2.0;
					
					double x = (actPre.getCoord().getX() + actPost.getCoord().getX()) / 2.0;
					double y = (actPre.getCoord().getY() + actPost.getCoord().getY()) / 2.0;
					Coord center = new CoordImpl(x,y);
										
					ChoiceSet cs = new ChoiceSet(travelTimeApproximationLevel, (PlansCalcRoute)this.controler.createRoutingAlgorithm(), 
							this.network, this.controler.getConfig());
					this.createChoiceSetCircle(
							center, maxRadius, this.actTypeConverter.convertType(((ActivityImpl)actToMove).getType()), cs,
							plan.getPerson().getId());
										
					// **************************************************
					// maybe repeat this a couple of times
					TravelTime travelTime = super.getControler().getTravelTimeCalculator();
					TravelDisutility travelCost = super.getControler().getTravelDisutilityFactory().
							createTravelDisutility((PersonalizableTravelTime)travelTime, (PlanCalcScoreConfigGroup)super.getControler().getConfig().getModule("planCalcScore"));
						
					this.setLocation((ActivityImpl)actToMove, 
							cs.getWeightedRandomChoice(actlegIndex, plan.getPerson(),
									actPre.getCoord(), actPost.getCoord(), this.facilities, 
									scoringFunction, plan, travelTime, travelCost,
									this.controler.getIterationNumber()));	
					
					this.evaluateAndAdaptPlans(plan, bestPlan, cs, scoringFunction);
					// **************************************************
				}
			}
		}		
	}
	
	private void setLocation(ActivityImpl act, Id facilityId) {
		act.setFacilityId(facilityId);
		act.setLinkId(((NetworkImpl) this.network).getNearestLink(this.facilities.getFacilities().get(facilityId).getCoord()).getId());
   		act.setCoord(this.facilities.getFacilities().get(facilityId).getCoord());
	}
	
	public void createChoiceSetCircle(Coord center, double radius, String type, ChoiceSet cs, Id personId) {
		ArrayList<ActivityFacility> list = 
			(ArrayList<ActivityFacility>) super.quadTreesOfType.get(type).get(center.getX(), center.getY(), radius);
	
		for (ActivityFacility facility : list) {
			if (this.sampler.sample(facility.getId(), personId)) { 
				cs.addDestination(facility.getId());
			}
		}
	}
	
	private void evaluateAndAdaptPlans(Plan plan, Plan bestPlan, ChoiceSet cs, ScoringFunctionAccumulator scoringFunction) {		
		double score = this.computeScoreAndAdaptPlan(plan, cs, scoringFunction);	
		if (score > bestPlan.getScore() + 0.0000000000001) {
			plan.setScore(score);
			((PlanImpl)bestPlan).getPlanElements().clear();
			((PlanImpl)bestPlan).copyPlan(plan);
		}
	}
	
	private double computeScoreAndAdaptPlan(Plan plan, ChoiceSet cs, ScoringFunctionAccumulator scoringFunction) {
		PlanImpl planTmp = new PlanImpl(plan.getPerson());
		planTmp.copyPlan(plan);			
		scoringFunction.reset();

// always use travel times
//		if (Boolean.parseBoolean(this.controler.getConfig().locationchoice().getTravelTimes())) {
			// plan, actToMove, actlegIndex, planTmp, scoringFunction, leastCostPathCalculatorForward, leastCostPathCalculatorBackward, approximationLevel
			cs.adaptAndScoreTimes((PlanImpl) plan, 0, planTmp, scoringFunction, null, null, 0);	
//		}
		// not needed anymore
		//scoringFunction.finish();
		double score = scoringFunction.getScore();
		scoringFunction.reset();
		PlanUtils.copyPlanFields((PlanImpl)plan, (PlanImpl)planTmp);		
		return score;
	}
	
	private double getMaximumDistanceFromEpsilon(PersonImpl person, String type) {
		double maxEpsilon = 0.0;
		double scale = 1.0;
		
		this.scaleEpsilon.getEpsilonFactor(type);
		maxEpsilon = (Double) this.personsMaxEpsUnscaled.getAttribute(person.getId().toString(), type);
		
		maxEpsilon *= scale;
		
		/* 
		 * here one could do a much more sophisticated calculation including time use and travel speed estimations (from previous iteration)
		 */
		double travelSpeedCrowFly = Double.parseDouble(this.controler.getConfig().locationchoice().getRecursionTravelSpeed());
		double betaTime = this.controler.getConfig().planCalcScore().getTraveling_utils_hr();
		double maxTravelTime = Double.MAX_VALUE;
		if (betaTime != 0.0) {
			maxTravelTime = Math.abs(maxEpsilon / (-1.0 * betaTime) * 3600.0); //[s] // abs used for the case when somebody defines beta > 0
		}
		// distance linear
		double maxDistance = travelSpeedCrowFly * maxTravelTime; 
		
		// non-linear and tastes: exhaustive search for the moment
		// later implement non-linear equation solver
		if (Double.parseDouble(this.controler.getConfig().locationchoice().getMaxDistanceEpsilon()) > 0.0) {
			maxDistance = Double.parseDouble(this.controler.getConfig().locationchoice().getMaxDistanceEpsilon());
		}
		return maxDistance;
	}
}
