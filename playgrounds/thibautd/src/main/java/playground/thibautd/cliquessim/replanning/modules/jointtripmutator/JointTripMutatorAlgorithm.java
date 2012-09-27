/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripMutatorAlgorithm.java
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
package playground.thibautd.cliquessim.replanning.modules.jointtripmutator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.cliquessim.population.DriverRoute;
import playground.thibautd.cliquessim.population.JointActingTypes;
import playground.thibautd.cliquessim.population.JointPlan;
import playground.thibautd.cliquessim.population.PassengerRoute;
import playground.thibautd.cliquessim.population.jointtrippossibilities.JointTripPossibilities;
import playground.thibautd.cliquessim.population.jointtrippossibilities.JointTripPossibilities.Od;
import playground.thibautd.cliquessim.population.jointtrippossibilities.JointTripPossibilities.Possibility;

/**
 * @author thibautd
 */
public class JointTripMutatorAlgorithm implements PlanAlgorithm {
	private final Network network;
	private final TripRouter tripRouter;
	private final JointTripPossibilities possibilities;
	private final Random random;

	private final StageActivityTypes checker = new StageActivityTypes() {
		@Override
		public boolean isStageActivity(final String activityType) {
			return tripRouter.getStageActivityTypes().isStageActivity( activityType ) ||
				JointActingTypes.PICK_UP.equals( activityType ) ||
				JointActingTypes.DROP_OFF.equals( activityType );
		}
	};

	public JointTripMutatorAlgorithm(
			final Network network,
			final TripRouter tripRouter,
			final JointTripPossibilities possibilities,
			final Random random) {
		this.network = network;
		this.possibilities = possibilities;
		this.random = random;
		this.tripRouter = tripRouter;
	}

	@Override
	public void run(final Plan plan) {
		jointRun( (JointPlan) plan );
	}

	private void jointRun(final JointPlan plan) {
		List<Possibility> cliquePossibilities = new ArrayList<Possibility>();

		for (Id person : plan.getClique().getMembers().keySet()) {
			cliquePossibilities.addAll( possibilities.getDriverPossibilities( person ) );
		}

		if (cliquePossibilities.size() == 0) return;

		while ( true ) {
			if (cliquePossibilities.size() > 0) {
				Possibility toMutate = cliquePossibilities.remove( random.nextInt( cliquePossibilities.size() ) );
				if (mutate( plan , toMutate )) {
					break;
				}
			}
			else {
				break;
			}
		}
	}

	private boolean mutate(
			final JointPlan plan,
			final Possibility toMutate) {
		Iterator<PlanElement> passengerElements = plan.getIndividualPlans().get( toMutate.getPassenger() ).getPlanElements().iterator();
		List<PlanElement> driverElements = plan.getIndividualPlans().get( toMutate.getDriver() ).getPlanElements();
		List<Tuple<Integer, Integer>> performedJointTrips = new ArrayList<Tuple<Integer, Integer>>();

		Activity origin = null;
		DriverRoute driverRoute = null;
		int count = -1;
		int driverLegIndex = -1;
		for (PlanElement driverElement : driverElements) {
			count++;
			if (driverElement instanceof Activity) {
				String type = ((Activity) driverElement).getType();
				if (type.equals( JointActingTypes.PICK_UP ) ||
						type.equals( JointActingTypes.DROP_OFF )) {
					continue;
				}
				else if (driverRoute != null) {
					if (origin.getLinkId().equals( toMutate.getDriverOd().getOriginLinkId() ) &&
						((Activity) driverElement).getLinkId().equals( toMutate.getDriverOd().getDestinationLinkId() ) ) {
						// get the passenger information
						int passengerLegIndex = 
							plan.getIndividualPlans().get( toMutate.getPassenger() )
								.getPlanElements().indexOf(
									getNextPassengerLeg( passengerElements , toMutate ) );
						performedJointTrips.add( new Tuple<Integer, Integer>( driverLegIndex , passengerLegIndex ) );
					}
					driverRoute = null;
				}
				origin = (Activity) driverElement;
			}
			else if (((Leg) driverElement).getMode().equals( JointActingTypes.DRIVER )) {
				driverRoute = (DriverRoute) ((Leg) driverElement).getRoute();
				if (!driverRoute.getPassengersIds().contains( toMutate.getPassenger() )) {
					driverRoute = null;
				}
				else {
					driverLegIndex = count;
				}
			}
		}

		if (performedJointTrips.size() == 0) {
			// FIXME: the insertion is buggy
			//insert( plan , toMutate );
			return false;
		}
		else {
			individualise( toMutate , performedJointTrips , plan);
			return true;
		}
	}

