/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripInsertorAndRemoverAlgorithm.java
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
package playground.thibautd.socnetsim.jointtrips.replanning.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

import playground.thibautd.socnetsim.framework.cliques.config.JointTripInsertorConfigGroup;
import playground.thibautd.socnetsim.jointtrips.population.JointActingTypes;
import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.SocialNetwork;
import playground.thibautd.socnetsim.framework.replanning.GenericPlanAlgorithm;

/**
 * @author thibautd
 */
public class JointTripInsertorAndRemoverAlgorithm implements GenericPlanAlgorithm<JointPlan> {
	private final TripRouter tripRouter;
	private final Random random;
	private final JointTripInsertorAlgorithm insertor;
	private final JointTripRemoverAlgorithm remover;
	private final boolean iterative;

	public JointTripInsertorAndRemoverAlgorithm(
			final Scenario scenario,
			final TripRouter tripRouter,
			final Random random,
			final boolean iterative) {
		this.tripRouter = tripRouter;
		this.random = random;
		this.insertor = new JointTripInsertorAlgorithm(
				random,
				(SocialNetwork) scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME ),
				(JointTripInsertorConfigGroup) scenario.getConfig().getModule( JointTripInsertorConfigGroup.GROUP_NAME ),
				tripRouter);
		this.remover = new JointTripRemoverAlgorithm(
				random,
				tripRouter.getStageActivityTypes(),
				tripRouter.getMainModeIdentifier());
		this.iterative = iterative;
	}

	@Override
	public void run(final JointPlan plan) {
		final List<Id<Person>> agentsToIgnore = new ArrayList< >();

		do {
			final ActedUponInformation actedUpon =
					random.nextDouble() < getProbRemoval( plan , agentsToIgnore ) ?
						remover.run( plan , agentsToIgnore ) :
						insertor.run( plan , agentsToIgnore );

			if (actedUpon == null) return;
			assert !agentsToIgnore.contains( actedUpon.getDriverId() ) : actedUpon+" <- "+agentsToIgnore;
			agentsToIgnore.add( actedUpon.getDriverId() );

			assert !agentsToIgnore.contains( actedUpon.getPassengerId() ) : actedUpon+" <- "+agentsToIgnore;
			agentsToIgnore.add( actedUpon.getPassengerId() );
		} while ( iterative );
	}

	private double getProbRemoval(
			final JointPlan plan,
			final List<Id<Person>> agentsToIgnore) {
		int countPassengers = 0;
		int countEgoists = 0;

		final CompositeStageActivityTypes stageTypes = new CompositeStageActivityTypes();
		stageTypes.addActivityTypes( tripRouter.getStageActivityTypes() );
		stageTypes.addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );

		for (Plan indivPlan : plan.getIndividualPlans().values()) {
			if ( agentsToIgnore.contains( indivPlan.getPerson().getId() ) ) continue;

			// parse trips, and count "egoists" (non-driver non-passenger) and
			// passengers. Some care is needed: joint trips are not identified as
			// trips by the router!
			for ( Trip trip : TripStructureUtils.getTrips( indivPlan , stageTypes ) ) {
				if ( tripContainsOneOfThoseModes( trip , Collections.singleton( JointActingTypes.PASSENGER ) ) ) countPassengers++;
				if ( !tripContainsOneOfThoseModes( trip , JointActingTypes.JOINT_MODES ) ) countEgoists++;
			}
		}

		return ((double) countPassengers) / (countEgoists + countPassengers);
	}

	private static boolean tripContainsOneOfThoseModes(final Trip trip, final Collection<String> modes) {
		for ( Leg leg : trip.getLegsOnly() ) {
			if ( modes.contains( leg.getMode() ) ) return true;
		}
		return false;
	}
}

