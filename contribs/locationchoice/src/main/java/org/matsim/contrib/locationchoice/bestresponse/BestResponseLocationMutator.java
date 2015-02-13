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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext.ActivityFacilityWithIndex;
import org.matsim.contrib.locationchoice.bestresponse.PlanTimesAdapter.ApproximationLevel;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.contrib.locationchoice.timegeography.RecursiveLocationMutator;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.contrib.locationchoice.utils.QuadTreeRing;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class BestResponseLocationMutator extends RecursiveLocationMutator {
	private final ActivityFacilities facilities;
	private final ObjectAttributes personsMaxDCScoreUnscaled;
	private final ScaleEpsilon scaleEpsilon;
	private final ActTypeConverter actTypeConverter;
	private final DestinationSampler sampler;
	private final DestinationChoiceBestResponseContext lcContext;
	private final MultiNodeDijkstra forwardMultiNodeDijkstra;
	private final BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final int iteration;
	private final Map<Id<ActivityFacility>, Id<Link>> nearestLinks;
	
	/*
	 * This is not nice! We hide the object of the super-super class since
	 * it expects a QuadTreeRing<ActivityFacility>. Find a better solution for this...
	 * cdobler, oct'13.
	 */
	protected TreeMap<String, QuadTreeRing<ActivityFacilityWithIndex>> quadTreesOfType;
	
	public BestResponseLocationMutator(
			TreeMap<String, QuadTreeRing<ActivityFacilityWithIndex>> quad_trees,
			TreeMap<String, ActivityFacilityImpl []> facilities_of_type,
			ObjectAttributes personsMaxDCScoreUnscaled, DestinationChoiceBestResponseContext lcContext,
			DestinationSampler sampler, TripRouter tripRouter, MultiNodeDijkstra forwardMultiNodeDijkstra,
			BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra, ScoringFunctionFactory scoringFunctionFactory,
			int iteration, Map<Id<ActivityFacility>, Id<Link>> nearestLinks) {
		// TODO: first null argument should be quad_trees...
		super(lcContext.getScenario(), tripRouter, null, facilities_of_type, null);
		this.facilities = lcContext.getScenario().getActivityFacilities();
		this.personsMaxDCScoreUnscaled = personsMaxDCScoreUnscaled;
		this.scaleEpsilon = lcContext.getScaleEpsilon();
		this.actTypeConverter = lcContext.getConverter();
		this.sampler = sampler;
		this.lcContext = lcContext;
		this.forwardMultiNodeDijkstra = forwardMultiNodeDijkstra;
		this.backwardMultiNodeDijkstra = backwardMultiNodeDijkstra;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.iteration = iteration;
		this.nearestLinks = nearestLinks;
		
		this.quadTreesOfType = quad_trees;
	}

	@Override
	public final void run(final Plan plan) {
		// if person is not in the analysis population
		// TODO: replace this now by subpopulation!
		final Person person = plan.getPerson();
		
		String idExclusion = super.scenario.getConfig().findParam("locationchoice", "idExclusion");
		if (idExclusion != null && Long.parseLong(person.getId().toString()) > Long.parseLong(idExclusion)) return;

		// why is all this plans copying necessary?  Could you please explain the design a bit?  Thanks.  kai, jan'13
		// (this may be done by now. kai, jan'13)

		Plan bestPlan = new PlanImpl(person);	

		// bestPlan is initialized as a copy from the old plan:
		((PlanImpl)bestPlan).copyFrom(plan);

		// this will probably generate an improved bestPlan (?):
		int personIndex = this.lcContext.getPersonIndex(person.getId());
		this.handleActivities(plan, bestPlan, personIndex);

		// copy the best plan into replanned plan
		// making a deep copy
		PlanUtils.copyPlanFieldsToFrom((PlanImpl)plan, (PlanImpl)bestPlan);
		// yy ( the syntax of this is copy( to, from) !!! )

		super.resetRoutes(plan);
	}

	private void handleActivities(Plan plan, final Plan bestPlan, int personIndex) {
		int tmp = Integer.parseInt(scenario.getConfig().findParam("locationchoice", "tt_approximationLevel"));
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
		// yyyy the above is not great, but when I found it, it was passing integers all the way down to the method.  kai, jan'13

		
		int actlegIndex = -1;
		for (PlanElement pe : plan.getPlanElements()) {
			actlegIndex++;
			if (pe instanceof Activity) {
				String actType = ((ActivityImpl)plan.getPlanElements().get(actlegIndex)).getType();
//				System.err.println("looking at act of type: " + actType ) ;
				if (actlegIndex > 0 && this.scaleEpsilon.isFlexibleType(actType)) {

					List<? extends PlanElement> actslegs = plan.getPlanElements();
					final Activity actToMove = (Activity)pe;
					final Activity actPre = (Activity)actslegs.get(actlegIndex - 2);

					final Activity actPost = (Activity)actslegs.get(actlegIndex + 2);					
					double distanceDirect = CoordUtils.calcDistance(actPre.getCoord(), actPost.getCoord());
					double maximumDistance = this.convertEpsilonIntoDistance(plan.getPerson(), 
							this.actTypeConverter.convertType(actToMove.getType()));

					double maxRadius = (distanceDirect +  maximumDistance) / 2.0;

					double x = (actPre.getCoord().getX() + actPost.getCoord().getX()) / 2.0;
					double y = (actPre.getCoord().getY() + actPost.getCoord().getY()) / 2.0;
					Coord center = new CoordImpl(x,y);

					ChoiceSet cs = createChoiceSetFromCircle(plan, personIndex, travelTimeApproximationLevel, actToMove, maxRadius, center);
					
//					System.err.println("ChoiceSet cs:\n" + cs.toString() ) ;

					// **************************************************
					// maybe repeat this a couple of times
					// yy why? kai, feb'13

					final Id<ActivityFacility> choice = cs.getWeightedRandomChoice(
							actlegIndex, scoringFunctionFactory, plan, this.getTripRouter(), this.lcContext.getPersonsKValuesArray()[personIndex],
							this.forwardMultiNodeDijkstra, this.backwardMultiNodeDijkstra, this.iteration);

					this.setLocation(actToMove, choice);
					
//					printTentativePlanToConsole(plan);

					// the change was done to "plan".  Now check if we want to copy this to bestPlan:
					this.evaluateAndAdaptPlans(plan, bestPlan, cs, scoringFunctionFactory);

					// yyyy if I understand this correctly, this means that, if the location change is accepted, all subsequent locachoice 
					// optimization attempts will be run with that new location.  kai, jan'13
					
					// **************************************************
				}
			}
		}		
	}

	private ChoiceSet createChoiceSetFromCircle(Plan plan, int personIndex,
			ApproximationLevel travelTimeApproximationLevel,
			final Activity actToMove, double maxRadius, Coord center) {

		ChoiceSet cs = new ChoiceSet(travelTimeApproximationLevel, this.scenario, this.nearestLinks);

		final String convertedType = this.actTypeConverter.convertType(actToMove.getType());
		Collection<ActivityFacilityWithIndex> list = this.quadTreesOfType.get(convertedType).get( center.getX(), center.getY(), maxRadius );
		
		for (ActivityFacilityWithIndex facility : list) {
//			int facilityIndex = this.lcContext.getFacilityIndex(facility.getId());
			int facilityIndex = facility.getArrayIndex();
			if (this.sampler.sample(facilityIndex, personIndex)) { 
				
				// only add destination if it can be reached with the chosen mode
				String mode = ((PlanImpl)plan).getPreviousLeg(actToMove).getMode();	
				
				Id<Link> linkId = null;
				// try to get linkId from facility, else get it from act. other options not allowed!
				if (facility.getLinkId() != null) {
					linkId = facility.getLinkId();
				}
				else {
					linkId = actToMove.getLinkId();
				}
				// TODO: solve this generic. for that we need info from the config, which modes are actually teleported.
				if (this.lcContext.getScenario().getNetwork().getLinks().get(linkId).getAllowedModes().contains(mode) || 
						mode.equals(TransportMode.bike) || 
						mode.equals(TransportMode.walk) ||
						mode.equals(TransportMode.transit_walk) ||
						mode.equals(TransportMode.other)) {
					cs.addDestination(facility.getId());
				}	
			}
		}
		
		return cs;
	}

	private void setLocation(Activity act2, Id<ActivityFacility> facilityId) {
		ActivityImpl act = (ActivityImpl) act2;
		act.setFacilityId(facilityId);
		ActivityFacility facility = this.facilities.getFacilities().get(facilityId);
		
		Id<Link> linkId = null;
		// try to get linkId from facility, else get it from coords. other options not allowed!
		if (facility.getLinkId() != null) {
			linkId = facility.getLinkId();
		}
		else {
			linkId = this.nearestLinks.get(facilityId);
		}	
		act.setLinkId(linkId);
		act.setCoord(facility.getCoord());
	}
	
	private void evaluateAndAdaptPlans(Plan plan, Plan bestPlanSoFar, ChoiceSet cs, ScoringFunctionFactory scoringFunction) {		
		double score = this.computeScoreAndAdaptPlan(plan, cs, scoringFunction);	
//		System.err.println("expected score of new plan is: " + score ) ;
//		System.err.println("existing score of old plan is: " + bestPlanSoFar.getScore() ) ;
		if (score > bestPlanSoFar.getScore() + 0.0000000000001) {
			plan.setScore(score);
			((PlanImpl)bestPlanSoFar).getPlanElements().clear();
			((PlanImpl)bestPlanSoFar).copyFrom(plan);
		}
	}

	private double computeScoreAndAdaptPlan(Plan plan, ChoiceSet cs, ScoringFunctionFactory scoringFunction) {
		// yyyy why is all this plans copying necessary?  kai, jan'13

		PlanImpl planTmp = new PlanImpl(plan.getPerson());
		planTmp.copyFrom(plan);			

		final double score = 
				cs.adaptAndScoreTimes(
						(PlanImpl) plan,
						0,
						planTmp,
						scoringFunction,
						null,
						null,
						this.getTripRouter(), 
						ApproximationLevel.COMPLETE_ROUTING);	
		PlanUtils.copyPlanFieldsToFrom((PlanImpl)plan, (PlanImpl)planTmp);	// copy( to, from )
		return score;
	}
	
	
	/**
	 * Conversion of the "frozen" logit model epsilon into a distance.
	 */
	private double convertEpsilonIntoDistance(Person person, String type) {
		double maxDCScore = 0.0;
		double scale = this.scaleEpsilon.getEpsilonFactor(type);		
		maxDCScore = (Double) this.personsMaxDCScoreUnscaled.getAttribute(person.getId().toString(), type);
		maxDCScore *= scale; // apply the scale factors given in the config file

		/* 
		 * here one could do a much more sophisticated calculation including time use and travel speed estimations (from previous iteration)
		 */
		double travelSpeedCrowFly = Double.parseDouble(this.scenario.getConfig().findParam("locationchoice", "travelSpeed_car"));
		double betaTime = this.scenario.getConfig().planCalcScore().getTraveling_utils_hr();
//		if ( Boolean.getBoolean(this.scenario.getConfig().vspExperimental().getValue(VspExperimentalConfigKey.isUsingOpportunityCostOfTimeForLocationChoice)) ) {
		if ( this.scenario.getConfig().vspExperimental().isUsingOpportunityCostOfTimeForLocationChoice() ) {
			betaTime -= this.scenario.getConfig().planCalcScore().getPerforming_utils_hr() ;
			// needs to be negative (I think) since AH uses this as a cost parameter. kai, jan'13
		}
		double maxTravelTime = Double.MAX_VALUE;
		if (betaTime != 0.0) {
			if ( betaTime >= 0. ) {
				throw new RuntimeException("betaTime >= 0 in location choice; method not designed for this; aborting ...") ;
			}
			maxTravelTime = Math.abs(maxDCScore / (-1.0 * betaTime) * 3600.0); //[s] // abs used for the case when somebody defines beta > 0
			// yy maybe one can still used it with betaTime>0 (i.e. traveling brings utility), but taking the "abs" of it is almost certainly not the
			// correct way.  kai, jan'13
		}
		// distance linear
		double maxDistance = travelSpeedCrowFly * maxTravelTime; 

		// define a maximum distance choice set manually
		if (Double.parseDouble(this.scenario.getConfig().findParam("locationchoice", "maxDistanceDCScore")) > 0.0) {
			maxDistance = Double.parseDouble(this.scenario.getConfig().findParam("locationchoice", "maxDistanceDCScore"));
		}
		return maxDistance;
	}
}