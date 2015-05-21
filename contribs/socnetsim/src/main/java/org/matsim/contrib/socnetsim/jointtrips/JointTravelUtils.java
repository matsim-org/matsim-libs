/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanUtils.java
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
package org.matsim.contrib.socnetsim.jointtrips;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;
import org.matsim.contrib.socnetsim.utils.Couple;

import java.util.*;

/**
 * @author thibautd
 */
public class JointTravelUtils {
	private JointTravelUtils() {}

	public static JointTravelStructure analyseJointTravel(final JointPlan plan) {
		// this is a two-passes algorithm:
		// 1- parse the plans, and identify who drives who from where to where
		// 2- parse the plans to identify the corresponding passenger legs.
		// This approach is the best I could find, as it may take several driver
		// legs to drive one passenger to destination, e.g.:
		//                    p={1,2}       p={1}
		// driver ...-----|############|#############|------...
		// pass 1 ...-----|############|--------------------...
		// pass 2 ...-----|##########################|------...
		// (TD, sept. 2012)
		final List<DriverTrip> driverTrips = parseDriverTrips( plan );
		final List<JointTrip> jointTrips = reconstructJointTrips( driverTrips , plan );

		return new JointTravelStructure( jointTrips );
	}

	private static List<JointTrip> reconstructJointTrips(
			final List<DriverTrip> driverTrips,
			final JointPlan plan) {
		final List<JointTrip> jointTrips = new ArrayList<JointTrip>();
		final PlanElementIterators iterators = new PlanElementIterators( plan );

		for ( final DriverTrip driverTrip : driverTrips ) {
			for (final Map.Entry<Id, Id> e : driverTrip.passengerOrigins.entrySet()) {
				final Id passengerId = e.getKey();
				final Id originId = e.getValue();
				final Id destinationId = driverTrip.passengerDestinations.get( passengerId );

				jointTrips.add(
						getNextPassengerTrip(
							iterators,
							driverTrip,
							passengerId,
							originId,
							destinationId) );
			}
		}

		return jointTrips;
	}

	private static JointTrip getNextPassengerTrip(
			final PlanElementIterators iterators,
			final DriverTrip driverTrip,
			final Id passengerId,
			final Id originId,
			final Id destinationId) {
		final Iterator<PlanElement> it =
			iterators.getIterator(
					driverTrip.driverId,
					passengerId,
					originId,
					destinationId);
		while ( it.hasNext() ) {
			final PlanElement pe = it.next();
			if (pe instanceof Leg &&
					((Leg) pe).getMode().equals( JointActingTypes.PASSENGER )) {
				final PassengerRoute route = (PassengerRoute) ((Leg) pe).getRoute();
				if ( route.getDriverId().equals( driverTrip.driverId ) &&
						route.getStartLinkId().equals( originId ) &&
						route.getEndLinkId().equals( destinationId )) {
					return new JointTrip(
								driverTrip.driverId,
								extractDriverSubTrip(
									originId,
									destinationId,
									driverTrip.driverTrip ),
								passengerId,
								(Leg) pe );
				}
			}
		}
		throw new RuntimeException( "no valid trip found for passenger "+passengerId+" in driver trip "+driverTrip );
	}

	private static List<Leg> extractDriverSubTrip(
			final Id originId,
			final Id destinationId,
			final List<Leg> driverTrip) {
		final ArrayList<Leg> subTrip = new ArrayList<Leg>();
		boolean inSubTrip = false;

		for ( final Leg l : driverTrip ) {
			if (l.getRoute().getStartLinkId().equals( originId )) inSubTrip = true;
			if (inSubTrip) subTrip.add( l );
			if (l.getRoute().getEndLinkId().equals( destinationId )) break;
		}

		return subTrip;
	}

