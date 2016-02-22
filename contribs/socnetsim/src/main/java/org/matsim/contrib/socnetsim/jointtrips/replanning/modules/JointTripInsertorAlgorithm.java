/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripInsertorAlgorithm.java
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
package org.matsim.contrib.socnetsim.jointtrips.replanning.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;

import org.matsim.core.utils.collections.MapUtils;
import org.matsim.contrib.socnetsim.framework.cliques.config.JointTripInsertorConfigGroup;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.jointtrips.JointMainModeIdentifier;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils.JointTravelStructure;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils.JointTrip;

/**
 * An algorithm which creates joint trips from nothing,
 * by grouping a car trip with a non-chain-based-mode trip.
 * @author thibautd
 */
public class JointTripInsertorAlgorithm implements GenericPlanAlgorithm<JointPlan> {
	private final TripRouter router;
	private final List<String> chainBasedModes;
	private final double betaDetour;
	private final double scale;
	private final Random random;

	private final SocialNetwork socialNetwork;

	public JointTripInsertorAlgorithm(
			final Random random,
			final SocialNetwork socialNetwork,
			final JointTripInsertorConfigGroup config,
			final TripRouter router) {
		this.router = router;
		this.socialNetwork = socialNetwork;
		chainBasedModes = config.getChainBasedModes();
		betaDetour = config.getBetaDetour();
		scale = config.getScale();
		this.random = random;
	}

	@Override
	public void run(final JointPlan plan) {
		run( plan , Collections.<Id<Person>>emptyList() );
	}

	public ActedUponInformation run(
			final JointPlan jointPlan,
			final Collection<Id<Person>> agentsToIgnore) {
		final ClassifiedTrips trips = extractClassifiedTrips( jointPlan , agentsToIgnore );
		final List<Match> matches =
			extractMatches(
					jointPlan,
					trips );
		if (matches.size() == 0) return null;

		final Match match = chooseMatch( matches );
		insertMatch( jointPlan , match );
		return match.toInformation();
	}

	private ClassifiedTrips extractClassifiedTrips(
			final JointPlan jointPlan,
			final Collection<Id<Person>> agentsToIgnore) {
		final ClassifiedTrips trips = new ClassifiedTrips();

		for (Map.Entry<Id<Person>, Plan> entry : jointPlan.getIndividualPlans().entrySet()) {
			final Id id = entry.getKey();
			if ( agentsToIgnore.contains( id ) ) continue;
			final Plan plan = entry.getValue();

			// identify the joint trips as one single trips.
			// Otherwise, the process will insert joint trips to access pick-ups
			// or go from drop offs...
			final CompositeStageActivityTypes types = new CompositeStageActivityTypes();
			types.addActivityTypes( router.getStageActivityTypes() );
			types.addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );

			final MainModeIdentifier mainModeIdentifier =
				new JointMainModeIdentifier(
						router.getMainModeIdentifier() );

			for ( TripStructureUtils.Trip trip : TripStructureUtils.getTrips( plan , types ) ) {
				final String mode = mainModeIdentifier.identifyMainMode( trip.getTripElements() );

				if ( mode.equals( TransportMode.car ) ) {
					trips.addCarTrip(
							new Trip(
								trip.getOriginActivity(),
								trip.getDestinationActivity(),
								calcEndOfActivity( trip.getOriginActivity() , plan ),
								id) );
				}
				else if (
						!JointActingTypes.JOINT_MODES.contains( mode ) &&
						!chainBasedModes.contains( mode ) ) {
					trips.addNonChainBasedTrip(
							new Trip(
								trip.getOriginActivity(),
								trip.getDestinationActivity(),
								calcEndOfActivity( trip.getOriginActivity() , plan ),
								id) );
				}
			}
		}

