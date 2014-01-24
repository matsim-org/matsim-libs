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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.BackwardFastMultiNodeDijkstra;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.old.PlanRouterAdapter;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;

public class PlanTimesAdapter {
	
	public static enum ApproximationLevel { COMPLETE_ROUTING, LOCAL_ROUTING, NO_ROUTING } ;
	
	// 0: complete routing
	// 1: local routing
	// 2: no routing (distance-based approximation)
	private ApproximationLevel approximationLevel = ApproximationLevel.COMPLETE_ROUTING; 
	private final MultiNodeDijkstra forwardMultiNodeDijkstra;
	private final BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra;
	private final Network network;
	private final Config config;
	private final TripRouter router;
	private Scenario scenario;
	
	private static final Logger log = Logger.getLogger(PlanTimesAdapter.class);
		
	/* package */ PlanTimesAdapter(ApproximationLevel approximationLevel, MultiNodeDijkstra forwardMultiNodeDijkstra, 
			BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra, TripRouter router, Scenario scenario) {
		this.approximationLevel = approximationLevel;
		this.forwardMultiNodeDijkstra = forwardMultiNodeDijkstra;
		this.backwardMultiNodeDijkstra = backwardMultiNodeDijkstra;
		this.network = scenario.getNetwork();
		this.router = router;
		this.config = scenario.getConfig();
		this.scenario = scenario;
	}
	