	private void individualise(
			final Possibility toMutate,
			final List<Tuple<Integer, Integer>> performedJointTrips,
			final JointPlan plan) {
		Tuple<Integer, Integer> toRemove = performedJointTrips.get( random.nextInt( performedJointTrips.size() ) );
		int driverLegIndex = toRemove.getFirst();
		int passengerLegIndex = toRemove.getSecond();

		List<PlanElement> driverPes = plan.getIndividualPlans().get( toMutate.getDriver() ).getPlanElements();
		List<PlanElement> passengerPes = plan.getIndividualPlans().get( toMutate.getPassenger() ).getPlanElements();
		
		Leg driverLeg = (Leg) driverPes.get( driverLegIndex );
		DriverRoute driverRoute = (DriverRoute) driverLeg.getRoute();

		if (driverRoute.getPassengersIds().size() > 1) {
			driverRoute.removePassenger( toMutate.getPassenger() );
		}
		else {
			removeJointTripAndPutLeg(
					driverPes,
					driverLegIndex - 2,
					new LegImpl( TransportMode.car ) );
		}
		removeJointTripAndPutLeg(
				passengerPes,
				passengerLegIndex - 2,
				new LegImpl( TransportMode.pt ) );
	}

	private static void removeJointTripAndPutLeg(
			final List<PlanElement> planElements,
			final int tripStartIndex,
			final Leg newLeg) {
		for (int i=0; i < 5; i++) {
			planElements.remove( tripStartIndex );
		}
		planElements.add( tripStartIndex , newLeg );
	}

	private Leg getNextPassengerLeg(
			final Iterator<PlanElement> passengerElements,
			final Possibility toMutate) {
		while (passengerElements.hasNext()) {
			PlanElement pe = passengerElements.next();

			if (pe instanceof Leg && ((Leg) pe).getMode().equals( JointActingTypes.PASSENGER )) {
				PassengerRoute r = (PassengerRoute) ((Leg) pe).getRoute();

				if (r.getDriverId().equals( toMutate.getDriver() ) &&
						r.getStartLinkId().equals( toMutate.getPassengerOd().getOriginLinkId() ) &&
						r.getEndLinkId().equals( toMutate.getPassengerOd().getDestinationLinkId() ) ) {
					// XXX: assumes passenger origin and destination are PU and DO!
					return (Leg) pe;
				}
			}
		}

		return null;
	}

	private void insert(final JointPlan plan, final Possibility toMutate) {
		// plans structures
		List<PlanElement> driverElements =
			plan.getIndividualPlans().get( toMutate.getDriver() ).getPlanElements();
		List<PlanElement> passengerElements =
			plan.getIndividualPlans().get( toMutate.getPassenger() ).getPlanElements();

		List<PlanElement> driverStructure = tripRouter.tripsToLegs( driverElements , checker );
		List<PlanElement> passengerStructure = tripRouter.tripsToLegs( passengerElements , checker);

		// trip generation
		Leg access = new LegImpl( TransportMode.car );
		Activity pu = new ActivityImpl(
				JointActingTypes.PICK_UP,
				network.getLinks().get( toMutate.getPassengerOd().getOriginLinkId() ).getCoord(),
				toMutate.getPassengerOd().getOriginLinkId());
		pu.setMaximumDuration( 0 );
		Leg shared = new LegImpl( JointActingTypes.DRIVER );
		DriverRoute driverRoute = new DriverRoute( toMutate.getPassengerOd().getOriginLinkId() , toMutate.getPassengerOd().getDestinationLinkId() );
		driverRoute.addPassenger( toMutate.getPassenger() );
		shared.setRoute( driverRoute );
		Activity dro = new ActivityImpl(
				JointActingTypes.DROP_OFF,
				network.getLinks().get( toMutate.getPassengerOd().getDestinationLinkId() ).getCoord(),
				toMutate.getPassengerOd().getDestinationLinkId() );
		dro.setMaximumDuration( 0 );
		Leg egress = new LegImpl( TransportMode.car );
		List<PlanElement> driverTrip = Arrays.asList( access , pu , shared , dro , egress );

		access = new LegImpl( TransportMode.walk );
		pu = new ActivityImpl(
				JointActingTypes.PICK_UP,
				network.getLinks().get( toMutate.getPassengerOd().getOriginLinkId() ).getCoord(),
				toMutate.getPassengerOd().getOriginLinkId() );
		pu.setMaximumDuration( 0 );
		shared = new LegImpl( JointActingTypes.PASSENGER );
		PassengerRoute passengerRoute = new PassengerRoute( toMutate.getPassengerOd().getOriginLinkId() , toMutate.getPassengerOd().getDestinationLinkId() );
		passengerRoute.setDriverId( toMutate.getDriver() );
		shared.setRoute( passengerRoute );
		dro = new ActivityImpl(
				JointActingTypes.DROP_OFF,
				network.getLinks().get( toMutate.getPassengerOd().getDestinationLinkId() ).getCoord(),
				toMutate.getPassengerOd().getDestinationLinkId() );
		dro.setMaximumDuration( 0 );
		egress = new LegImpl( TransportMode.car );
		List<PlanElement> passengerTrip = Arrays.asList( access , pu , shared , dro , egress );

		// insertion point search
		Map<Integer, Double> driverTripsIndices =
				getTripIndices(
						driverStructure,
						toMutate.getDriverOd() );
		Map<Integer, Double> passengerTripsIndices =
				getTripIndices(
						passengerStructure,
						toMutate.getPassengerOd() );

		Tuple<Integer, Integer> match = minDiff( driverTripsIndices , passengerTripsIndices );

		// insertion
		Activity driverOrigin = (Activity) driverStructure.get( match.getFirst() - 1 );
		Collection<Id> currentPassengers = getPassengers( driverElements , driverOrigin );
		driverRoute.addPassengers( currentPassengers );
		tripRouter.insertTrip(
				driverElements,
				driverOrigin,
				driverTrip,
				(Activity) driverStructure.get( match.getFirst() + 1 ));

		tripRouter.insertTrip(
				passengerElements,
				(Activity) passengerStructure.get( match.getSecond() - 1 ),
				passengerTrip,
				(Activity) passengerStructure.get( match.getSecond() + 1 ));
	}

