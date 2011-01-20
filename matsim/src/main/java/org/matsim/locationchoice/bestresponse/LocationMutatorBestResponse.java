/* *********************************************************************** *
 * project: org.matsim.*
 * LocationMutatorCompleteSet.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.locationchoice.utils.ActTypeConverter;
import org.matsim.locationchoice.utils.QuadTreeRing;

public class LocationMutatorBestResponse extends LocationMutatorwChoiceSet {
	private HashSet<String> flexibleTypes = new HashSet<String>();
	private ActivityFacilitiesImpl facilities;
	private Plan bestPlan;
	private Network network;
			
	public LocationMutatorBestResponse(final Network network, Controler controler, Knowledges kn,
			TreeMap<String, QuadTreeRing<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacilityImpl []> facilities_of_type, Random random) {
		super(network, controler, kn, quad_trees, facilities_of_type, random);
		facilities = (ActivityFacilitiesImpl) super.controler.getFacilities();
		this.network = network;
	}
	
	@Override
	public void handlePlan(final Plan plan){
		
		// if person is not in the analysis population
		if (Integer.parseInt(plan.getPerson().getId().toString()) > 1000000000) return;
		
		this.initFlexibleTypes(this.controler.getConfig().locationchoice());
		super.resetRoutes(plan);
		this.bestPlan = new PlanImpl();	
		// make sure there is a valid plan in bestPlan
		((PlanImpl)this.bestPlan).copyPlan(plan);
		
		this.handleActivities(plan);
		
		((PlanImpl)plan).getPlanElements().clear();
		// copy the best plan into replanned plan
		((PlanImpl)plan).copyPlan(this.bestPlan);		
	}
		
	private void initFlexibleTypes(LocationChoiceConfigGroup config) {
		String types = config.getFlexibleTypes();
		if (!(types.equals("null") || types.equals(""))) {
			String[] entries = types.split(",", -1);
			for (int i = 0; i < entries.length; i++) {
				this.flexibleTypes.add(entries[i].trim());
			}
		}
	}
	
	private void handleActivities(Plan plan) {	
		int actlegIndex = -1;
		for (PlanElement pe : plan.getPlanElements()) {
			actlegIndex++;
			if (pe instanceof Activity) {
				if (actlegIndex > 0 && this.flexibleTypes.contains(
						ActTypeConverter.convert2FullType(((ActivityImpl)plan.getPlanElements().get(actlegIndex)).getType()))) {
					
					List<? extends PlanElement> actslegs = plan.getPlanElements();
					final Activity actToMove = (Activity)pe;
					final Activity actPre = (Activity)actslegs.get(actlegIndex - 2);
					
// 					not done for the moment
//					// find the next fixed activity
//					int indexFixed = actlegIndex + 2;
//					while (this.flexibleTypes.contains(
//							ActTypeConverter.convert2FullType(((ActivityImpl)plan.getPlanElements().get(indexFixed)).getType()))) {
//						indexFixed += 2;
//					}
					
					final Activity actPost = (Activity)actslegs.get(actlegIndex + 2);					
					double distanceDirect = ((CoordImpl)actPre.getCoord()).calcDistance(actPost.getCoord());
					double distanceFromEpsilon = this.getMaximumDistanceFromEpsilon((PersonImpl)plan.getPerson(), actToMove.getType());
					double maxRadius = (distanceDirect +  distanceFromEpsilon) / 2.0;
					
					double x = (actPre.getCoord().getX() + actPost.getCoord().getX()) / 2.0;
					double y = (actPre.getCoord().getY() + actPost.getCoord().getY()) / 2.0;
					Coord center = new CoordImpl(x,y);
					
					int travelTimeApproximationLevel = Integer.parseInt(
							this.controler.getConfig().locationchoice().getTravelTimeApproximationLevel());
					
					ChoiceSet cs = new ChoiceSet(travelTimeApproximationLevel, (PlansCalcRoute)this.controler.createRoutingAlgorithm(), 
							this.network, this.controler.getConfig());
					this.createChoiceSetCircle(center, maxRadius, ActTypeConverter.convert2FullType(((ActivityImpl)actToMove).getType()), cs);
										
					// **************************************************
					if (travelTimeApproximationLevel > 0) {
						// maybe repeat this a couple of times
						TravelTime travelTime = super.getControler().getTravelTimeCalculator();
						TravelCost travelCost = super.getControler().getTravelCostCalculatorFactory().
							createTravelCostCalculator((PersonalizableTravelTime)travelTime, (PlanCalcScoreConfigGroup)super.getControler().getConfig().getModule("planCalcScore"));
						
						ScoringFunctionAccumulator scorer = (ScoringFunctionAccumulator) this.controler.getPlansScoring().getPlanScorer().getScoringFunctionForAgent(plan.getPerson().getId());
						this.setLocation((ActivityImpl)actToMove, 
								cs.getWeightedRandomChoice(actlegIndex, plan.getPerson(),
										actPre.getCoord(), actPost.getCoord(), this.facilities, this.random,
										scorer, plan, travelTime, travelCost));						
						this.evaluatePlan(plan, cs, true);
					}
					else {
						cs.shuffle(this.random);
						int cnt = 0;
						while (cs.hasUnvisited() && cnt < cs.getNumberOfDestinations() * 0.5) {
							this.setLocation((ActivityImpl)actToMove, cs.visitNext());
							this.evaluatePlan(plan, cs, false);
							cnt++;
						}
					}
					// **************************************************
				}
			}
		}		
	}
	
	private void setLocation(ActivityImpl act, Id facilityId) {
		act.setFacilityId(facilityId);
		act.setLinkId((Id)((NetworkImpl) this.network).getNearestLink(this.facilities.getFacilities().get(facilityId).getCoord()).getId());
   		act.setCoord(this.facilities.getFacilities().get(facilityId).getCoord());
	}
	
	public void createChoiceSetCircle(Coord center, double radius, String type, ChoiceSet cs) {
		ArrayList<ActivityFacility> list = 
			(ArrayList<ActivityFacility>) super.quadTreesOfType.get(type).get(center.getX(), center.getY(), radius);
		
		for (ActivityFacility facility : list) {
			cs.addDestination(facility.getId());
		}
	}
	
	private void evaluatePlan(Plan plan, ChoiceSet cs, boolean adapt) {		
		double score = this.computeScore(plan, cs, adapt);	
		if (score > this.bestPlan.getScore()) {
			plan.setScore(score);
			((PlanImpl)bestPlan).getPlanElements().clear();
			((PlanImpl)this.bestPlan).copyPlan(plan);
		}
	}
	
	private double computeScore(Plan plan, ChoiceSet cs, boolean adapt) {
		PlanImpl planTmp;
		if (adapt) {
		 planTmp = (PlanImpl) plan;	
		}
		else {
			planTmp = new PlanImpl();
			planTmp.copyPlan(plan);
		}		
		ScoringFunctionAccumulator scoringFunction = 
			(ScoringFunctionAccumulator) this.controler.getPlansScoring().getPlanScorer().getScoringFunctionForAgent(plan.getPerson().getId());		
		scoringFunction.reset();
		
		if (Boolean.parseBoolean(this.controler.getConfig().locationchoice().getTravelTimes())) {
			// plan, actToMove, actlegIndex, planTmp, scoringFunction, leastCostPathCalculator, approximationLevel
			cs.adaptAndScoreTimes((PlanImpl) plan, 0, planTmp, scoringFunction, null, 0);	
		}
		scoringFunction.finish();
		double score = scoringFunction.getScore();
		scoringFunction.reset();		
		return score;
	}
	
	private double getMaximumDistanceFromEpsilon(PersonImpl person, String type) {
		double maxEpsilon = 0.0;
		if (type.startsWith("s")) {
			maxEpsilon = Double.parseDouble(person.getDesires().getDesc().split("_")[2]);
		}
		else if (type.startsWith("l")) {
			maxEpsilon = Double.parseDouble(person.getDesires().getDesc().split("_")[3]);
		}
		// here one could do a much more sophisticated calculation including time use and travel speed estimations (from previous iteration)
		// distance linear
		double maxDistance = -1.0 * (maxEpsilon / Double.parseDouble(this.controler.getConfig().locationchoice().getSearchSpaceBeta()));
		
		// non-linear and tastes: exhaustive search for the moment
		// later implement non-linear equation solver
		if (Double.parseDouble(this.controler.getConfig().locationchoice().getMaxDistanceEpsilon()) > 0.0) {
			maxDistance = Double.parseDouble(this.controler.getConfig().locationchoice().getMaxDistanceEpsilon());
		}
		return maxDistance;
	}
}
