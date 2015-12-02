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

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.contrib.locationchoice.router.PlanRouterAdapter;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
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
	private final Map<String, Double> teleportedModeSpeeds;
	private final Map<String, Double> beelineDistanceFactors;
	private final DestinationChoiceConfigGroup dccg;
	private final TripRouter router;
	
	private static final Logger log = Logger.getLogger(PlanTimesAdapter.class);
	
	/* package */ PlanTimesAdapter(ApproximationLevel approximationLevel, MultiNodeDijkstra forwardMultiNodeDijkstra, 
			BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra, TripRouter router, Scenario scenario, 
			Map<String, Double> teleportedModeSpeeds, Map<String, Double> beelineDistanceFactors) {
		this.approximationLevel = approximationLevel;
		this.forwardMultiNodeDijkstra = forwardMultiNodeDijkstra;
		this.backwardMultiNodeDijkstra = backwardMultiNodeDijkstra;
		this.network = scenario.getNetwork();
		this.router = router;
		this.config = scenario.getConfig();
		this.teleportedModeSpeeds = teleportedModeSpeeds;
		this.beelineDistanceFactors = beelineDistanceFactors;
		this.dccg = (DestinationChoiceConfigGroup) this.config.getModule(DestinationChoiceConfigGroup.GROUP_NAME);
	}	
	
	/*
	 * Why do we have plan and planTmp?!
	 * Probably to avoid something like concurrent modification problems?
	 * 
	 * - TODO: check whether values from planTmp are used before they are overwritten. If not, we could re-use a single plan over and over again as long as the plan structure is not changed! 
	 * 
	 * - Iterate over plan: skip all legs. They are scored when their subsequent activity is handled.
	 * 
	 * - Adapt activities and legs in planTmp: times and routes
	 * 
	 * ApproximationLevel:
	 * 	- COMPLETE_ROUTING: calculate route for EACH leg
	 * 	- LOCAL_ROUTING: calculate new routes from and to the adapted activity, take other route information from existing leg
	 * 	- NO_ROUTING: same as LOCAL_ROUTING but with estimated travel times for the calculated routes
	 * 
	 */
	/* package */ double adaptTimesAndScorePlan(Plan plan, int actlegIndex, Plan planTmp, ScoringFunctionFactory scoringFunctionFactory) {

		// yyyy Note: getPrevious/NextLeg/Activity all relies on alternating activities and leg, which was given up as a requirement
		// a long time ago (which is why it is not in the interface).  kai, jan'13

		// This used to use ScoringFunction accumulator (wrongly, as it started the first activity),
		// with the same instance used over and over. td, mai'14
		final ScoringFunction scoringFunction = scoringFunctionFactory.createNewScoringFunction(plan.getPerson());

		// iterate through plan and adapt travel and activity times
		int planElementIndex = -1;
		for (PlanElement pe : plan.getPlanElements()) {
			planElementIndex++;
			if (!(pe instanceof Activity)) continue;

			Activity act = (Activity) pe;
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

			// Those 3 variables are essentially the return value of the procedure below,
			// that depends on the approximation level
			// Before correcting for PT, this was only PathCosts
			PathCosts pathCosts = null;
			List<? extends PlanElement> listPlanElements = null;
			double legTravelTime = 0.0;

			final Leg previousLegPlanTmp = (Leg) planTmp.getPlanElements().get(planElementIndex - 1);

			// This should be split in 3 methods, returning an Object containing all the relevant information
			// td dec 15
			if (this.approximationLevel == ApproximationLevel.COMPLETE_ROUTING) {
				final Leg previousLeg = PlanUtils.getPreviousLeg(plan, act);
				final String mode = previousLeg.getMode();
				final Activity previousActivity = PlanUtils.getPreviousActivity(plan, previousLeg);
				pathCosts = computeTravelTimeFromCompleteRouting(plan, previousActivity, act, mode);
				listPlanElements =
						this.router.calcRoute(
								mode,
								new LinkWrapperFacility(
										this.network.getLinks().get(
												previousActivity.getLinkId())),
								new LinkWrapperFacility(
										this.network.getLinks().get(
												act.getLinkId())),
								previousActivity.getEndTime(),
								plan.getPerson());

				for(PlanElement pel : listPlanElements) {
					if (pel instanceof Leg) {
						scoringFunction.handleLeg( (Leg)pel );
						legTravelTime += ((Leg) pel).getTravelTime();
					}
				}

				final GenericRouteImpl route =
						new GenericRouteImpl(
								((Leg) listPlanElements.get(0)).getRoute().getStartLinkId(),
								((Leg) listPlanElements.get(listPlanElements.size() - 1)).getRoute().getEndLinkId());

				previousLegPlanTmp.setRoute(route);
				previousLegPlanTmp.setTravelTime(legTravelTime);

			} else if (this.approximationLevel == ApproximationLevel.LOCAL_ROUTING ) {
				pathCosts = computeTravelTimeFromLocalRouting(plan, actlegIndex, planElementIndex, act);
				previousLegPlanTmp.setRoute(pathCosts.getRoute());
				legTravelTime = pathCosts.getRoute().getTravelTime();
				previousLegPlanTmp.setTravelTime(legTravelTime);
			} else if (this.approximationLevel == ApproximationLevel.NO_ROUTING ) {
				pathCosts = approximateTravelTimeFromDistance(plan, actlegIndex, planElementIndex, act);
				previousLegPlanTmp.setRoute(pathCosts.getRoute());
				legTravelTime = pathCosts.getRoute().getTravelTime();
				previousLegPlanTmp.setTravelTime(legTravelTime);
			}

			double actDur = act.getMaximumDuration();

			final Activity prevActTmp = (Activity) planTmp.getPlanElements().get(planElementIndex - 2);
			double departureTime = 
				prevActTmp.getEndTime() != Time.UNDEFINED_TIME ?
					prevActTmp.getEndTime() :
					prevActTmp.getStartTime() + prevActTmp.getMaximumDuration();
			double arrivalTime = departureTime + legTravelTime;

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

	private PathCosts approximateTravelTimeFromDistance(Plan plan, int actlegIndex, int planElementIndex, PlanElement pe) {
		
		PathCosts pathCosts = new PathCostsGeneric(this.network);

		if (planElementIndex ==  actlegIndex || planElementIndex == (actlegIndex + 2)) {
			pathCosts = getTravelTimeApproximation(plan, (Activity) pe);
		}
		else {
			//use travel times from last iteration for efficiency reasons if route is available
//			Route routeOfPlan = plan.getPreviousLeg((Activity) pe).getRoute();
			Activity activity = (Activity) pe;
			Leg previousLeg = PlanUtils.getPreviousLeg(plan, activity);
			Route routeOfPlan = previousLeg.getRoute();
			if (routeOfPlan != null) {
				pathCosts.setRoute(routeOfPlan);
			} else {
				pathCosts = getTravelTimeApproximation(plan, (Activity) pe);
			}
		}
		return pathCosts;
	}

	private PathCosts computeTravelTimeFromLocalRouting(Plan plan, int actlegIndex, int planElementIndex, PlanElement pe) {

		PathCosts pathCosts = new PathCostsNetwork(this.network);
		Activity actToMove = (Activity) plan.getPlanElements().get(actlegIndex);		
		
		if  (!PlanUtils.getPreviousLeg(plan, actToMove).getMode().equals(TransportMode.car)) {
			pathCosts = this.getTravelTimeApproximation(plan, actToMove);
		} else {
			/*
			 * TODO: fix this! the if statement above compared the previous leg (the getMode() part was missing!!) with
			 * Transportmode.car, which of course always failed. Therefore, the code below was never executed. Seems that
			 * it is not running anymore - probably never did? cdobler oct'15
			 */
			pathCosts = this.getTravelTimeApproximation(plan, actToMove);
			
//			if (planElementIndex == actlegIndex) {
//				// adapt travel times: actPre -> actToMove
////				Activity previousActivity = plan.getPreviousActivity(plan.getPreviousLeg((ActivityImpl) pe));
//				Activity activity = (Activity) pe;
//				Leg previousLeg = PlanUtils.getPreviousLeg(plan, activity);
//				Activity previousActivity = PlanUtils.getPreviousActivity(plan, previousLeg);
//				
//				Id<Link> fromLinkId = previousActivity.getLinkId();
//				Id<Link> toLinkId = actToMove.getLinkId();
//				Node fromNode = network.getLinks().get(fromLinkId).getToNode();
//				Node toNode = network.getLinks().get(toLinkId).getToNode();
//
//				Path p = this.forwardMultiNodeDijkstra.constructPath(fromNode, toNode, previousActivity.getEndTime());
//				
//				((PathCostsNetwork) pathCosts).createRoute(fromLinkId, p, toLinkId);			
//			}
//			else if (planElementIndex == (actlegIndex + 2)) {
//				//adapt travel times: actToMove -> actPost
//				// replaced approximation: legTravelTime = getTravelTimeApproximation((PlanImpl)plan, (ActivityImpl)pe);
//				
//				Activity activity = (Activity) pe;
//				Id<Link> fromLinkId = activity.getLinkId();
//				Id<Link> toLinkId = actToMove.getLinkId();
//				Node fromNode = network.getLinks().get(fromLinkId).getToNode();
//				Node toNode = network.getLinks().get(toLinkId).getToNode();
//				
//				/*
//				 * Use activity.getStartTime() as time for the routing. Does this make sense?
//				 */
//				Path p = this.backwardMultiNodeDijkstra.constructPath(fromNode, toNode, ((Activity) pe).getStartTime());
//				((PathCostsNetwork) pathCosts).createRoute(fromLinkId, p, toLinkId);
//			}
//			else {
//				//use travel times from last iteration for efficiency reasons
//				Activity activity = (Activity) pe;
//				Leg previousLeg = PlanUtils.getPreviousLeg(plan, activity);
//				if (previousLeg.getRoute() != null) pathCosts.setRoute(previousLeg.getRoute());
//				
//				// Seems that this case occurs - not sure whether this is okay or not. cdobler oct'15
//				else pathCosts = this.getTravelTimeApproximation(plan, actToMove);
//			}
		}
		return pathCosts;
	}
	
	private PathCosts getTravelTimeApproximation(Plan plan, Activity act) {
		
		Leg previousLeg = PlanUtils.getPreviousLeg(plan, act);
		Activity actPre = PlanUtils.getPreviousActivity(plan, previousLeg);
		String mode = previousLeg.getMode();
//		Activity actPre = plan.getPreviousActivity(plan.getPreviousLeg(act));
//		String mode = plan.getPreviousLeg(act).getMode();
		PathCosts pathCosts = new PathCostsGeneric(this.network);
		
		// TODO: as soon as plansCalcRoute provides defaults for all modes use them 
		// I do not want users having to set dc parameters in other config modules!
		double speed = this.dccg.getTravelSpeed_car();	// seems to be overwritten in any case? cdobler, oct'14
		
		if (mode.equals(TransportMode.pt)) {
			speed = this.dccg.getTravelSpeed_pt();	
		} else if (mode.equals(TransportMode.bike)) {
			speed = this.teleportedModeSpeeds.get(TransportMode.bike);
		} else if (mode.equals(TransportMode.walk) || mode.equals(TransportMode.transit_walk)) {
			speed = this.teleportedModeSpeeds.get(TransportMode.walk);
		} else {
			speed = this.teleportedModeSpeeds.get(PlansCalcRouteConfigGroup.UNDEFINED);
		}
		((PathCostsGeneric) pathCosts).createRoute(
				this.network.getLinks().get(actPre.getLinkId()), 
				this.network.getLinks().get(act.getLinkId()),
				this.beelineDistanceFactors.get(PlansCalcRouteConfigGroup.UNDEFINED),
				speed);
		return pathCosts;
	}
	
	private PathCosts computeTravelTimeFromCompleteRouting(Plan plan, Activity fromAct, Activity toAct, String mode) {
		PathCosts pathCosts = new PathCostsNetwork(this.network);
		
		if (mode.equals(TransportMode.car)) {
			LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
			leg.setDepartureTime(0.0);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(0.0);
			
			try {
				PlanRouterAdapter.handleLeg(this.router, plan.getPerson(), leg, fromAct, toAct, fromAct.getEndTime());	
				pathCosts.setRoute(leg.getRoute());
			} catch (Exception e) {
				log.info("plan routing not working for person " + plan.getPerson().getId().toString());
				GenericRouteImpl route = new GenericRouteImpl(null, null);
				route.setTravelTime(Double.MAX_VALUE);
				route.setDistance(Double.MAX_VALUE);
				pathCosts.setRoute(route);
			}
			
	//	} else if (mode.equals(TransportMode.pt) && config.transit().isUseTransit()) {
		//	LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.pt);
			//leg.setDepartureTime(0.0);
			//leg.setTravelTime(0.0);
			//leg.setArrivalTime(0.0);
     		//PlanRouterAdapter.handleLeg(router, plan.getPerson(), leg, fromAct, toAct, fromAct.getEndTime());	
			
		} else {
			pathCosts = this.getTravelTimeApproximation(plan, toAct);
		}
		return pathCosts;
	}
}