		return trips;
	}

	private List<Match> extractMatches(
			final JointPlan jointPlan,
			final ClassifiedTrips trips) {
		final List<Match> matches = new ArrayList<Match>();
		final JointTravelStructure structure = JointTravelUtils.analyseJointTravel(jointPlan);

		for ( Map.Entry<Id<Person>, List<Trip>> eDriver : trips.carTrips.entrySet() ) {
			final Id<Person> driverId = eDriver.getKey();
			for ( Map.Entry<Id<Person>, List<Trip>> ePass : trips.nonChainBasedModeTrips.entrySet() ) {
				final Id<Person> passengerId = ePass.getKey();
				if ( acceptAgentMatch( driverId , passengerId ) ) {
					addMatches( matches , jointPlan , structure , eDriver.getValue() , ePass.getValue() );
				}
			}
		}

		return matches;
	}

	private void addMatches(
			final List<Match> matches,
			final JointPlan jointPlan,
			final JointTravelStructure structure,
			final List<Trip> driverTrips,
			final List<Trip> passengerTrips) {
		for ( Trip driverTrip : driverTrips ) {
			for (Trip passengerTrip : passengerTrips ) {
				if ( isInCorrectSequence( jointPlan , structure , driverTrip , passengerTrip ) ) {
					matches.add(
							new Match(
								driverTrip,
								passengerTrip,
								calcMatchCost(
									driverTrip,
									passengerTrip )));
				}
			}
		}
	}

	private boolean acceptAgentMatch(
			final Id driver,
			final Id passenger ) {
		if ( driver.equals( passenger ) ) return false;
		if ( socialNetwork == null ) return true;

		final boolean passengerAlterOfDriver =
			socialNetwork.getAlters( driver ).contains( passenger );

		if ( socialNetwork.isReflective() ) return passengerAlterOfDriver;

		return passengerAlterOfDriver || 
			socialNetwork.getAlters( passenger ).contains( driver );
	}

	private static boolean isInCorrectSequence(
			final JointPlan jointPlan,
			final JointTravelStructure structure,
			final Trip driverTrip,
			final Trip passengerTrip) {
		final List<JointTrip> jointTrips =
				structure.getJointTripsForCotravelers(
						driverTrip.agentId,
						passengerTrip.agentId);
		final int positionInDriverPlan = getPosition( jointPlan , jointTrips , driverTrip );
		final int positionInPassengerPlan = getPosition( jointPlan , jointTrips , passengerTrip );
		return positionInDriverPlan == positionInPassengerPlan;
	}

	private static int getPosition(
			final JointPlan jointPlan,
			final List<JointTrip> jointTrips,
			final Trip trip) {
		final Plan plan = jointPlan.getIndividualPlan( trip.agentId );

		final int indexOfTrip = plan.getPlanElements().indexOf( trip.departure );

		// count joint trips occuring before the candidate trip
		int pos = 0;
		for ( JointTrip jt : jointTrips ) {
			final int indexOfJointTrip =
				Math.max(
						plan.getPlanElements().indexOf( jt.getPassengerLeg() ),
						plan.getPlanElements().indexOf( jt.getDriverLegs().get( 0 ) ) );
			assert indexOfJointTrip >= 0;
			if ( indexOfJointTrip < indexOfTrip ) pos++;
		}

		return pos;
	}

	private double calcMatchCost(
			final Trip driverTrip,
			final Trip passengerTrip) {
		final double timeDiff = Math.abs( driverTrip.departureTime - passengerTrip.departureTime );
		final double detourDist = 
			CoordUtils.calcEuclideanDistance( driverTrip.departure.getCoord() , passengerTrip.departure.getCoord() ) +
			CoordUtils.calcEuclideanDistance( driverTrip.arrival.getCoord() , passengerTrip.arrival.getCoord() ) +
			passengerTrip.length - driverTrip.length;
			
		return scale * (timeDiff + betaDetour * detourDist);
	}

	private Match chooseMatch(final List<Match> matches) {
		final double[] thresholds = new double[ matches.size() ];

		double sum = 0;
		int i=0;
		for (Match match : matches) {
			sum += Math.exp( -match.cost );
			thresholds[i] = sum;
			i++;
		}

		final double choice = random.nextDouble() * sum;

		for (i=0; i < thresholds.length; i++) {
			if (choice <= thresholds[i]) {
				return matches.get( i );
			}
		}

		throw new RuntimeException( "choice procedure failed! this should not happen! choice="+choice+" in thresholds="+Arrays.toString(thresholds) );
	}

	private static void insertMatch(
			final JointPlan jointPlan,
			final Match match) {
		final Plan driverPlan = jointPlan.getIndividualPlans().get( match.tripDriver.agentId );
		final Plan passengerPlan = jointPlan.getIndividualPlans().get( match.tripPassenger.agentId );

		/* scope of driver-specific variables */ {
			// insert in driver plan
			final List<PlanElement> driverTrip = new ArrayList<PlanElement>();
			driverTrip.add( new LegImpl( TransportMode.car ) );
			/* scope of firstAct */ {
				final Activity firstAct = new ActivityImpl(
						JointActingTypes.INTERACTION,
						match.tripPassenger.departure.getCoord(),
						match.tripPassenger.departure.getLinkId());
				firstAct.setMaximumDuration( 0 );
				driverTrip.add( firstAct );
			}
			/* scope of leg */ {
				final Leg leg =  new LegImpl( JointActingTypes.DRIVER );
				final DriverRoute dRoute = new DriverRoute(
						match.tripPassenger.departure.getLinkId(),
						match.tripPassenger.arrival.getLinkId());
				dRoute.addPassenger( match.tripPassenger.agentId );
				leg.setRoute( dRoute );
				driverTrip.add( leg );
			}
			/* scope of secondAct */ {
				final Activity secondAct = new ActivityImpl(
						JointActingTypes.INTERACTION,
						match.tripPassenger.arrival.getCoord(),
						match.tripPassenger.arrival.getLinkId());
				secondAct.setMaximumDuration( 0 );
				driverTrip.add( secondAct );
			}
			driverTrip.add( new LegImpl( TransportMode.car ) );

			TripRouter.insertTrip(
					driverPlan,
					match.tripDriver.departure,
					driverTrip,
					match.tripDriver.arrival );
		}

		/* scope of passenger-specific variables */ {
			// insert in passenger plan
			final Leg pLeg =  new LegImpl( JointActingTypes.PASSENGER );
			final PassengerRoute pRoute = new PassengerRoute(
					match.tripPassenger.departure.getLinkId(),
					match.tripPassenger.arrival.getLinkId());
			pRoute.setDriverId( match.tripDriver.agentId );
			pLeg.setRoute( pRoute );


			TripRouter.insertTrip(
					passengerPlan,
					match.tripPassenger.departure,
					Collections.singletonList( pLeg ),
					match.tripPassenger.arrival );
		}
	}

	private static double calcEndOfActivity(
			final Activity activity,
			final Plan plan) {
		if (activity.getEndTime() != Time.UNDEFINED_TIME) return activity.getEndTime();

		// no sufficient information in the activity...
		// do it the long way.
		// XXX This is inefficient! Using a cache for each plan may be an option
		// (knowing that plan elements are iterated in proper sequence,
		// no need to re-examine the parts of the plan already known)
		double now = 0;

		for (PlanElement pe : plan.getPlanElements()) {
			now = updateNow( now , pe );
			if (pe == activity) return now;
		}

		throw new RuntimeException( "activity "+activity+" not found in "+plan.getPlanElements() );
	}

	private static double updateNow(
			final double now,
			final PlanElement pe) {
		if (pe instanceof Activity) {
			Activity act = (Activity) pe;
			double endTime = act.getEndTime();
			double startTime = act.getStartTime();
			double dur = (act instanceof ActivityImpl ? ((ActivityImpl) act).getMaximumDuration() : Time.UNDEFINED_TIME);
			if (endTime != Time.UNDEFINED_TIME) {
				// use fromAct.endTime as time for routing
				return endTime;
			}
			else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
				// use fromAct.startTime + fromAct.duration as time for routing
				return startTime + dur;
			}
			else if (dur != Time.UNDEFINED_TIME) {
				// use last used time + fromAct.duration as time for routing
				return now + dur;
			}
			else {
				throw new RuntimeException("activity has neither end-time nor duration." + act);
			}
		}
		double tt = ((Leg) pe).getTravelTime();
		return now + (tt != Time.UNDEFINED_TIME ? tt : 0);
	}	

	// /////////////////////////////////////////////////////////////////////////
	// helper classes
	// /////////////////////////////////////////////////////////////////////////
	private static class ClassifiedTrips {
		public final Map<Id<Person>, List<Trip>> carTrips = new HashMap< >();
		public final Map<Id<Person>, List<Trip>> nonChainBasedModeTrips = new HashMap< >();

		public void addCarTrip( final Trip t ) {
			MapUtils.getList( t.agentId , carTrips ).add( t );
		}

		public void addNonChainBasedTrip( final Trip t ) {
			MapUtils.getList( t.agentId , nonChainBasedModeTrips ).add( t );
		}
	}

	private static class Trip {
		final Activity departure;
		final Activity arrival;
		final double departureTime;
		final double length;
		final Id<Person> agentId;

		public Trip(
				final Activity departure,
				final Activity arrival,
				final double departureTime,
				final Id<Person> agentId) {
			this.departure = departure;
			this.arrival = arrival;
			this.departureTime = departureTime;
			this.length = CoordUtils.calcEuclideanDistance( departure.getCoord() , arrival.getCoord() );
			this.agentId = agentId;
		}
	}

	/**
	 * a "record" class with all information relative to a match
	 */
	private static class Match {
		final Trip tripDriver;
		final Trip tripPassenger;
		final double cost;

		public Match(
				final Trip tripDriver,
				final Trip tripPassenger,
				final double cost) {
			this.tripDriver = tripDriver;
			this.tripPassenger = tripPassenger;
			this.cost = cost;
		}

		public ActedUponInformation toInformation() {
			return new ActedUponInformation(
					tripDriver.agentId,
					tripPassenger.agentId);
		}
	}
}

