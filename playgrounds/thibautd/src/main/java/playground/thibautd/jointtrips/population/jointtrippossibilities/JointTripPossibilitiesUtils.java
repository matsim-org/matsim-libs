/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripPossibilitiesUtils.java
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
package playground.thibautd.jointtrips.population.jointtrippossibilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.collections.Tuple;

import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.jointtrips.population.JointActivity;
import playground.thibautd.jointtrips.population.JointLeg;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.utils.RemoveJointTrips;

/**
 * provides static helper methods.
 *
 * @author thibautd
 */
public class JointTripPossibilitiesUtils {

	// do not instanciate
	private JointTripPossibilitiesUtils() {}

	// /////////////////////////////////////////////////////////////////////////
	// creation
	// /////////////////////////////////////////////////////////////////////////
	public static JointTripPossibilities extractJointTripPossibilities(
			final JointPlan plan) {
		return extractJointTripPossibilities( plan , new JointTripPossibilitiesFactoryImpl() );
	}

	public static JointTripPossibilities extractJointTripPossibilities(
			final JointPlan plan,
			final JointTripPossibilitiesFactory factory) {
		Map<Id, ParticipationBuilder> drivers = new HashMap<Id, ParticipationBuilder>();
		Map<ParticipationBuilder, Id> passengers = new HashMap<ParticipationBuilder, Id>();

		Id lastNonPuDoAct = null;
		boolean lastActWasPuDo = false;
		ParticipationBuilder currentParticipation = new ParticipationBuilder();

		// extract participations
		for ( PlanElement pe : plan.getPlanElements() ) {
			if (pe instanceof JointActivity) {
				JointActivity act = (JointActivity) pe;

				String type = act.getType();

				if (!type.equals( JointActingTypes.PICK_UP ) && !type.equals( JointActingTypes.DROP_OFF )) {
					lastNonPuDoAct = act.getId();
					if (lastActWasPuDo) {
						// end of JT
						currentParticipation.setDestinationActivityId( act.getId() );
						currentParticipation = new ParticipationBuilder();
						lastActWasPuDo = false;
					}
				}
				else {
					lastActWasPuDo = true;
				}

			}
			else if (pe instanceof JointLeg) {
				JointLeg leg = (JointLeg) pe;

				if (leg.getJoint()) {
					currentParticipation.setAgentId( leg.getPerson().getId() );
					currentParticipation.setOriginActivityId( lastNonPuDoAct );

					if (leg.getIsDriver()) {
						drivers.put( leg.getId() , currentParticipation );
					}
					else {
						passengers.put( currentParticipation , getDriverId( leg ) );
					}
				}
			}
			else {
				throw new RuntimeException( "Unexpected plan element type "+pe.getClass().getName());
			}
		}

		// group drivers and passengers
		List<JointTripPossibility> possibilities = new ArrayList<JointTripPossibility>();

		for (Map.Entry<ParticipationBuilder, Id> entry : passengers.entrySet()) {
			ParticipationBuilder driver = drivers.get( entry.getValue() );
			possibilities.add( factory.createJointTripPossibility(
						driver.getParticipation( factory ),
						entry.getKey().getParticipation( factory ) ) );
		}

		return factory.createJointTripPossibilities( possibilities );
	}

	private static Id getDriverId(final JointLeg leg) {
		for ( JointLeg other : leg.getLinkedElements().values() ) {
			if (other.getIsDriver()) {
				return other.getId();
			}
		}
		throw new RuntimeException( "could not resolve driver leg" );
	}


