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
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.old.PlanRouterAdapter;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;

public class PlanTimesAdapter {
	
	public static enum ApproximationLevel { COMPLETE_ROUTING, LOCAL_ROUTING, NO_ROUTING } ;
	
	// 0: complete routing
	// 1: local routing
	// 2: no routing (distance-based approximation)
	private PlansConfigGroup.ApproximationLevel approximationLevel = PlansConfigGroup.ApproximationLevel.COMPLETE_ROUTING; 
	private final MultiNodeDijkstra forwardMultiNodeDijkstra;
	private final BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra;
	private final Network network;
	private final Config config;
	private final TripRouter router;
	
	private static final Logger log = Logger.getLogger(PlanTimesAdapter.class);
		
	/* package */ PlanTimesAdapter(PlansConfigGroup.ApproximationLevel approximationLevel, MultiNodeDijkstra forwardMultiNodeDijkstra, 
			BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra, TripRouter router, Scenario scenario) {
		this.approximationLevel = approximationLevel;
		this.forwardMultiNodeDijkstra = forwardMultiNodeDijkstra;
		this.backwardMultiNodeDijkstra = backwardMultiNodeDijkstra;
		this.network = scenario.getNetwork();
		this.router = router;
		this.config = scenario.getConfig();
	}
	
	/* package */ double adaptTimesAndScorePlan(PlanImpl plan, int actlegIndex, PlanImpl planTmp, ScoringFunctionFactory scoringFunctionFactory) {	
		
		// yyyy Note: getPrevious/NextLeg/Activity all relies on alternating activities and leg, which was given up as a requirement
		// a long time ago (which is why it is not in the interface).  kai, jan'13
		
		// This used to use ScoringFunction accumulator (wrongly, as it started the first activity),
		// with the same instance used over and over. td, mai'14
		final ScoringFunction scoringFunction = scoringFunctionFactory.createNewScoringFunction( plan.getPerson() );
		
		// iterate through plan and adapt travel and activity times
		int planElementIndex = -1;
		for (PlanElement pe : plan.getPlanElements()) {
			planElementIndex++;
			if ( !( pe instanceof Activity ) ) continue;

			Activity act = (Activity) pe ;
			if (planElementIndex == 0) {
				final Activity actTmp = (Activity) planTmp.getPlanElements().get(planElementIndex);
				// this used to assume the activity starts at 00:00:00, but this
				// is not what happens in the iterations: the first activity
				// has no start time, the last has no end time.
				actTmp.setStartTime( Time.UNDEFINED_TIME );
				scoringFunction.handleActivity( actTmp );
//					System.err.println("10 score: " + scoringFunction.getScore() ) ;
				continue; 
			}
			
			PathCosts pathCosts = null;
			if (approximationLevel == PlansConfigGroup.ApproximationLevel.COMPLETE_ROUTING ) {
				pathCosts = computeTravelTimeFromCompleteRouting(plan.getPerson(),
					plan.getPreviousActivity(plan.getPreviousLeg(act)), act, ((Leg)plan.getPreviousLeg(act)).getMode());
			} else if (approximationLevel == PlansConfigGroup.ApproximationLevel.LOCAL_ROUTING ){
				pathCosts = computeTravelTimeFromLocalRouting(plan, actlegIndex, planElementIndex, act);
			} else if (approximationLevel == PlansConfigGroup.ApproximationLevel.NO_ROUTING ) {
				pathCosts = approximateTravelTimeFromDistance(plan, actlegIndex, planElementIndex, act);
			}
			
			double legTravelTime = pathCosts.getRoute().getTravelTime();
			double actDur = act.getMaximumDuration();
								
			// set new leg travel time, departure time and arrival time
			// XXX This will silently fail with multi-legs trips without "stage" activity! td may'14
			Leg previousLegPlanTmp = (Leg)planTmp.getPlanElements().get(planElementIndex -1);
			previousLegPlanTmp.setTravelTime(legTravelTime);
			// this assumes "tryEndTimeThenDuration"
			final Activity prevActTmp = (Activity) planTmp.getPlanElements().get(planElementIndex -2);
			double departureTime = 
				prevActTmp.getEndTime() != Time.UNDEFINED_TIME ?
					prevActTmp.getEndTime() :
					prevActTmp.getStartTime() + prevActTmp.getMaximumDuration();
			previousLegPlanTmp.setDepartureTime(departureTime);
			double arrivalTime = departureTime + legTravelTime;
			previousLegPlanTmp.setRoute(pathCosts.getRoute());
			if ( previousLegPlanTmp instanceof LegImpl ) {
				// this should not be necessary, as scoring functions can reconstruct
				// this information from interface methods,
				// but one never knows. I feel safer this way. td, may'14
				((LegImpl) previousLegPlanTmp).setArrivalTime( arrivalTime );
			}
			
			scoringFunction.handleLeg(previousLegPlanTmp);

			// yyyy I think this is really more complicated than it should be since it could/should just copy the time structure of the 
			// original plan and not think so much.  kai, jan'13

			// set new activity end time
			// yy Isn't this the same as "act"?  (The code originally was a bit different, but in the original code it was also not using the
			// iterated element.)  kai, jan'13
			// No, it is not: this one comes from planTmp, not plan... Not sure why this duplication is there, though. td may '14
			final Activity actTmp = (Activity) planTmp.getPlanElements().get(planElementIndex);

			actTmp.setStartTime(arrivalTime);

			PlansConfigGroup.ActivityDurationInterpretation actDurInterpr = ( config.plans().getActivityDurationInterpretation() ) ;
			switch ( actDurInterpr ) {
			case tryEndTimeThenDuration:
				if ( act.getEndTime() == Time.UNDEFINED_TIME &&
						act.getMaximumDuration() != Time.UNDEFINED_TIME) {
					// duration based definition: requires some care
					actTmp.setMaximumDuration( act.getMaximumDuration() ) ;

					// scoring function works with start and end time, not duration,
					// but we do not want to put an end time in the plan.
					assert actTmp.getEndTime() == Time.UNDEFINED_TIME : actTmp;
					actTmp.setEndTime( arrivalTime + actDur ) ;
					scoringFunction.handleActivity( actTmp );
					actTmp.setEndTime( Time.UNDEFINED_TIME );
				}
				else {
					actTmp.setEndTime( act.getEndTime() ) ;
					scoringFunction.handleActivity( actTmp );
				}
				break;
			default:
				throw new RuntimeException("activity duration interpretation of " 
						+ config.plans().getActivityDurationInterpretation().toString() + " is not supported for locationchoice; aborting ... " +
								"Use " + PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration.toString() + "instead.") ;
			}

		}

		scoringFunction.finish();
		return scoringFunction.getScore();
	}

