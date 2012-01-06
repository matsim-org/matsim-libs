/* *********************************************************************** *
 * project: org.matsim.*
 * LegTravelTimeEstimatorWrapper.java
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
package playground.thibautd.planomat.basic.algorithms;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.planomat.basic.PlanomatFitnessFunctionImpl;

/**
 * Uses a LegTravelTimeEstimator (part of planomat v1) to estimate travel
 * times.
 * It is meant to be used as a postdecoding algorithm in the {@link PlanomatFitnessFunctionImpl}.
 * It can only be used on the plan it was passed at initialisation.
 * <br>
 * It is meant to be a temporary solution, as:
 * <ul>
 * <li> it assumes a strict act/leg alternance
 * <li> the interface of LegTravelTimeEstimator is clealy not optimal, and should
 * be dropped some day.
 * </ul>
 *
 * @author thibautd
 */
public class LegTravelTimeEstimatorWrapper implements PlanAlgorithm {
	private static final double MINIMAL_DURATION = 1;

	private final Plan internalPlan;
	private final LegTravelTimeEstimator estimator;

	public LegTravelTimeEstimatorWrapper(
			final Plan plan,
			final LegTravelTimeEstimator estimator) {
		this.internalPlan = plan;
		this.estimator = estimator;
	}

	@Override
	public void run(final Plan plan) {
		if (plan != internalPlan) {
			throw new IllegalArgumentException( "cannot run "+
					this.getClass().getSimpleName()+".run() on "+
					"a plan different from the one with which it was "+
					"initialised.");
		}

		List<PlanElement> planElements = plan.getPlanElements();
		Id personId = plan.getPerson().getId();
		// no nice way to do this, due to the need to provide the method with origin
		// and destination...
		for (int i=1; i < planElements.size(); i += 2) {
			Activity origin = (Activity) planElements.get( i - 1 );
			Leg leg = (Leg) planElements.get( i );
			Activity destination = (Activity) planElements.get( i + 1 );

			double tt = estimator.getLegTravelTimeEstimation(
					personId,
					origin.getEndTime(),
					origin,
					destination,
					leg,
					true);

			double startTime = origin.getEndTime() + tt;
			destination.setStartTime( startTime );

			double earlierEndTime = startTime + MINIMAL_DURATION;
			if (destination.getEndTime() < earlierEndTime) {
				destination.setEndTime( earlierEndTime );
			}
		}
	}
}