	// /////////////////////////////////////////////////////////////////////////
	// analysis
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Analyses the participation in the joint trips defined in a plan.
	 *
	 * @param plan the plan to analyse
	 * @return a map linking the {@link JointTripPossibility} returned by the plan
	 * to a Boolean indicating whether the corresponding trip is performed or not
	 *
	 * @throws IllegalArgumentException if the method {@link JointPlan#getJointTripPossibilities()}
	 * of the argument plan returns null.
	 */
	public static Map<JointTripPossibility, Boolean> getPerformedJointTrips(
			final JointPlan plan) {
		JointTripPossibilities possibilities = plan.getJointTripPossibilities();

		if (possibilities == null) {
			throw new IllegalArgumentException( "cannot analyse JointTripPossibilities participation on a plan without JointTripPossibilities" );
		}

		Map<JointTripPossibility, Boolean> participations = new HashMap<JointTripPossibility, Boolean>();

		List< Tuple<Id , Id> > passengerODs = new ArrayList< Tuple<Id , Id> >();

		// identify passengers OD
		Id lastNonPuDoAct = null;
		boolean inPassengerTrip = false;
		for ( PlanElement pe : plan.getPlanElements() ) {
			if (pe instanceof JointActivity) {
				JointActivity act = (JointActivity) pe;

				String type = act.getType();

				if (!type.equals( JointActingTypes.PICK_UP ) && !type.equals( JointActingTypes.DROP_OFF )) {
					if (inPassengerTrip) {
						// end of JT
						inPassengerTrip = false;
						passengerODs.add(
								new Tuple<Id, Id>(
									lastNonPuDoAct,
									act.getId() ) );
					}
					lastNonPuDoAct = act.getId();
				}

			}
			else if (pe instanceof JointLeg) {
				JointLeg leg = (JointLeg) pe;

				if (leg.getJoint() && !leg.getIsDriver()) {
					inPassengerTrip = true;
				}
			}
			else {
				throw new RuntimeException( "Unexpected plan element type "+pe.getClass().getName());
			}
		}

		// create participation info
		for ( JointTripPossibility possibility : possibilities.getJointTripPossibilities() ) {
			JointTripParticipation passenger = possibility.getPassenger();
			Tuple<Id, Id> passengerOD =
				new Tuple<Id, Id>(
						passenger.getOriginActivityId(),
						passenger.getDestinationActivityId() );

			if (passengerODs.remove( passengerOD )) {
				participations.put( possibility , true );
			}
			else {
				participations.put( possibility , false );
			}
		}

		return participations;
	}

	// /////////////////////////////////////////////////////////////////////////
	// inclusion
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Transforms a plan so that its joint trips correspond to the passed boolean
	 * information.
	 * The behaviour of this method on joint trips with several passengers is ill-specified
	 * (only one PU and one DO will be defined, on the location of the origin and destination
	 * of the first passenger examined).
	 * More complex joint trips should be handled by a router-like class.
	 * No routing is done at all, and all non-car or passenger legs are set to mode
	 * walk. Mode consistency is not enforced in any way.
	 */
	public static void includeJointTrips(
			final Map<JointTripPossibility, Boolean> participation,
			final JointPlan plan) {
		// maps origin driver act id to the list of corresponding JT possibilities
		// for which the participation value is "true"
		Map<Id, List<JointTripPossibility>> driverTrips = new HashMap<Id, List<JointTripPossibility>>();

		// fill the "driver trips" map.
		for (Map.Entry<JointTripPossibility, Boolean> entry : participation.entrySet()) {
			if (entry.getValue()) {
				JointTripPossibility possibility = entry.getKey();
				Id driverOrigin = possibility.getDriver().getOriginActivityId();
				List<JointTripPossibility> passengerTrips = driverTrips.get( driverOrigin );
				
				if (passengerTrips == null) {
					passengerTrips = new ArrayList<JointTripPossibility>();
					driverTrips.put( driverOrigin , passengerTrips );
				}

				passengerTrips.add( possibility );
			}
		}

		Map<Id, Plan> individualPlans = plan.getIndividualPlans();
		Map<Id, List<PlanElement>> planElements = new HashMap<Id, List<PlanElement>>();

		for (Map.Entry<Id, Plan> individualPlan : individualPlans.entrySet()) {
			RemoveJointTrips.removeJointTrips( individualPlan.getValue() );
			planElements.put(
					individualPlan.getKey(),
					individualPlan.getValue().getPlanElements());
		}

		Map<Id, ? extends Person> persons = plan.getClique().getMembers();
		for (Map.Entry<Id, List<JointTripPossibility>> trip : driverTrips.entrySet()) {
			JointTripPossibility firstTripPossibility = trip.getValue().get( 0 );
			PuDoFactory factory = new PuDoFactory( firstTripPossibility , plan );

			List<JointLeg> linkedLegs = new ArrayList<JointLeg>();

			// driver trip
			List<PlanElement> individualPlanElements =
				planElements.get( firstTripPossibility.getDriver().getAgentId() );
			int index = getIndex( trip.getKey() , individualPlanElements ) + 1;
			Person agent = persons.get( firstTripPossibility.getDriver().getAgentId() );

			individualPlanElements.add( index , factory.createDropOff( agent ) );
			JointLeg leg = new JointLeg( TransportMode.car , agent );
			linkedLegs.add( leg );
			individualPlanElements.add( index , leg );
			individualPlanElements.add( index , factory.createPickUp( agent ) );
			leg = new JointLeg( TransportMode.car , agent );
			individualPlanElements.add( index , leg );

			// passengers
			for (JointTripPossibility possibility : trip.getValue()) {
				individualPlanElements =
					planElements.get( possibility.getPassenger().getAgentId() );
				index = getIndex( possibility.getPassenger().getOriginActivityId() , individualPlanElements ) + 1;
				agent = persons.get( possibility.getPassenger().getAgentId() );

				individualPlanElements.add( index , factory.createDropOff( agent ) );
				leg = new JointLeg( JointActingTypes.PASSENGER , agent );
				linkedLegs.add( leg );
				individualPlanElements.add( index , leg );
				individualPlanElements.add( index , factory.createPickUp( agent ) );
				leg = new JointLeg( TransportMode.walk , agent );
				individualPlanElements.add( index , leg );
			}

			// linkage
			for (JointLeg linkedLeg : linkedLegs) {
				Id legId = linkedLeg.getId();
				for (JointLeg otherLeg : linkedLegs) {
					if (!legId.equals( otherLeg.getId() )) {
						linkedLeg.addLinkedElementById( otherLeg.getId() );
					}
				}
			}
		}

		JointPlan newPlan =
			new JointPlan(
					plan.getClique(),
					individualPlans,
					false,
					false,
					plan.getScoresAggregatorFactory());
		plan.resetFromPlan( newPlan );
	}

	/**
	 * includes all joint trip possibilities
	 */
	public static void includeAllJointTrips(
			final JointPlan plan) {
		Map<JointTripPossibility, Boolean> participation = getPerformedJointTrips( plan );

		for (Map.Entry<JointTripPossibility, Boolean> entry : participation.entrySet()) {
			entry.setValue( true );
		}

		includeJointTrips( participation , plan );
	}

	private static int getIndex(
			final Id actId,
			final List<PlanElement> pes) {
		int count = 0;
		for (PlanElement pe : pes) {
			if (pe instanceof JointActivity && ((JointActivity) pe).getId().equals( actId )) {
				return count;
			}
			count++;
		}
		throw new RuntimeException( "could not find required activity" );
	}
}

class PuDoFactory {
	//one instance per joint trip
	private static long count = Long.MIN_VALUE;
	private final String puName;
	private final Id puLink, doLink;
	private final Coord puCoord, doCoord;