	private PathCosts approximateTravelTimeFromDistance(Plan thePlan, int actlegIndex, int planElementIndex, PlanElement pe) {
		PlanImpl plan = (PlanImpl) thePlan ;
		PathCosts pathCosts = new PathCostsGeneric(this.network);

		if (planElementIndex ==  actlegIndex || planElementIndex == (actlegIndex + 2)) {
			pathCosts = getTravelTimeApproximation(plan, (ActivityImpl)pe);
		}
		else {
			//use travel times from last iteration for efficiency reasons if route is available
			Route routeOfPlan = (plan).getPreviousLeg((Activity)pe).getRoute();
			if (routeOfPlan != null) {
				pathCosts.setRoute(routeOfPlan);
			} else {
				pathCosts = getTravelTimeApproximation(plan, (ActivityImpl)pe);
			}
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
		double speed = Double.parseDouble(this.config.findParam("locationchoice", "travelSpeed_car"));	// seems to be overwritten in any case? cdobler, oct'14
		
		if (mode.equals(TransportMode.pt)) {
			speed = Double.parseDouble(this.config.findParam("locationchoice", "travelSpeed_pt"));	
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
//				config.plansCalcRoute().getBeelineDistanceFactor(), 
				config.plansCalcRoute().getModeRoutingParams().get( PlansCalcRouteConfigGroup.UNDEFINED ).getBeelineDistanceFactor(),
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
