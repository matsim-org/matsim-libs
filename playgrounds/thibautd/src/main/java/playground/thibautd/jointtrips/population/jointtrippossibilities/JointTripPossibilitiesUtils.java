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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.jointtrips.population.JointActivity;
import playground.thibautd.jointtrips.population.JointLeg;
import playground.thibautd.jointtrips.population.JointPlan;

/**
 * provides static helper methods.
 *
 * @author thibautd
 */
public class JointTripPossibilitiesUtils {

	// do not instanciate
	private JointTripPossibilitiesUtils() {}

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
					else {
						lastActWasPuDo = true;
					}
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