	/* package */ void adaptTimesAndScorePlan(PlanImpl plan, int actlegIndex, PlanImpl planTmp, ScoringFunctionAccumulator scoringFunction) {	
		
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
//					System.err.println("10 score: " + scoringFunction.getScore() ) ;
					continue; 
				}
				else {					
					PathCosts pathCosts = null;
					if (approximationLevel == ApproximationLevel.COMPLETE_ROUTING ) {
						pathCosts = computeTravelTimeFromCompleteRouting(plan.getPerson(),
							plan.getPreviousActivity(plan.getPreviousLeg(act)), act, ((Leg)plan.getPreviousLeg(act)).getMode());
					} else if (approximationLevel == ApproximationLevel.LOCAL_ROUTING ){
						pathCosts = computeTravelTimeFromLocalRouting(plan, actlegIndex, planElementIndex, act);
					} else if (approximationLevel == ApproximationLevel.NO_ROUTING ) {
						pathCosts = approximateTravelTimeFromDistance(plan, actlegIndex, planElementIndex, act);
					}
					
					double legTravelTime = pathCosts.getRoute().getTravelTime();
					double actDur = act.getMaximumDuration();
										
					// set new leg travel time, departure time and arrival time
					Leg previousLegPlanTmp = (Leg)planTmp.getPlanElements().get(planElementIndex -1);
					previousLegPlanTmp.setTravelTime(legTravelTime);
					double departureTime = ((Activity)planTmp.getPlanElements().get(planElementIndex -2)).getEndTime();
					previousLegPlanTmp.setDepartureTime(departureTime);
					double arrivalTime = departureTime + legTravelTime;
					previousLegPlanTmp.setRoute(pathCosts.getRoute());
					
					// yyyy I think this is really more complicated than it should be since it could/should just copy the time structure of the 
					// original plan and not think so much.  kai, jan'13
					
					// set new activity end time
					Activity actTmp = (Activity) planTmp.getPlanElements().get(planElementIndex);
					// yy Isn't this the same as "act"?  (The code originally was a bit different, but in the original code it was also not using the
					// iterated element.)  kai, jan'13
					
//					actTmp.setStartTime(arrivalTime);

					if ( config.vspExperimental().getActivityDurationInterpretation().equals(ActivityDurationInterpretation.endTimeOnly.toString()) ) {
						throw new RuntimeException("activity duration interpretation of " 
								+ config.vspExperimental().getActivityDurationInterpretation().toString() + " is not supported for locationchoice; aborting ... " +
										"Use " + ActivityDurationInterpretation.tryEndTimeThenDuration.toString() + "instead.") ;
					} else 
//						if ( config.vspExperimental().getActivityDurationInterpretation()==ActivityDurationInterpretation.tryEndTimeThenDuration ) 
					{
						if ( act.getEndTime() != Time.UNDEFINED_TIME ) {
							actTmp.setEndTime( act.getEndTime() ) ;
						} else if ( act.getMaximumDuration() != Time.UNDEFINED_TIME) {
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
										
//					System.err.println("20 score: " + scoringFunction.getScore() ) ;

					scoringFunction.startActivity(arrivalTime, actTmp);
//					System.err.println("arrivalTime: " + arrivalTime ) ;
					if (planElementIndex < plan.getPlanElements().size() -1) {
						
						if ( actTmp.getEndTime() != Time.UNDEFINED_TIME ) {
							scoringFunction.endActivity( actTmp.getEndTime(), actTmp ) ;
//							System.err.println( "actEndTime: " + actTmp.getEndTime() ) ;
						} else if ( actTmp.getMaximumDuration() != Time.UNDEFINED_TIME ) {
							scoringFunction.endActivity(arrivalTime + actDur, actTmp);
//							System.err.println( "arrivalTime: " + arrivalTime + " actDur: " + actDur ) ;
						}
//						System.err.println("30 score: " + scoringFunction.getScore() ) ;
					}
					else {
						// (last (home) activity does not end the activity:)
						scoringFunction.finish();
//						System.err.println("40 score: " + scoringFunction.getScore() ) ;
					}
				}				
			}
		}
	}

	private PathCosts approximateTravelTimeFromDistance(Plan thePlan, int actlegIndex, int planElementIndex, PlanElement pe) {
		PlanImpl plan = (PlanImpl) thePlan ;
		PathCosts pathCosts = new PathCostsGeneric(this.network);

		if (planElementIndex ==  actlegIndex || planElementIndex == (actlegIndex + 2)) {
			pathCosts = getTravelTimeApproximation(plan, (ActivityImpl)pe);
		}
		else {
			//use travel times from last iteration for efficiency reasons
			pathCosts.setRoute((plan).getPreviousLeg((Activity)pe).getRoute());
		}
		return pathCosts;
	}

	private PathCosts computeTravelTimeFromLocalRouting(Plan thePlan, int actlegIndex, int planElementIndex, PlanElement pe) {
		PlanImpl plan = (PlanImpl) thePlan ;
		PathCosts pathCosts = new PathCostsNetwork(this.network);
		Activity actToMove = (Activity) plan.getPlanElements().get(actlegIndex);		
		
		if  (!plan.getPreviousLeg(actToMove).equals(TransportMode.car)) {
			pathCosts = this.getTravelTimeApproximation(plan, (ActivityImpl)actToMove);
		} else {	
			if (planElementIndex == actlegIndex) {
				// adapt travel times: actPre -> actToMove
				Activity previousActivity = plan.getPreviousActivity(plan.getPreviousLeg((ActivityImpl)pe));
				
				Node fromNode = network.getLinks().get(previousActivity.getLinkId()).getToNode();
				Node toNode = network.getLinks().get(actToMove.getLinkId()).getToNode();
				Path p = this.forwardMultiNodeDijkstra.constructPath(fromNode, toNode, previousActivity.getEndTime());
				
				((PathCostsNetwork)pathCosts).createRoute(p.links, p.travelTime);
			}
			else if (planElementIndex == (actlegIndex + 2)) {
				//adapt travel times: actToMove -> actPost
				// replaced approximation: legTravelTime = getTravelTimeApproximation((PlanImpl)plan, (ActivityImpl)pe);
				
				Node fromNode = network.getLinks().get(((ActivityImpl)pe).getLinkId()).getToNode();
				Node toNode = network.getLinks().get(actToMove.getLinkId()).getToNode();
				
				/*
				 * Use activity.getStartTime() as time for the routing. Does this make sense?
				 */
				Path p = this.backwardMultiNodeDijkstra.constructPath(fromNode, toNode, ((Activity) pe).getStartTime());
				((PathCostsNetwork)pathCosts).createRoute(p.links, p.travelTime);
			}
			else {
				//use travel times from last iteration for efficiency reasons
				pathCosts.setRoute((plan).getPreviousLeg((Activity)pe).getRoute());
			}
		}
		return pathCosts;
	}
	
	private PathCosts getTravelTimeApproximation(PlanImpl plan, ActivityImpl pe) {	
		Activity actPre = plan.getPreviousActivity(plan.getPreviousLeg(pe));
		String mode = plan.getPreviousLeg(pe).getMode();
		PathCosts pathCosts = new PathCostsGeneric(this.network);
				
		// TODO: as soon as plansCalcRoute provides defaults for all modes use them 
		// I do not want users having to set dc parameters in other config modules!
		double speed = Double.parseDouble(this.config.locationchoice().getTravelSpeed_car());
		
		if (mode.equals(TransportMode.pt)) {
			speed = Double.parseDouble(this.config.locationchoice().getTravelSpeed_pt());	
		} else if (mode.equals(TransportMode.bike)) {
			speed = config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.bike);
		} else if (mode.equals(TransportMode.walk) || mode.equals(TransportMode.transit_walk)) {
			speed = config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk);
		} else {
			PlansCalcRouteConfigGroup r = config.plansCalcRoute();
			speed = r.getTeleportedModeSpeeds().get(PlansCalcRouteConfigGroup.UNDEFINED);
		}	
		((PathCostsGeneric)pathCosts).createRoute(
				this.network.getLinks().get(actPre.getLinkId()), 
				this.network.getLinks().get(pe.getLinkId()),
				config.plansCalcRoute().getBeelineDistanceFactor(), 
				speed);
		return pathCosts;
	}
	
	private PathCosts computeTravelTimeFromCompleteRouting(Person person, Activity fromAct, Activity toAct, String mode) {
		PathCosts pathCosts = new PathCostsNetwork(this.network);
			
		if (mode.equals(TransportMode.car)) {
			LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
			leg.setDepartureTime(0.0);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(0.0);
			
			try {
				PlanRouterAdapter.handleLeg(router, person, leg, fromAct, toAct, fromAct.getEndTime());	
				pathCosts.setRoute(leg.getRoute());
			} catch (Exception e) {
				log.info("plan routing not working for person " + person.getId().toString());
				GenericRouteImpl route = new GenericRouteImpl(null, null);
				route.setTravelTime(Double.MAX_VALUE);
				route.setDistance(Double.MAX_VALUE);
				pathCosts.setRoute(route);
			}
			
//		} else if (mode.equals(TransportMode.pt) && config.scenario().isUseTransit()) {
//			LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.pt);
//			leg.setDepartureTime(0.0);
//			leg.setTravelTime(0.0);
//			leg.setArrivalTime(0.0);
//			PlanRouterAdapter.handleLeg(router, person, leg, fromAct, toAct, fromAct.getEndTime());		
//			legTravelTime = leg.getTravelTime();			
		} else {
			pathCosts = this.getTravelTimeApproximation((PlanImpl)person.getSelectedPlan(), (ActivityImpl)toAct);
		}
		return pathCosts;
	}
}
