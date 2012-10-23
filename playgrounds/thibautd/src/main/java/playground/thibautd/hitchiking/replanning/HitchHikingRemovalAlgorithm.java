/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingRemovalAlgorithm.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.utils.SubtourStructure;
import playground.thibautd.utils.SubtourStructure.Subtour;

/**
 * Algorithm which removes a random hitch hiking trip and replaces it by a leg
 * of the subtour mode.
 * @author thibautd
 */
public class HitchHikingRemovalAlgorithm implements PlanAlgorithm {
	private final Random random;
	private static final String DEFAULT_REPL_MODE_DRIVER = TransportMode.car;
	private static final String DEFAULT_REPL_MODE_PASSENGER = TransportMode.pt;

	public HitchHikingRemovalAlgorithm(final Random random) {
		this.random = random;
	}

	@Override
	public void run(final Plan plan) {
		SubtourStructure structure = new SubtourStructure( plan );

		Subtour toActOn = getRandomHhSubtour( structure );

		String mode = getNonHhMode( toActOn );

		List<Leg> eligibleLegs = new ArrayList<Leg>();
		for (PlanElement pe : toActOn.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			Leg l = (Leg) pe;
			if ( HitchHikingConstants.DRIVER_MODE.equals( l.getMode() ) ||
					HitchHikingConstants.PASSENGER_MODE.equals( l.getMode() ) ) {
				eligibleLegs.add( l );
			}
		}

		Leg leg = eligibleLegs.get( random.nextInt( eligibleLegs.size() ) );
		leg.setMode( mode != null ? mode : defaultReplacementMode( leg ) );
		leg.setRoute( null );
	}

	private final String defaultReplacementMode(final Leg leg) {
		String oldMode = leg.getMode();
		if (oldMode.equals( HitchHikingConstants.DRIVER_MODE )) return DEFAULT_REPL_MODE_DRIVER;
		if (oldMode.equals( HitchHikingConstants.PASSENGER_MODE )) return DEFAULT_REPL_MODE_PASSENGER;
		throw new IllegalArgumentException( oldMode );
	}

	private static String getNonHhMode(final Subtour s) {
		if (s == null) return null;

		for (PlanElement pe : s.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			Leg l = (Leg) pe;
			if ( !HitchHikingConstants.DRIVER_MODE.equals( l.getMode() ) &&
					!HitchHikingConstants.PASSENGER_MODE.equals( l.getMode() ) ) {
				return l.getMode();
			}
		}

		return getNonHhMode( s.getParent() );
	}

	private Subtour getRandomHhSubtour(final SubtourStructure structure) {
		List<Subtour> eligibleSubtours = new ArrayList<Subtour>();

		for (Subtour s : structure.getSubtours()) {
			for ( PlanElement pe : s.getPlanElements() ) {
				if ( !(pe instanceof Leg) ) continue;
				Leg l = (Leg) pe;
				if ( HitchHikingConstants.DRIVER_MODE.equals( l.getMode() ) ||
						HitchHikingConstants.PASSENGER_MODE.equals( l.getMode() ) ) {
					// add once per leg, so that probability of selecting a subtour
					// is proportional to the number of HH trips in it.
					eligibleSubtours.add( s );
				}
			}
		}

		return eligibleSubtours.get( random.nextInt( eligibleSubtours.size() ) );
	}
}

