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
package playground.thibautd.socnetsim.cliques.replanning.modules.jointtripinsertor;

import playground.thibautd.socnetsim.utils.JointPlanUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.utils.JointPlanUtils.JointTravelStructure;
import playground.thibautd.socnetsim.utils.JointPlanUtils.JointTrip;

/**
 * @author thibautd
 */
public class JointTripRemoverAlgorithm implements GenericPlanAlgorithm<JointPlan> {
	private static final Logger log =
		Logger.getLogger(JointTripRemoverAlgorithm.class);

	private final Random random;

	public JointTripRemoverAlgorithm(final Random random) {
		this.random = random;
	}

	@Override
	public void run(final JointPlan plan) {
		run( plan , Collections.<Id>emptyList() );
	}

	public ActedUponInformation run( final JointPlan plan , final Collection<Id> agentsToIgnore ) {
		final JointTravelStructure structure = JointPlanUtils.analyseJointTravel( plan );

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
			final Collection<Id> agentsToIgnore) {
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
	final static void removePassengerTrip(
			final JointTrip toRemove,
			final JointPlan jointPlan) {
		final Plan passengerPlan = jointPlan.getIndividualPlan( toRemove.getPassengerId() );
		final List<PlanElement> pes = passengerPlan.getPlanElements();
		final int index = pes.indexOf( toRemove.getPassengerLeg() ) - 2;

		for (int i=0; i < 5; i++) {
			pes.remove( index );
		}
		// TODO: verify mode of subtour?
		pes.add( index , new LegImpl( TransportMode.pt ) );
	}

	// package protected for tests
	final static void removeDriverTrip(
			final JointTrip toRemove,
			final JointPlan plan) {
		unregisterPassengerFromDriverRoutes( toRemove );
		repareDriverTrips( toRemove , plan );
	}

	private static void repareDriverTrips(
			final JointTrip toRemove,
			final JointPlan plan) {
		final List<PlanElement> allPes = plan.getIndividualPlan( toRemove.getDriverId() ).getPlanElements();
		final List<PlanElement> trip = getDriverTrip( toRemove , plan.getIndividualPlan( toRemove.getDriverId() ) );
		final List<PlanElement> newTrip = new ArrayList<PlanElement>();
		newTrip.add( new LegImpl( TransportMode.car ) );

		final int start = allPes.indexOf( trip.get( 0 ) ) - 1;

		// "state" variables, changed in the loop:
		// - keeps track of the passengers currently in the vehicle.
		//   Pick-up or drop-offs are created at each change
		Set<Id> currentPassengers = Collections.<Id>emptySet();
		// - stores the Id of the last activity, in case we cannot get this info
		//   from the Route
		Id lastActLinkId = ((Activity) allPes.get( start )).getLinkId();
		for (PlanElement pe : trip ) {
			if ( !(pe instanceof Leg) ) {
				// this is collected just in case some legs have no route
				// XXX: This will fail without crash without a strict Act/leg alternance!
				lastActLinkId = ((Activity) pe).getLinkId();
				continue;
			}
			final Leg leg = (Leg) pe;
			final Route route = leg.getRoute();

			final Set<Id> newPassengers = route instanceof DriverRoute ?
				new TreeSet<Id>( ((DriverRoute) route).getPassengersIds() ) :
				Collections.<Id>emptySet();
			// note that no check of the mode is done...
			if ( !newPassengers.equals( currentPassengers ) ) {
				// note that this allows passengers to be pick-up at drop offs
				// (and reverse) if more passengers are droped off that picked up
				// (or reverse).
				if ( newPassengers.size() > currentPassengers.size() ) {
					newTrip.add(
							new ActivityImpl(
								JointActingTypes.PICK_UP,
								route != null ?
									route.getStartLinkId() :
									lastActLinkId ) );
				}
				else {
					newTrip.add(
							new ActivityImpl(
								JointActingTypes.DROP_OFF,
								route != null ?
									route.getStartLinkId() :
									lastActLinkId ) );
				}
				// as the spatial structure of the trip is modified, it is possible
				// that some pre-existing subtours are removed. Thus, a driver that may
				// have walked to a pick up (because at the same location as its departure)
				// may then have to drive to pick up the second passenger directly if
				// the first passenger was removed. Setting all non-driver legs
				// to car ensures to have a consistent mode chain.
				// XXX It could be done in a smarter way, so that if no subtour is removed, no modification is done
				// For instance, when removing an "intern" trip, first PU and last DO are
				// left untouched, and thus access and egress leg need not be touched.
				newTrip.add(
						leg.getMode().equals( JointActingTypes.DRIVER ) ?
							leg :
							new LegImpl( TransportMode.car ));
				currentPassengers = newPassengers;
			}
		}

		trip.clear();
		trip.addAll( newTrip );
	}

	private static List<PlanElement> getDriverTrip(
			final JointTrip toRemove,
			final Plan driverPlan) {
		boolean isTheTrip = false;

		int i = 0;
		int startTrip = 0;
		for (PlanElement pe : driverPlan.getPlanElements()) {
			if (pe instanceof Leg ||
					JointActingTypes.JOINT_STAGE_ACTS.isStageActivity( ((Activity) pe).getType() )) {
				if (toRemove.getDriverLegs().contains( pe )) {
					isTheTrip = true;
				}
			}
			else {
				if (isTheTrip) return driverPlan.getPlanElements().subList( startTrip , i );
				startTrip = i + 1;
			}
			i++;
		}
		if (isTheTrip) return driverPlan.getPlanElements().subList( startTrip , i );

		throw new RuntimeException( "this part of the code should be unreachable" );
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

