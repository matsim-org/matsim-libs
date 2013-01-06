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

package org.matsim.contrib.locationchoice.bestresponse;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.bestresponse.PlanTimesAdapter.ApproximationLevel;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.timegeography.RecursiveLocationMutator;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.contrib.locationchoice.utils.QuadTreeRing;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.old.PlanRouterAdapter;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

public final class BestResponseLocationMutator extends RecursiveLocationMutator {
	private final ActivityFacilitiesImpl facilities;
	private final ObjectAttributes personsMaxEpsUnscaled;
	private final ScaleEpsilon scaleEpsilon;
	private final ActTypeConverter actTypeConverter;
	private final DestinationSampler sampler;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final TravelTime travelTime;
	private final TravelDisutility travelCost;
	private final TripRouter tripRouter;
	private final int iteration;

	/* TODO: Use Singleton pattern or encapsulating object for the many constructor objects
	 * Similar to Scenario object
	 */
	public BestResponseLocationMutator(final Scenario scenario,
			TreeMap<String, QuadTreeRing<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacilityImpl []> facilities_of_type,
			ObjectAttributes personsMaxEpsUnscaled, ScaleEpsilon scaleEpsilon,
			ActTypeConverter actTypeConverter, DestinationSampler sampler,
			ScoringFunctionFactory scoringFunctionFactory,
			TravelTime travelTime, TravelDisutility travelCost,
			TripRouter tripRouter, int iteration) {
		super(scenario, tripRouter, quad_trees, facilities_of_type, null);
		this.facilities = (ActivityFacilitiesImpl) ((ScenarioImpl) scenario).getActivityFacilities();
		this.personsMaxEpsUnscaled = personsMaxEpsUnscaled;
		this.scaleEpsilon = scaleEpsilon;
		this.actTypeConverter = actTypeConverter;
		this.sampler = sampler;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.travelTime = travelTime;
		this.travelCost = travelCost;
		this.tripRouter = tripRouter;
		this.iteration = iteration;
	}

	@Override
	public final void handlePlan(final Plan plan){
		// if person is not in the analysis population
		if (Integer.parseInt(plan.getPerson().getId().toString()) > 
		Integer.parseInt(super.scenario.getConfig().locationchoice().getIdExclusion())) return;

		// yyyy why is all this plans copying necessary?  Could you please explain the design a bit?  Thanks.  kai, jan'13

		Plan bestPlan = new PlanImpl(plan.getPerson());	

		// bestPlan is initialized as a copy from the old plan:
		((PlanImpl)bestPlan).copyFrom(plan);

		// this will probably generate an improved bestPlan (?):
		this.handleActivities(plan, bestPlan);

		// copy the best plan into replanned plan
		// making a deep copy
		PlanUtils.copyPlanFieldsToFrom((PlanImpl)plan, (PlanImpl)bestPlan);
		// yy ( the syntax of this is copy( to, from) !!! )

		super.resetRoutes(plan);
	}