	/**
	 * Parses the plans and get information about driver trips, ie who drives
	 * whom from where to where.
	 * Package protected to be callable from tests
	 */
	final static List<DriverTrip> parseDriverTrips( final JointPlan plan ) {
		final List<DriverTrip> driverTrips = new ArrayList<DriverTrip>();

		for ( final Plan indivPlan : plan.getIndividualPlans().values() ) {
			final Id driverId = indivPlan.getPerson().getId();
			final List<Id> currentPassengers = new ArrayList<Id>();

			// store the driver trips we are in, if any
			DriverTrip currentDriverTrip = null;
			for ( final PlanElement pe : indivPlan.getPlanElements() ) {
				if ( pe instanceof Leg &&
						JointActingTypes.DRIVER.equals( ((Leg) pe).getMode() ) ) {
					if ( currentDriverTrip == null ) {
						currentDriverTrip = new DriverTrip( driverId );
						driverTrips.add( currentDriverTrip );
						currentPassengers.clear();
					}
					currentDriverTrip.driverTrip.add( (Leg) pe );

					final DriverRoute dRoute = (DriverRoute) ((Leg) pe).getRoute();
					final Id origin = dRoute.getStartLinkId();
					final Id destination = dRoute.getEndLinkId();
					final Collection<Id<Person>> passengerIds = dRoute.getPassengersIds();

					for ( final Id passengerId : passengerIds ) {
						if ( !currentPassengers.contains( passengerId ) ) {
							currentDriverTrip.passengerOrigins.put(
									passengerId,
									origin );
							currentPassengers.add( passengerId );
						}
					}

					final Iterator<Id> currPassengersIter = currentPassengers.iterator();
					while ( currPassengersIter.hasNext() ) {
						final Id p = currPassengersIter.next();
						if ( !passengerIds.contains( p ) ) {
							// passenger is arrived
							currPassengersIter.remove();
						}
						else {
							// update destination
							currentDriverTrip.passengerDestinations.put(
									p,
									destination );
						}
					}
				}
				else if ( pe instanceof Leg ||
						!JointActingTypes.JOINT_STAGE_ACTS.isStageActivity(
							((Activity) pe).getType() ) ) {
					currentDriverTrip = null;
				}
			}
		}

		return driverTrips;
	}

	// package protected for tests
	static final class DriverTrip {
		final Id driverId;
		final List<Leg> driverTrip = new ArrayList<Leg>();
		final Map<Id, Id> passengerOrigins = new HashMap<Id, Id>();
		final Map<Id, Id> passengerDestinations = new HashMap<Id, Id>();

		public DriverTrip(final Id driverId) {
			this.driverId = driverId;
		}

		@Override
		public int hashCode() {
			int c = 0;

			c += driverId.hashCode();
			c += driverTrip.hashCode();
			c += passengerOrigins.hashCode();
			c += passengerDestinations.hashCode();

			return c;
		}

		@Override
		public boolean equals(final Object o) {
			if (o != null && o instanceof DriverTrip) {
				final DriverTrip other = (DriverTrip) o;
				return other.driverId.equals( driverId ) &&
					other.driverTrip.equals( driverTrip ) &&
					other.passengerOrigins.equals( passengerOrigins ) &&
					other.passengerDestinations.equals( passengerDestinations );
			}
			return false;
		}

