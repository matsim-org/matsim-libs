/* *********************************************************************** *
 * project: org.matsim.*
 * PlanLinkIdentifierUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning;

import playground.thibautd.socnetsim.PlanLinkConfigGroup;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;

/**
 * @author thibautd
 */
public class PlanLinkIdentifierUtils {
	private PlanLinkIdentifierUtils() {}

	public static PlanLinkIdentifier createDefaultPlanLinkIdentifier() {
		return new CompositePlanLinkIdentifier(
				new JointTripsPlanLinkIdentifier(),
				new VehicularPlanBasedIdentifier() );
	}

	public static PlanLinkIdentifier createPlanLinkIdentifierForSocialActivities(
			final String activityType,
			final SocialNetwork socialNetwork) {
		final CompositePlanLinkIdentifier id =
			new CompositePlanLinkIdentifier(
				new JointTripsPlanLinkIdentifier(),
				new VehicularPlanBasedIdentifier(),
				new JoinableActivitiesPlanLinkIdentifier( activityType ) );

		id.addAndComponent( new SocialNetworkPlanLinkIdentifier( socialNetwork ) );
		return id;
	}

	public static PlanLinkIdentifier createConfigurablePlanLinkIdentifier(
			final PlanLinkConfigGroup conf,
			final SocialNetwork socialNetwork) {
		final CompositePlanLinkIdentifier id =
			new CompositePlanLinkIdentifier();

		id.addAndComponent( new SocialNetworkPlanLinkIdentifier( socialNetwork ) );

		if ( conf.getLinkJointTrips() ) {
			id.addOrComponent( new JointTripsPlanLinkIdentifier() );
		}

		if ( conf.getLinkVehicles() ) {
			id.addOrComponent( new VehicularPlanBasedIdentifier() );
		}

		if ( conf.getLinkJoinableActivities() ) {
			for ( String activityType : conf.getJoinableTypes() ) {
				id.addOrComponent( new JoinableActivitiesPlanLinkIdentifier( activityType ) );
			}
		}

		return id;
	}
}