	private void handleActivities(Plan plan, final Plan bestPlan) {
		int tmp = Integer.parseInt(scenario.getConfig().locationchoice().getTravelTimeApproximationLevel());
		ApproximationLevel travelTimeApproximationLevel ;
		if ( tmp==0 ) {
			travelTimeApproximationLevel = ApproximationLevel.COMPLETE_ROUTING ;
		} else if ( tmp==1 ) {
			travelTimeApproximationLevel = ApproximationLevel.LOCAL_ROUTING ;
		} else if ( tmp==2 ) {
			travelTimeApproximationLevel = ApproximationLevel.NO_ROUTING ;
		} else {
			throw new RuntimeException("unknwon travel time approximation level") ;
		}
		// yyyy the above is not great, but when I found it, it was passing integers all the way to the method.  kai, jan'13

		
		ScoringFunctionAccumulator scoringFunction = (ScoringFunctionAccumulator) this.scoringFunctionFactory.createNewScoringFunction(plan); 

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
					double maximumDistance = this.convertEpsilonIntoDistance(plan.getPerson(), 
							this.actTypeConverter.convertType(actToMove.getType()));

					double maxRadius = (distanceDirect +  maximumDistance) / 2.0;

					double x = (actPre.getCoord().getX() + actPost.getCoord().getX()) / 2.0;
					double y = (actPre.getCoord().getY() + actPost.getCoord().getY()) / 2.0;
					Coord center = new CoordImpl(x,y);

					ChoiceSet cs = new ChoiceSet(travelTimeApproximationLevel, this.scenario.getNetwork(),
							this.scenario.getConfig());
					this.createChoiceSetFromCircle(
							center, maxRadius, this.actTypeConverter.convertType(((ActivityImpl)actToMove).getType()), cs,
							plan.getPerson().getId());

					// **************************************************
					// maybe repeat this a couple of times

					this.setLocation((ActivityImpl)actToMove, 
							cs.getWeightedRandomChoice(actlegIndex, actPre.getCoord(),
									actPost.getCoord(), this.facilities, scoringFunction, 
									plan, travelTime, travelCost, tripRouter, iteration));	

					// the change was done to "plan".  Now check if we want to copy this to bestPlan:
					this.evaluateAndAdaptPlans(plan, bestPlan, cs, scoringFunction);
					// yyyy if I understand this correctly, this means that, if the location change is accepted, all subsequent locachoice 
					// optimization attempts
					// will be run with that new location.  kai, jan'13
					
					// **************************************************
				}
			}
		}		
	}

	private void setLocation(ActivityImpl act, Id facilityId) {
		act.setFacilityId(facilityId);
		act.setLinkId(((NetworkImpl) this.scenario.getNetwork()).getNearestLink(this.facilities.getFacilities().get(facilityId).getCoord()).getId());
		act.setCoord(this.facilities.getFacilities().get(facilityId).getCoord());
	}
	
	private final void createChoiceSetFromCircle(Coord center, double radius, String type, ChoiceSet cs, Id personId) {
		ArrayList<ActivityFacility> list = 
				(ArrayList<ActivityFacility>) super.quadTreesOfType.get(type).get(center.getX(), center.getY(), radius);

		for (ActivityFacility facility : list) {
			if (this.sampler.sample(facility.getId(), personId)) { 
				cs.addDestination(facility.getId());
			}
		}
	}

	private void evaluateAndAdaptPlans(Plan plan, Plan bestPlanSoFar, ChoiceSet cs, ScoringFunctionAccumulator scoringFunction) {		
		double score = this.computeScoreAndAdaptPlan(plan, cs, scoringFunction);	
		if (score > bestPlanSoFar.getScore() + 0.0000000000001) {
			plan.setScore(score);
			((PlanImpl)bestPlanSoFar).getPlanElements().clear();
			((PlanImpl)bestPlanSoFar).copyFrom(plan);
		}
	}

	private double computeScoreAndAdaptPlan(Plan plan, ChoiceSet cs, ScoringFunctionAccumulator scoringFunction) {
		// yyyy why is all this plans copying necessary?  kai, jan'13

		PlanImpl planTmp = new PlanImpl(plan.getPerson());
		planTmp.copyFrom(plan);			
		scoringFunction.reset();

		cs.adaptAndScoreTimes((PlanImpl) plan, 0, planTmp, scoringFunction, null, null, tripRouter, ApproximationLevel.COMPLETE_ROUTING);	
		double score = scoringFunction.getScore();
		scoringFunction.reset();
		PlanUtils.copyPlanFieldsToFrom((PlanImpl)plan, (PlanImpl)planTmp);	// copy( to, from )
		return score;
	}
	
	
	/**
	 * Conversion of the "frozen" logit model epsilon into a distance.
	 */
	private double convertEpsilonIntoDistance(Person person, String type) {
		double maxEpsilon = 0.0;
		double scale = 1.0;

		this.scaleEpsilon.getEpsilonFactor(type);
		// yyyyyy what is the above line of code doing?  kai, jan'13
		
		maxEpsilon = (Double) this.personsMaxEpsUnscaled.getAttribute(person.getId().toString(), type);

		maxEpsilon *= scale;

		/* 
		 * here one could do a much more sophisticated calculation including time use and travel speed estimations (from previous iteration)
		 */
		double travelSpeedCrowFly = Double.parseDouble(this.scenario.getConfig().locationchoice().getRecursionTravelSpeed());
		double betaTime = this.scenario.getConfig().planCalcScore().getTraveling_utils_hr();
		double maxTravelTime = Double.MAX_VALUE;
		if (betaTime != 0.0) {
			maxTravelTime = Math.abs(maxEpsilon / (-1.0 * betaTime) * 3600.0); //[s] // abs used for the case when somebody defines beta > 0
		}
		// distance linear
		double maxDistance = travelSpeedCrowFly * maxTravelTime; 

		// non-linear and tastes: exhaustive search for the moment
		// later implement non-linear equation solver
		if (Double.parseDouble(this.scenario.getConfig().locationchoice().getMaxDistanceEpsilon()) > 0.0) {
			maxDistance = Double.parseDouble(this.scenario.getConfig().locationchoice().getMaxDistanceEpsilon());
		}
		return maxDistance;
	}
}
