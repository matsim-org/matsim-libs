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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.old.PlanRouterAdapter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;

public class PlanTimesAdapter {
	
	public static enum ApproximationLevel { COMPLETE_ROUTING, LOCAL_ROUTING, NO_ROUTING } ;
	
	// 0: complete routing
	// 1: local routing
	// 2: no routing (distance-based approximation)
	private ApproximationLevel approximationLevel = ApproximationLevel.COMPLETE_ROUTING ; 
	private final LeastCostPathCalculator leastCostPathCalculatorForward ;
	private final LeastCostPathCalculator leastCostPathCalculatorBackward ;
	private final Network network;
	private final Config config;
	private final TripRouter router;
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PlanTimesAdapter.class);
		
	/* package */ PlanTimesAdapter(ApproximationLevel approximationLevel, LeastCostPathCalculator leastCostPathCalculatorForward, 
			LeastCostPathCalculator leastCostPathCalculatorBackward, Network network, TripRouter router, Config config) {
		this.approximationLevel = approximationLevel;
		this.leastCostPathCalculatorForward = leastCostPathCalculatorForward;
		this.leastCostPathCalculatorBackward = leastCostPathCalculatorBackward;
		this.network = network;
		this.router = router;
		this.config = config;
	}
	
	/* package */ void adaptAndScoreTimes(PlanImpl plan, int actlegIndex, PlanImpl planTmp, ScoringFunctionAccumulator scoringFunction) {	
		
		// yyyy Note: getPrevious/NextLeg/Activity all relies on alternating activities and leg, which was given up as a requirement
		// a long time ago (which is why it is not in the interface).  kai, jan'13
		
		// yy This only works when the ScoringFunction is an implementation of ScoringFunctionAccumulator.  This may not always be the case.
		// kai, jan'13
		
		// iterate through plan and adapt travel and activity times
		int planElementIndex = -1;
		for (PlanElement pe : plan.getPlanElements()) {
			planElementIndex++;
			if (pe instanceof Activity) {
				Activity act = (Activity) pe ;
				if (planElementIndex == 0) {
					scoringFunction.startActivity(0.0, act);
					scoringFunction.endActivity(act.getEndTime(), act);
					continue; 
				}
				else {
					double legTravelTime = 0.0;
					if (approximationLevel == ApproximationLevel.COMPLETE_ROUTING ) {
						legTravelTime = computeTravelTimeFromCompleteRouting(plan.getPerson(),
							plan.getPreviousActivity(plan.getPreviousLeg(act)), 
							act);
					}
					else if (approximationLevel == ApproximationLevel.LOCAL_ROUTING ){
						legTravelTime = computeTravelTimeFromLocalRouting(plan, actlegIndex, planElementIndex, act);
					}
					else if (approximationLevel == ApproximationLevel.NO_ROUTING ) {
						legTravelTime = approximateTravelTimeFromDistance(plan, actlegIndex, planElementIndex, act);
					}
					
					double actDur = act.getMaximumDuration();
										
					// set new leg travel time, departure time and arrival time
					Leg previousLegPlanTmp = (Leg)planTmp.getPlanElements().get(planElementIndex -1);
					previousLegPlanTmp.setTravelTime(legTravelTime);
					double departureTime = ((Activity)planTmp.getPlanElements().get(planElementIndex -2)).getEndTime();
					previousLegPlanTmp.setDepartureTime(departureTime);
					double arrivalTime = departureTime + legTravelTime;
//					previousLegPlanTmp.setArrivalTime(arrivalTime);
					
					// set new activity end time
					Activity actTmp = (Activity) planTmp.getPlanElements().get(planElementIndex);
					// yy Isn't this the same as "act"?  (The code originally was a bit different, but in the original code it was also not using the
					// iterated element.)  kai, jan'13
					
//					actTmp.setStartTime(arrivalTime);

					if ( config.vspExperimental().getActivityDurationInterpretation()==ActivityDurationInterpretation.endTimeOnly ) {
						throw new RuntimeException("activity duration interpretation of " 
								+ config.vspExperimental().getActivityDurationInterpretation().toString() + " is not supported for locationchoice; aborting ... " +
										"Use " + ActivityDurationInterpretation.tryEndTimeThenDuration.toString() + "instead.") ;
					} else 
//						if ( config.vspExperimental().getActivityDurationInterpretation()==ActivityDurationInterpretation.tryEndTimeThenDuration ) 
					{
						if ( act.getEndTime() != Time.UNDEFINED_TIME ) {
							actTmp.setEndTime( act.getEndTime() ) ;
						} else if ( act.getMaximumDuration() == Time.UNDEFINED_TIME) {
							actTmp.setMaximumDuration( act.getMaximumDuration() ) ;
						}
					} 
//					else {
//						actTmp.setEndTime(arrivalTime + actDur);
//						// This is the original variant.  I would not make "duration" take absolute precedence over "endTime", but since
//						// VSP should use "tryEndTimeThenDuration", I will leave it for the time being.  kai, jan'13
//					}
					
					scoringFunction.startLeg(departureTime, previousLegPlanTmp);
					scoringFunction.endLeg(arrivalTime);
										
					scoringFunction.startActivity(arrivalTime, actTmp);
					if (planElementIndex < plan.getPlanElements().size() -1) {
						// (last (home) activity does not finish act)
						
						scoringFunction.endActivity(arrivalTime + actDur, actTmp);
					}
					else {
						scoringFunction.finish();
					}
				}				
			}
		}
	}

	private double approximateTravelTimeFromDistance(PlanImpl plan,
			int actlegIndex, int planElementIndex, PlanElement pe) {
		double legTravelTime;
		if (planElementIndex ==  actlegIndex || planElementIndex == (actlegIndex + 2)) {
			legTravelTime = getTravelTimeApproximation(plan, (ActivityImpl)pe);
		}
		else {
			//use travel times from last iteration for efficiency reasons
			legTravelTime = (plan).getPreviousLeg((Activity)pe).getTravelTime();
		}
		return legTravelTime;
	}

	private double computeTravelTimeFromLocalRouting(PlanImpl plan,
			int actlegIndex, int planElementIndex, PlanElement pe) {

		Activity actToMove = (Activity) plan.getPlanElements().get(actlegIndex);
		
		double legTravelTime;
		if (planElementIndex == actlegIndex) {
			// adapt travel times: actPre -> actToMove
			Activity previousActivity = (plan).getPreviousActivity((plan).
					getPreviousLeg((ActivityImpl)pe));
			
			Node fromNode = network.getLinks().get(previousActivity.getLinkId()).getToNode();
			Node toNode = network.getLinks().get(actToMove.getLinkId()).getToNode();
			
			Path p = this.leastCostPathCalculatorForward.calcLeastCostPath(fromNode, toNode, previousActivity.getEndTime(), plan.getPerson(), null);
			legTravelTime = p.travelTime;
			
			//log.info("planElementIndex: " + planElementIndex + "------------");
			//log.info("	Forward travel time: " + legTravelTime / 60.0);
			
		}
		else if (planElementIndex == (actlegIndex + 2)) {
			//adapt travel times: actToMove -> actPost
			// replaced approximation: legTravelTime = getTravelTimeApproximation((PlanImpl)plan, (ActivityImpl)pe);
			
			Node fromNode = network.getLinks().get(((ActivityImpl)pe).getLinkId()).getToNode();
			Node toNode = network.getLinks().get(actToMove.getLinkId()).getToNode();
																	
			Path p = this.leastCostPathCalculatorBackward.calcLeastCostPath(fromNode, toNode, -1.0, plan.getPerson(), null);
			legTravelTime = p.travelTime;
			//log.info("	Backward travel time: " + legTravelTime / 60.0);
		}
		else {
			//use travel times from last iteration for efficiency reasons
			legTravelTime = (plan).getPreviousLeg((Activity)pe).getTravelTime();
		}
		return legTravelTime;
	}
	
	private double getTravelTimeApproximation(PlanImpl plan, ActivityImpl pe) {
		Activity actPre = plan.getPreviousActivity(plan.getPreviousLeg(pe));
		double distance = 1.5 * ((CoordImpl)actPre.getCoord()).calcDistance(pe.getCoord());
		// TODO: get better speed estimation
		double speed = Double.parseDouble(this.config.locationchoice().getRecursionTravelSpeed());
		return distance / speed;
	}
	
	private double computeTravelTimeFromCompleteRouting(Person person, Activity fromAct, Activity toAct) {
		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(0.0);
		leg.setArrivalTime(0.0);
		PlanRouterAdapter.handleLeg(router, person, leg, fromAct, toAct, fromAct.getEndTime());
		return leg.getTravelTime();
	}

}
