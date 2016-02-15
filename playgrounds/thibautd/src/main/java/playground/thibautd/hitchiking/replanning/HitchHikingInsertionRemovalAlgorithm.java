/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingInsertionRemovalAlgorithm.java
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
package playground.thibautd.hitchiking.replanning;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.thibautd.hitchiking.HitchHikingConstants;

import java.util.Random;

/**
 * @author thibautd
 */
public class HitchHikingInsertionRemovalAlgorithm implements PlanAlgorithm {
	private final static double PROB_REMOVAL = 0.5;
	private final Random random;
	private final HitchHikingInsertionAlgorithm insertionAlgorithm;
	private final HitchHikingRemovalAlgorithm removalAlgorithm;

	public HitchHikingInsertionRemovalAlgorithm(final Random random) {
		this.random = random;
		insertionAlgorithm = new HitchHikingInsertionAlgorithm( random );
		removalAlgorithm = new HitchHikingRemovalAlgorithm( random );
	}

	@Override
	public void run(final Plan plan) {
		boolean hasNonHhTrips = false;

		for (PlanElement pe : plan.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			Leg l = (Leg) pe;
			if ( HitchHikingConstants.DRIVER_MODE.equals( l.getMode() ) ||
					HitchHikingConstants.PASSENGER_MODE.equals( l.getMode() ) ) {
				// there is one HH trip
				if ( random.nextDouble() < PROB_REMOVAL ) {
					// redraw for each trip, so that the probability of removing a trip
					// increases with the number of trips.
					removalAlgorithm.run( plan );
					return;
				}
			}
			else {
				hasNonHhTrips = true;
			}
		}

		if (hasNonHhTrips) {
			insertionAlgorithm.run( plan );
		}
		else {
			// there are only HH trips (or no trip, which make no sense and will crash)
			removalAlgorithm.run( plan );
		}
	}
}

