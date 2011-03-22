/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeLegMode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.population.algorithms;

import java.util.Random;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;

/**
 * Changes the transportation mode of each legs in a plan to a randomly chosen
 * mode, given a list of possible modes. Each leg can have another mode assigned,
 * and it may be possible that the mode is not changed at all (i.e., the same mode
 * was randomly chosen again).
 *
 * <b>Warning:</b> Using this class in a replanning strategy may result in many more
 * iterations being needed until a useful state can be reached.
 *
 * @author mrieser
 */
public class ChooseRandomSingleLegMode implements PlanAlgorithm {

	private final String[] possibleModes;
	private boolean ignoreCarAvailability = true;

	private final Random rng;

	/**
	 * @param possibleModes
	 * @param rng The random number generator used to draw random numbers to select another mode.
	 * @see TransportMode
	 * @see MatsimRandom
	 */
	public ChooseRandomSingleLegMode(final String[] possibleModes, final Random rng) {
		this.possibleModes = possibleModes.clone();
		this.rng = rng;
	}

	public void setIgnoreCarAvailability(final boolean ignoreCarAvailability) {
		this.ignoreCarAvailability = ignoreCarAvailability;
	}

	@Override
	public void run(final Plan plan) {
		boolean forbidCar = false;
		if (!this.ignoreCarAvailability) {
			String carAvail = ((PersonImpl) plan.getPerson()).getCarAvail();
			if ("never".equals(carAvail)) {
				forbidCar = true;
			}
		}

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				((Leg) pe).setMode(getRandomLegMode(forbidCar));
			}
		}
	}

	private String getRandomLegMode(final boolean forbidCar) {
		String newMode;
		while (true) {
			newMode = this.possibleModes[this.rng.nextInt(this.possibleModes.length)];
			if (!(forbidCar && TransportMode.car.equals(newMode))) {
				return newMode;
			} else if (this.possibleModes.length == 1) {
				return newMode; // there is no other mode available
			}
		}
	}

}
