/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingInsertionAlgorithm.java
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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.thibautd.hitchiking.HitchHikingConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Parses a plan to get pt and car trips, and chooses one at random to transform
 * it to hitch hiking.
 *
 * The plan must no have multi-leg trips!
 *
 * @author thibautd
 */
public class HitchHikingInsertionAlgorithm implements PlanAlgorithm {
	private final Random random;

	public HitchHikingInsertionAlgorithm(final Random random) {
		this.random = random;
	}

	@Override
	public void run(final Plan plan) {
		List<Leg> choiceSet = new ArrayList<Leg>();
		for (PlanElement pe : plan.getPlanElements()) {
			if ( pe instanceof Leg &&
					( ((Leg) pe).getMode().equals( TransportMode.car ) ||
					  ((Leg) pe).getMode().equals( TransportMode.pt ) ) ) {
				choiceSet.add( (Leg) pe );
			}
		}
		if (choiceSet.size() == 0) return;

		Leg choice = choiceSet.get( random.nextInt( choiceSet.size() ) );
		choice.setRoute( null );
		
		if (choice.getMode().equals( TransportMode.car )) {
			choice.setMode( HitchHikingConstants.DRIVER_MODE );
		}
		else if (choice.getMode().equals( TransportMode.pt )) {
			choice.setMode( HitchHikingConstants.PASSENGER_MODE );
		}
		else {
			throw new RuntimeException( choice.getMode() );
		}
	}
}