	private Collection<Id> getPassengers(
			final List<PlanElement> driverElements,
			final Activity driverOrigin) {
		// if we are on a multi-passenger joint trip, search for the other
		// passengers.
		// It actually requires that all passenger ODs for a given driver
		// OD are equal...
		int orIndex = driverElements.indexOf( driverOrigin );
		Route r = driverElements.size() > orIndex + 3 ?
			((Leg) driverElements.get( orIndex + 3 )).getRoute() :
			null;
		return r instanceof DriverRoute ? ((DriverRoute) r).getPassengersIds() : Collections.EMPTY_SET;
	}

	private Tuple<Integer, Integer> minDiff(
			final Map<Integer, Double> driverTripsIndices,
			final Map<Integer, Double> passengerTripsIndices) {
		Tuple<Integer, Integer> out = null;
		double minDiff = Double.POSITIVE_INFINITY;

		for (Map.Entry<Integer, Double> ed : driverTripsIndices.entrySet()) {
			for (Map.Entry<Integer, Double> ep : passengerTripsIndices.entrySet()) {
				double diff = ed.getValue() - ep.getValue();
				if ( Math.abs( diff ) < minDiff ) {
					out = new Tuple<Integer, Integer>( ed.getKey() , ep.getKey() );
					minDiff = diff;
				}
			}
		}

		return out;
	}

	private Map<Integer, Double> getTripIndices(
			final List<PlanElement> elements,
			final Od od) {
		Map<Integer, Double> out = new HashMap<Integer, Double>();

		int i=0;
		double now = 0;
		Activity origin = null;
		for (PlanElement pe : elements) {
			if (pe instanceof Activity) {
				if (origin != null) {
					if ( isSameOd( od , origin , pe ) ) {
						// add the preceding leg
						out.put( i - 1 , now );
					}
				}
				origin = (Activity) pe;
			}
			i++;
			now = updateNow( pe , now );
		}

		return out;
	}

	private double updateNow(final PlanElement pe, final double now) {
		if (pe instanceof Activity) {
			Activity a = (Activity) pe;

			if (a.getEndTime() != Time.UNDEFINED_TIME) {
				return a.getEndTime();
			}
			else if (a.getMaximumDuration() != Time.UNDEFINED_TIME) {
				return now + a.getMaximumDuration();
			}
		}
		else {
			Leg l = (Leg) pe;
			if (l.getTravelTime() != Time.UNDEFINED_TIME) {
				return now + l.getTravelTime();
			}
		}

		return now;
	}

	private static boolean isSameOd(
			final Od od,
			final Activity origin,
			final PlanElement destination) {
		return origin.getLinkId().equals( od.getOriginLinkId() ) &&
			((Activity) destination).getLinkId().equals( od.getDestinationLinkId() );
	}
}

