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
package playground.thibautd.socnetsim.cliques.replanning.modules.jointtripinsertor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.thibautd.socnetsim.cliques.config.JointTripInsertorConfigGroup;
import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.utils.RoutingUtils;

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
	private final MainModeIdentifier mainModeIdentifier;

	public JointTripInsertorAlgorithm(
			final Random random,
			final JointTripInsertorConfigGroup config,
			final TripRouter router) {
		this.router = router;
		chainBasedModes = config.getChainBasedModes();
		betaDetour = config.getBetaDetour();
		scale = config.getScale();
		this.random = random;
		this.mainModeIdentifier = new MainModeIdentifierImpl();
	}

	@Override
	public void run(final JointPlan plan) {
		run( plan , Collections.<Id>emptyList() );
	}

	public ActedUponInformation run(
			final JointPlan jointPlan,
			final Collection<Id> agentsToIgnore) {
		final ClassifiedTrips trips = extractClassifiedTrips( jointPlan , agentsToIgnore );
		final List<Match> matches = extractMatches( trips );
		if (matches.size() == 0) return null;

		final Match match = chooseMatch( matches );
		insertMatch( jointPlan , match );
		return match.toInformation();
	}

	private ClassifiedTrips extractClassifiedTrips(
			final JointPlan jointPlan,
			final Collection<Id> agentsToIgnore) {
		final ClassifiedTrips trips = new ClassifiedTrips();

		for (Map.Entry<Id, Plan> entry : jointPlan.getIndividualPlans().entrySet()) {
			final Id id = entry.getKey();
			if ( agentsToIgnore.contains( id ) ) continue;
			final Plan plan = entry.getValue();

			final Iterator<PlanElement> structure =
					RoutingUtils.tripsToLegs(
							plan,
							router.getStageActivityTypes(),
							mainModeIdentifier).iterator();

			// variables modified during the iteration
			Activity origin = (Activity) structure.next();
			double now = TripRouter.calcEndOfPlanElement( 0 , origin );
			while (structure.hasNext()) {
				final Leg leg = (Leg) structure.next();
				final Activity destination = (Activity) structure.next();

				if ( isElectableTrip( origin , leg , destination ) ) {
					if (leg.getMode().equals( TransportMode.car )) {
						trips.carTrips.add(
								new Trip(
									origin,
									destination,
									now,
									id,
									TransportMode.car) );
					}
					else if ( !chainBasedModes.contains( leg.getMode() ) ) {
						trips.nonChainBasedModeTrips.add(
								new Trip(
									origin,
									destination,
									now,
									id,
									leg.getMode()) );
					}
				}

				now = TripRouter.calcEndOfPlanElement( now , leg );
				now = TripRouter.calcEndOfPlanElement( now , destination );
				origin = destination;
			}
		}

		return trips;
	}

	private static boolean isElectableTrip(
			final Activity origin,
			final Leg leg,
			final Activity destination) {
		final String orType = origin.getType();
		final String destType = destination.getType();
		final String mode = leg.getMode();
		final boolean isPartOfJointTrip = 
			mode.equals( JointActingTypes.PASSENGER ) ||
			mode.equals( JointActingTypes.DRIVER ) ||
			destType.equals( JointActingTypes.PICK_UP ) ||
			orType.equals( JointActingTypes.DROP_OFF );
		return !isPartOfJointTrip;
	}

	private List<Match> extractMatches(final ClassifiedTrips trips) {
		final List<Match> matches = new ArrayList<Match>();

		for (Trip driverTrip : trips.carTrips) {
			final Id driverId = driverTrip.agentId;
			for (Trip passengerTrip : trips.nonChainBasedModeTrips) {
				if ( !driverId.equals( passengerTrip.agentId ) ) {
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

		return matches;
	}

	private double calcMatchCost(
			final Trip driverTrip,
			final Trip passengerTrip) {
		final double timeDiff = Math.abs( driverTrip.departureTime - passengerTrip.departureTime );
		final double detourDist = 
			CoordUtils.calcDistance( driverTrip.departure.getCoord() , passengerTrip.departure.getCoord() ) +
			CoordUtils.calcDistance( driverTrip.arrival.getCoord() , passengerTrip.arrival.getCoord() ) +
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

		// insert in driver plan
		List<PlanElement> driverTrip = new ArrayList<PlanElement>();
		driverTrip.add( new LegImpl( TransportMode.car ) );
		Activity act = new ActivityImpl(
				JointActingTypes.PICK_UP,
				match.tripPassenger.departure.getCoord(),
				match.tripPassenger.departure.getLinkId());
		act.setMaximumDuration( 0 );
		driverTrip.add( act );
		Leg leg =  new LegImpl( JointActingTypes.DRIVER );
		DriverRoute dRoute = new DriverRoute(
				match.tripPassenger.departure.getLinkId(),
				match.tripPassenger.arrival.getLinkId());
		dRoute.addPassenger( match.tripPassenger.agentId );
		leg.setRoute( dRoute );
		driverTrip.add( leg );
		act = new ActivityImpl(
				JointActingTypes.DROP_OFF,
				match.tripPassenger.arrival.getCoord(),
				match.tripPassenger.arrival.getLinkId());
		act.setMaximumDuration( 0 );
		driverTrip.add( act );
		driverTrip.add( new LegImpl( TransportMode.car ) );

		// insert in passenger plan
		List<PlanElement> passengerTrip = new ArrayList<PlanElement>();
		passengerTrip.add( new LegImpl( match.tripPassenger.initialMode ) );
		act = new ActivityImpl(
				JointActingTypes.PICK_UP,
				match.tripPassenger.departure.getCoord(),
				match.tripPassenger.departure.getLinkId());
		act.setMaximumDuration( 0 );
		passengerTrip.add( act );
		leg =  new LegImpl( JointActingTypes.PASSENGER );
		PassengerRoute pRoute = new PassengerRoute(
				match.tripPassenger.departure.getLinkId(),
				match.tripPassenger.arrival.getLinkId());
		pRoute.setDriverId( match.tripDriver.agentId );
		leg.setRoute( pRoute );
		passengerTrip.add( leg );
		act = new ActivityImpl(
				JointActingTypes.DROP_OFF,
				match.tripPassenger.arrival.getCoord(),
				match.tripPassenger.arrival.getLinkId());
		act.setMaximumDuration( 0 );
		passengerTrip.add( act );
		passengerTrip.add( new LegImpl( match.tripPassenger.initialMode ) );

		TripRouter.insertTrip( driverPlan , match.tripDriver.departure , driverTrip , match.tripDriver.arrival );
		TripRouter.insertTrip( passengerPlan , match.tripPassenger.departure , passengerTrip , match.tripPassenger.arrival );
	}

	// /////////////////////////////////////////////////////////////////////////
	// helper classes
	// /////////////////////////////////////////////////////////////////////////
	private static class ClassifiedTrips {
		public final List<Trip> carTrips = new ArrayList<Trip>();
		public final List<Trip> nonChainBasedModeTrips = new ArrayList<Trip>();
	}

	private static class Trip {
		final Activity departure;
		final Activity arrival;
		final double departureTime;
		final double length;
		final Id agentId;
		final String initialMode;

		public Trip(
				final Activity departure,
				final Activity arrival,
				final double departureTime,
				final Id agentId,
				final String initialMode) {
			this.departure = departure;
			this.arrival = arrival;
			this.departureTime = departureTime;
			this.length = CoordUtils.calcDistance( departure.getCoord() , arrival.getCoord() );
			this.agentId = agentId;
			this.initialMode = initialMode;
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

