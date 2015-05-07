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

import org.matsim.api.core.v01.Scenario;

import playground.thibautd.socnetsim.PlanLinkConfigGroup;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author thibautd
 */
public class PlanLinkIdentifierUtils {
	private PlanLinkIdentifierUtils() {}

	public static class LinkIdentifierProvider implements Provider<PlanLinkIdentifier> {
		private final Scenario sc;

		@Inject
		public LinkIdentifierProvider(final Scenario sc) {
			this.sc = sc;
		}

		@Override
		public PlanLinkIdentifier get() {
			return createPlanLinkIdentifier( sc );
		}
	}

	public static class WeakLinkIdentifierProvider implements Provider<PlanLinkIdentifier> {
		private final Scenario sc;

		@Inject
		public WeakLinkIdentifierProvider(final Scenario sc) {
			this.sc = sc;
		}

		@Override
		public PlanLinkIdentifier get() {
			return createWeakPlanLinkIdentifier( sc );
		}
	}

	public static PlanLinkIdentifier createPlanLinkIdentifier(
			final Scenario scenario ) {
		final PlanLinkConfigGroup configGroup = (PlanLinkConfigGroup)
			scenario.getConfig().getModule( PlanLinkConfigGroup.GROUP_NAME );
		return PlanLinkIdentifierUtils.createPlanLinkIdentifier(
					configGroup,
					(SocialNetwork) scenario.getScenarioElement(
						SocialNetwork.ELEMENT_NAME ) );
	}

	public static PlanLinkIdentifier createPlanLinkIdentifier(
			final PlanLinkConfigGroup conf,
			final SocialNetwork socialNetwork) {
		final CompositePlanLinkIdentifier id =
			new CompositePlanLinkIdentifier();

		id.addAndComponent( new SocialNetworkPlanLinkIdentifier( socialNetwork ) );

		if ( conf.getLinkJointTrips().isWeak() ) {
			id.addOrComponent( new JointTripsPlanLinkIdentifier() );
		}

		if ( conf.getLinkVehicles().isWeak() ) {
			id.addOrComponent( new VehicularPlanBasedIdentifier() );
		}

		if ( conf.getLinkJoinableActivities().isWeak() ) {
			for ( String activityType : conf.getJoinableTypes() ) {
				id.addOrComponent( new JoinableActivitiesPlanLinkIdentifier( activityType ) );
			}
		}

		return id;
	}
	
	public static PlanLinkIdentifier createWeakPlanLinkIdentifier(
			final Scenario scenario ) {
		final PlanLinkConfigGroup configGroup = (PlanLinkConfigGroup)
			scenario.getConfig().getModule( PlanLinkConfigGroup.GROUP_NAME );
		return PlanLinkIdentifierUtils.createWeakPlanLinkIdentifier(
					configGroup,
					(SocialNetwork) scenario.getScenarioElement(
						SocialNetwork.ELEMENT_NAME ) );
	}
	
	/**
	 * The "weak" identifier only considers strong links!
	 * (The name should be changed)
	 * @param conf
	 * @param socialNetwork
	 * @return
	 */
	public static PlanLinkIdentifier createWeakPlanLinkIdentifier(
			final PlanLinkConfigGroup conf,
			final SocialNetwork socialNetwork) {
		final CompositePlanLinkIdentifier id =
			new CompositePlanLinkIdentifier();

		id.addAndComponent( new SocialNetworkPlanLinkIdentifier( socialNetwork ) );

		if ( conf.getLinkJointTrips().isStrong() ) {
			id.addOrComponent( new JointTripsPlanLinkIdentifier() );
		}

		if ( conf.getLinkVehicles().isStrong() ) {
			id.addOrComponent( new VehicularPlanBasedIdentifier() );
		}

		if ( conf.getLinkJoinableActivities().isStrong() ) {
			for ( String activityType : conf.getJoinableTypes() ) {
				id.addOrComponent( new JoinableActivitiesPlanLinkIdentifier( activityType ) );
			}
		}

		return id;
	}
}

