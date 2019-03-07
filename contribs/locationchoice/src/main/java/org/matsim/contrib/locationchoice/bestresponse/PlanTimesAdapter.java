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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.FacilitiesUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class PlanTimesAdapter {
	private static final Logger log = Logger.getLogger( PlanTimesAdapter.class ) ;

	private final DestinationChoiceConfigGroup.ApproximationLevel approximationLevel;
	private final Network network;
	private final Config config;
	private final Map<String, Double> teleportedModeSpeeds;
	private final Map<String, Double> beelineDistanceFactors;
	private final DestinationChoiceConfigGroup dccg;
	private final TripRouter router;

	/* package */ PlanTimesAdapter(
			final DestinationChoiceConfigGroup.ApproximationLevel approximationLevel,
			final TripRouter router,
			final Scenario scenario,
			final Map<String, Double> teleportedModeSpeeds,
			final Map<String, Double> beelineDistanceFactors) {
		this.approximationLevel = approximationLevel;
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
	 * 	- completeRouting: calculate route for EACH leg
	 * 	- localRouting: calculate new routes from and to the adapted activity, take other route information from existing leg
	 * 	- noRouting: same as localRouting but with estimated travel times for the calculated routes
	 * 
	 */
	/* package */ double adaptTimesAndScorePlan(
			final Plan plan,
			final Plan planTmp,
			final ScoringFunctionFactory scoringFunctionFactory ) {

		// yyyy Note: getPrevious/NextLeg/Activity all relies on alternating activities and leg, which was given up as a requirement
		// a long time ago (which is why it is not in the interface).  kai, jan'13

		final ScoringFunction scoringFunction = scoringFunctionFactory.createNewScoringFunction(plan.getPerson());

		// iterate through plan and adapt travel and activity times
		boolean isFirstActivity = true;
		for ( Activity act : TripStructureUtils.getActivities( plan , EmptyStageActivityTypes.INSTANCE ) ) {
			final int planElementIndex = plan.getPlanElements().indexOf( act );
			if ( isFirstActivity ) {
				isFirstActivity = false;
				final Activity actTmp = (Activity) planTmp.getPlanElements().get(planElementIndex);
				// this used to assume the activity starts at 00:00:00, but this
				// is not what happens in the iterations: the first activity
				// has no start time, the last has no end time.
				actTmp.setStartTime( Time.UNDEFINED_TIME );
				scoringFunction.handleActivity( actTmp );
				continue;
			}

			final List<? extends PlanElement> trip = estimateTravelTime( plan, act );

			for ( PlanElement pe : trip ) {
				if ( pe instanceof Activity ) {
					// ignore this for the time being (otherwise, need to check times and re-create start and end time
					// scoringFunction.handleActivity( (Activity) pe );
				}
				else if ( pe instanceof Leg ){
					final Leg leg = (Leg) pe;
					Gbl.assertIf( !Time.isUndefinedTime( leg.getDepartureTime() ) ) ;
					Gbl.assertIf( !Time.isUndefinedTime( leg.getTravelTime() ) );
					scoringFunction.handleLeg( leg );
				}
				else {
					throw new RuntimeException( "unknown plan element type? "+pe.getClass().getName() );
				}
			}

			double actDur = act.getMaximumDuration();

			final Activity prevActTmp = (Activity) planTmp.getPlanElements().get(planElementIndex - 2);
			double departureTime = 
				prevActTmp.getEndTime() != Time.UNDEFINED_TIME ?
					prevActTmp.getEndTime() :
					prevActTmp.getStartTime() + prevActTmp.getMaximumDuration();

			if ( departureTime == Time.UNDEFINED_TIME ) {
				throw new RuntimeException( "could not infer departure time after activity "+prevActTmp );
			}

			final double arrivalTime = departureTime + getTravelTime( trip );

			if ( arrivalTime == Time.UNDEFINED_TIME ) {
				throw new RuntimeException( "could not infer arrival time after trip "+trip );
			}

			// yyyy I think this is really more complicated than it should be since it could/should just copy the time structure of the 
			// original plan and not think so much.  kai, jan'13

			// set new activity end time
			// yy Isn't this the same as "act"?  (The code originally was a bit different, but in the original code it was also not using the
			// iterated element.)  kai, jan'13
			// No, it is not: this one comes from planTmp, not plan... Not sure why this duplication is there, though. td may '14
			final Activity actTmp = (Activity) planTmp.getPlanElements().get(planElementIndex);

			actTmp.setStartTime(arrivalTime);

			final PlansConfigGroup.ActivityDurationInterpretation actDurInterpr = ( config.plans().getActivityDurationInterpretation() ) ;
			switch ( actDurInterpr ) {
			case tryEndTimeThenDuration:
				if ( act.getEndTime() == Time.UNDEFINED_TIME && act.getMaximumDuration() != Time.UNDEFINED_TIME) {
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

	private double getTravelTime( List<? extends PlanElement> trip ) {
		double tt = 0;
		for ( PlanElement pe : trip ) {
			if ( pe instanceof Leg ) {
				final double curr = ((Leg) pe).getTravelTime();
				if ( curr == Time.UNDEFINED_TIME ) throw new RuntimeException( "undefined travel time in "+pe );
				tt += curr;
			}
		}
		return tt;
	}

	private List<? extends PlanElement> estimateTravelTime( Plan plan , Activity act ) {
		// Ouch... should be passed as parameter!
		final Leg previousLeg = LCPlanUtils.getPreviousLeg( plan, act );
		final Activity previousActivity = LCPlanUtils.getPreviousActivity( plan, previousLeg );
		final String mode = previousLeg.getMode();

		switch ( this.approximationLevel ) {
			case completeRouting:

				Level lvl = Level.INFO ;

				final List<? extends PlanElement> trip =
						this.router.calcRoute(
							  mode,
								new ActivityWrapperFacility( previousActivity ),
								new ActivityWrapperFacility( act ),
								previousActivity.getEndTime(),
							  plan.getPerson() );
				//		log.log(lvl,"") ;
				//		for( PlanElement planElement : trip ){
				//			log.log(lvl, planElement ) ;
				//		}
				//		log.log(lvl,"") ;
				//		fillInLegTravelTimes( fromAct.getEndTime() , trip );
				//		log.log(lvl,"") ;
				//		for( PlanElement planElement : trip ){
				//			log.log(lvl, planElement ) ;
				//		}
				//		log.log(lvl,"") ;
				return trip;
			case noRouting:
				// Yes, those two are doing the same. I passed some time to simplify the (rather convoluted) code,
				// and it boiled down to this. No idea of since how long this is not doing what it is claiming to do...
				// The only difference is that "noRouting" was getting travel times from the route for car if it exists
				// td dec 15
				if ( mode.equals( TransportMode.car ) && previousLeg.getTravelTime() != Time.UNDEFINED_TIME ) {
					return Collections.singletonList( previousLeg );
				}
				// fall through to local routing if not car or previous travel time not found
			case localRouting:
				return getTravelTimeApproximation(
						previousActivity,
						act,
						mode );
			default:
				throw new RuntimeException( "unknown method "+this.approximationLevel );
		}
	}

	private static void fillInLegTravelTimes(
			final double departureTime,
			final List<? extends PlanElement> trip ) {
		double time = departureTime;
		for ( PlanElement pe : trip ) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg leg = (Leg) pe;
			if ( leg.getDepartureTime() == Time.UNDEFINED_TIME ) {
				leg.setDepartureTime( time );
			}
			if ( leg.getTravelTime() == Time.UNDEFINED_TIME ) {
				leg.setTravelTime( leg.getRoute().getTravelTime() );
			}
			time += leg.getTravelTime();
		}
	}

	private List<? extends PlanElement> getTravelTimeApproximation(
			Activity fromAct,
			Activity toAct,
			String mode ) {
		// TODO: as soon as plansCalcRoute provides defaults for all modes use them
		// I do not want users having to set dc parameters in other config modules!
		double speed;

		// Those two parameters could probably be defined with more flexibility...
		if ( mode.equals( TransportMode.pt )) {
			speed = this.dccg.getTravelSpeed_pt();
		}
		else if ( mode.equals( TransportMode.car ) ) {
			speed = this.dccg.getTravelSpeed_car();
		}
		else if ( mode.equals( TransportMode.bike )) {
			speed = this.teleportedModeSpeeds.get( TransportMode.bike );
		}
		else if ( mode.equals( TransportMode.walk ) || mode.equals( TransportMode.transit_walk )) {
			speed = this.teleportedModeSpeeds.get(TransportMode.walk);
		}
		else {
			speed = this.teleportedModeSpeeds.get(PlansCalcRouteConfigGroup.UNDEFINED);
		}

		// This is the only place where this class is used: delete it? td dec 15
		final PathCostsGeneric pathCosts = new PathCostsGeneric( network );
		pathCosts.createRoute(
				this.network.getLinks().get( fromAct.getLinkId() ),
				this.network.getLinks().get( toAct.getLinkId() ),
				this.beelineDistanceFactors.get( PlansCalcRouteConfigGroup.UNDEFINED ),
				speed );
		final Leg l = PopulationUtils.createLeg(mode);
		l.setRoute( pathCosts.getRoute() );
		l.setTravelTime( pathCosts.getRoute().getTravelTime() );
		l.setDepartureTime( fromAct.getEndTime() );
		return Collections.singletonList( l );
	}

}