		@Override
		public String toString() {
			return "[DriverTrip: driverId="+driverId
				+", driverTrip="+driverTrip
				+", passengerOrigins="+passengerOrigins
				+", passengerDestinations="+passengerDestinations+"]";
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// Data structures
	/**
	 * Represents the "joint travel structure" of the plan, that is,
	 * who travels with whom from where to where, with references
	 * to the relevant plan elements.
	 */
	public static final class JointTravelStructure {
		private final List<JointTrip> jointTrips;
		private final Map<Couple, List<JointTrip>> jointTripsPerCouple = new HashMap<Couple, List<JointTrip>>();

		public JointTravelStructure(
				final List<JointTrip> jointTrips) {
			this.jointTrips = jointTrips;

			for (final JointTrip jt : jointTrips) {
				final List<JointTrip> trips =
					MapUtils.getList(
							new Couple(
								jt.getDriverId(),
								jt.getPassengerId() ),
							jointTripsPerCouple );
				trips.add( jt );
			}
		}	

		public List<JointTrip> getJointTrips() {
			return jointTrips;
		}

		public List<JointTrip> getJointTripsForCotravelers(final Id id1, final Id id2) {
			final List<JointTrip> trips =
				jointTripsPerCouple.get(
						new Couple(
							id1,
							id2 ) );

			return trips == null ?
				Collections.<JointTrip>emptyList() :
				Collections.unmodifiableList( trips );
		}

		@Override
		public int hashCode() {
			return getJointTrips().hashCode();
		}

		@Override
		public boolean equals(final Object o) {
			if (o != null && o instanceof JointTravelStructure) {
				final JointTravelStructure other = (JointTravelStructure) o;
				return other.getJointTrips().size() == getJointTrips().size() &&
					other.getJointTrips().containsAll( getJointTrips() );
			}
			return false;
		}

		@Override
		public String toString() {
			return "[JointTravelStructure: "+getJointTrips()+"]";
		}
	}

	/**
	 * Gathers information releted to one joint trip, that is,
	 * one (and <b>only one</b>) passenger being driven.
	 * Note that driver legs may pertain to several joint trips,
	 * if the driver drives several passengers at the same time.
	 */
	public static final class JointTrip {
		private final Id driverId;
		private final Id passengerId;
		private final List<Leg> driverLegs;
		private final Leg passengerLeg;

		public JointTrip(
				final Id driverId,
				final List<Leg> driverLegs,
				final Id passengerId,
				final Leg passengerLeg) {
			this.driverId = driverId;
			this.passengerId = passengerId;
			this.driverLegs = Collections.unmodifiableList( driverLegs );
			this.passengerLeg = passengerLeg;
		}

		public List<Leg> getDriverLegs() {
			return driverLegs;
		}

		public Id getDriverId() {
			return driverId;
		}

		public Id getPassengerId() {
			return passengerId;
		}

		public Leg getPassengerLeg() {
			return passengerLeg;
		}

		@Override
		public int hashCode() {
			int c = 0;

			c += getDriverId().hashCode();
			c += getPassengerId().hashCode();
			c += getDriverLegs().hashCode();
			c += getPassengerLeg().hashCode(); 

			return c;
		}

		@Override
		public boolean equals(final Object o) {
			if (o != null && o instanceof JointTrip) {
				final JointTrip other = (JointTrip) o;
				return other.getDriverId().equals( getDriverId() ) &&
					other.getPassengerId().equals( getPassengerId() ) &&
					other.getDriverLegs().equals( getDriverLegs() ) &&
					other.getPassengerLeg().equals( getPassengerLeg() );
			}
			return false;
		}

		@Override
		public String toString() {
			return "[JointTrip: driver="+getDriverId()
				+", driverTrip="+getDriverLegs()
				+", passenger="+getPassengerId()
				+", passengerLeg="+getPassengerLeg()+"]";
		}
	}
}

class PlanElementIterators {
	private final JointPlan jointPlan;
	private final Map<TripCharacteristics, Iterator<PlanElement>> iterators = new HashMap<TripCharacteristics, Iterator<PlanElement>>();

	public PlanElementIterators(final JointPlan jointPlan) {
		this.jointPlan = jointPlan;
	}

	public Iterator<PlanElement> getIterator(
			final Id driver,
			final Id passenger,
			final Id origin,
			final Id destination) {
		final TripCharacteristics key = new TripCharacteristics( driver, passenger , origin , destination );
		Iterator<PlanElement> it = iterators.get( key );

		if ( it == null ) {
			final Plan plan = jointPlan.getIndividualPlan( passenger );
			if ( plan == null ) throw new RuntimeException( "no plan for passenger "+passenger+" in joint plan "+jointPlan );
			it = plan.getPlanElements().iterator();
			iterators.put( key , it );
		}

		return it;
	}
}

class TripCharacteristics {
	private final Id driver, passenger, origin, destination;

	public TripCharacteristics(
			final Id driver,
			final Id passenger,
			final Id origin,
			final Id destination) {
		this.driver = driver;
		this.passenger = passenger;
		this.origin = origin;
		this.destination = destination;
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof TripCharacteristics &&
			((TripCharacteristics) o).driver.equals( driver ) &&
			((TripCharacteristics) o).passenger.equals( passenger ) &&
			((TripCharacteristics) o).origin.equals( origin ) &&
			((TripCharacteristics) o).destination.equals( destination );
	}

	@Override
	public int hashCode() {
		return driver.hashCode() + passenger.hashCode() + origin.hashCode() + destination.hashCode();
	}
}
