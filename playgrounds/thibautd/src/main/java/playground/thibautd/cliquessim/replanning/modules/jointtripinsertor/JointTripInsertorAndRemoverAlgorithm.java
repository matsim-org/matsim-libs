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
package playground.thibautd.cliquessim.replanning.modules.jointtripinsertor;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.cliquessim.config.JointTripInsertorConfigGroup;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;

/**
 * @author thibautd
 */
public class JointTripInsertorAndRemoverAlgorithm implements PlanAlgorithm {
	private final TripRouter tripRouter;
	private final Random random;
	private final PlanAlgorithm insertor, remover;

	public JointTripInsertorAndRemoverAlgorithm(
			final Config config,
			final TripRouter tripRouter,
			final Random random) {
		this.tripRouter = tripRouter;
		this.random = random;
		this.insertor = new JointTripInsertorAlgorithm(
				random,
				(JointTripInsertorConfigGroup) config.getModule( JointTripInsertorConfigGroup.GROUP_NAME ),
				tripRouter);
		this.remover = new JointTripRemoverAlgorithm( random );
	}

	@Override
	public void run(final Plan plan) {
		if ( random.nextDouble() < getProbRemoval( plan )) {
			remover.run( plan );
		}
		else {
			insertor.run( plan );
		}
	}

	private double getProbRemoval(final Plan plan) {
		int countPassengers = 0;
		int countEgoists = 0;
		for (Plan indivPlan : ((JointPlan) plan).getIndividualPlans().values()) {
			List<PlanElement> struct = tripRouter.tripsToLegs( indivPlan );
			// parse trips, and count "egoists" (non-driver non-passenger) and
			// passengers. Some care is needed: joint trips are not identified as
			// trips by the router!
			boolean first = true;
			boolean isPassenger = false;
			for (PlanElement pe : struct) {
				if (first) {
					first = false;
				}
				else if (pe instanceof Activity) {
					if (JointActingTypes.JOINT_STAGE_ACTS.isStageActivity( ((Activity) pe).getType() )) {
						// skip
					}
					else if (isPassenger) {
						countPassengers++;
						isPassenger = false;
					}
					else {
						countEgoists++;
					}
				}
				else if ( ((Leg) pe).getMode().equals( JointActingTypes.PASSENGER ) ) {
					isPassenger = true;
				}
			}
		}

		return ((double) countPassengers) / (countEgoists + countPassengers);
	}

}

