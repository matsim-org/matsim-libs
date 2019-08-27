/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripRemoverAlgorithm.java
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils.JointTravelStructure;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils.JointTrip;

/**
 * @author thibautd
 */
public class JointTripRemoverAlgorithm implements GenericPlanAlgorithm<JointPlan> {
	private static final Logger log =
		Logger.getLogger(JointTripRemoverAlgorithm.class);

	private final Random random;
	private final Set<String> stagesWithJointTypes;
	private final MainModeIdentifier mainModeIdentifier;

	public JointTripRemoverAlgorithm(
			final Random random,
			final MainModeIdentifier mainModeIdentifier) {
		this.random = random;
		this.stagesWithJointTypes = JointActingTypes.JOINT_STAGE_ACTS;
		this.mainModeIdentifier = mainModeIdentifier;
	}

	@Override
	public void run(final JointPlan plan) {
		run( plan , Collections.<Id<Person>>emptyList() );
	}

	public ActedUponInformation run( final JointPlan plan , final Collection<Id<Person>> agentsToIgnore ) {
		final JointTravelStructure structure = JointTravelUtils.analyseJointTravel(plan);

		if (structure.getJointTrips().size() == 0) {
			log.warn( getClass().getSimpleName()+" was called on a plan with no joint trips."
					+" Make sure it is what you want!" );
			return null;
		}

		final List<JointTrip> choiceSet = getChoiceSet( structure , agentsToIgnore );
		if (choiceSet.isEmpty()) return null;

		final JointTrip toRemove = choiceSet.get( random.nextInt( choiceSet.size() ) );

		removePassengerTrip( toRemove , plan );
		removeDriverTrip( toRemove , plan );
		return new ActedUponInformation(
				toRemove.getDriverId(),
				toRemove.getPassengerId() );
	}

	private static List<JointTrip> getChoiceSet(
			final JointTravelStructure structure,
			final Collection<Id<Person>> agentsToIgnore) {
		if (agentsToIgnore.isEmpty()) return structure.getJointTrips();

		final List<JointTrip> choiceSet = new ArrayList<JointTrip>();

		for (JointTrip t : structure.getJointTrips()) {
			if ( agentsToIgnore.contains( t.getDriverId() ) ) continue;
			if ( agentsToIgnore.contains( t.getPassengerId() ) ) continue;
			choiceSet.add( t );
		}

		return choiceSet;
	}

	// package protected for tests
	final void removePassengerTrip(
			final JointTrip toRemove,
			final JointPlan jointPlan) {
		final Plan passengerPlan = jointPlan.getIndividualPlan( toRemove.getPassengerId() );

		final Trip tripWithLeg =
			getTripWithLeg(
					passengerPlan,
					toRemove.getPassengerLeg(),
					stagesWithJointTypes );

		TripRouter.insertTrip(
				passengerPlan , 
				tripWithLeg.getOriginActivity(),
				Collections.singletonList( PopulationUtils.createLeg(TransportMode.pt) ),
				tripWithLeg.getDestinationActivity() );
	}

	private static Trip getTripWithLeg(
			final Plan plan,
			final Leg leg,
			final Set<String> stageActivityTypes) {
		for ( Trip t : TripStructureUtils.getTrips( plan , stageActivityTypes ) ) {
			if ( t.getTripElements().contains( leg ) ) return t;
		}
		throw new RuntimeException( plan.getPlanElements() +" doesn't contain "+leg );
	}

	// package protected for tests
	final void removeDriverTrip(
			final JointTrip toRemove,
			final JointPlan plan) {
		unregisterPassengerFromDriverRoutes( toRemove );
		repareDriverTrips( toRemove , plan );
	}

	private void repareDriverTrips(
			final JointTrip toRemove,
			final JointPlan plan) {
		final List<TripStructureUtils.Trip> subtrips = getDriverTrip( toRemove , plan.getIndividualPlan( toRemove.getDriverId() ) );
		final List<PlanElement> newTrip = new ArrayList<PlanElement>();
		newTrip.add( PopulationUtils.createLeg(TransportMode.car) );

		// "state" variables, changed in the loop:
		// - keeps track of the passengers currently in the vehicle.
		//   Pick-up or drop-offs are created at each change
		Set<Id> currentPassengers = Collections.<Id>emptySet();
		for ( TripStructureUtils.Trip subtrip : subtrips ) {
			final Leg leg = getDriverLegIfItIs( subtrip );
			final Route route = leg == null ? null : leg.getRoute();

			final Set<Id> newPassengers = route != null ?
				new HashSet<Id>( ((DriverRoute) route).getPassengersIds() ) :
				Collections.<Id>emptySet();
			// note that no check of the mode is done...
			if ( !newPassengers.equals( currentPassengers ) ) {
				newTrip.add(
						PopulationUtils.createActivityFromLinkId(JointActingTypes.INTERACTION, route != null ?
							route.getStartLinkId() :
								subtrip.getOriginActivity().getLinkId()) );

				// as the spatial structure of the trip is modified, it is possible
				// that some pre-existing subtours are removed. Thus, a driver that may
				// have walked to a pick up (because at the same location as its departure)
				// may then have to drive to pick up the second passenger directly if
				// the first passenger was removed. Setting all non-driver legs
				// to car ensures to have a consistent mode chain.
				// XXX It could be done in a smarter way, so that if no subtour is removed, no modification is done
				// For instance, when removing an "intern" trip, first PU and last DO are
				// left untouched, and thus access and egress leg need not be touched.
				newTrip.add( leg != null ? leg : PopulationUtils.createLeg(TransportMode.car) );
				currentPassengers = newPassengers;
			}
		}

		TripRouter.insertTrip(
				plan.getIndividualPlan( toRemove.getDriverId() ),
				subtrips.get( 0 ).getOriginActivity(),
				newTrip,
				subtrips.get( subtrips.size() - 1 ).getDestinationActivity() );
	}

	private Leg getDriverLegIfItIs(final Trip subtrip) {
		if ( !mainModeIdentifier.identifyMainMode( subtrip.getTripElements() ).equals( JointActingTypes.DRIVER ) ) return null;
		if ( subtrip.getLegsOnly().size() != 1 ) throw new RuntimeException( "unexpected driver subtrip length: "+subtrip );
		return subtrip.getLegsOnly().get( 0 );
	}

	private List<TripStructureUtils.Trip> getDriverTrip(
			final JointTrip toRemove,
			final Plan driverPlan) {
		final TripStructureUtils.Trip driverTrip = getTripWithLeg( driverPlan , toRemove.getDriverLegs().get( 0 ) , stagesWithJointTypes );
		assert driverTrip.getTripElements().containsAll( toRemove.getDriverLegs() );

		final List<PlanElement> elements = new ArrayList<PlanElement>();
		elements.add( driverTrip.getOriginActivity() );
		elements.addAll( driverTrip.getTripElements() );
		elements.add( driverTrip.getDestinationActivity() );
		return TripStructureUtils.getTrips( elements );
	}

	private static void unregisterPassengerFromDriverRoutes(
			final JointTrip toRemove) {
		for (Leg driverLeg : toRemove.getDriverLegs()) {
			final DriverRoute route = (DriverRoute) driverLeg.getRoute();
			route.removePassenger( toRemove.getPassengerId() );
			if ( route.getPassengersIds().isEmpty() ) {
				driverLeg.setMode( TransportMode.car );
				driverLeg.setRoute( null );
			}
		}
	}
}