	public PuDoFactory(
			final JointTripPossibility possibility,
			final JointPlan plan) {
		puName = JointActingTypes.PICK_UP_BEGIN + JointActingTypes.PICK_UP_SPLIT_EXPR + getCount();
		Activity origin = plan.getActById( (Id) possibility.getPassenger().getOriginActivityId() );
		Activity destination  = plan.getActById( (Id) possibility.getPassenger().getDestinationActivityId() );

		puLink = origin.getLinkId();
		doLink = destination.getLinkId();

		puCoord = origin.getCoord();
		doCoord = destination.getCoord();
	}

	private static synchronized long getCount() {
		return count++;
	}

	public JointActivity createPickUp(final Person person) {
		return new JointActivity(
				puName,
				puCoord,
				puLink,
				person);
	}

	public JointActivity createDropOff(final Person person) {
		return new JointActivity(
				JointActingTypes.DROP_OFF,
				doCoord,
				doLink,
				person);
	}
}

class ParticipationBuilder {
	private Id agentId = null;
	private Id originId = null;
	private Id destinationId = null;

	private JointTripParticipation participation = null;

	public void setAgentId(final Id id) {
		agentId = id;
	}

	public void setOriginActivityId(final Id id) {
		originId = id;
	}

	public void setDestinationActivityId(final Id id) {
		destinationId = id;
	}

	public JointTripParticipation getParticipation(
			final JointTripPossibilitiesFactory factory) {
		if (participation == null) {
			participation = factory.createJointTripParticipation( agentId , originId , destinationId );
		}
		return participation;
	}
}
