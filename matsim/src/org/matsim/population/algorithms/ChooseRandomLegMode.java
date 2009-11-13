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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.LegImpl;

/**
 * Changes the transportation mode of all legs in a plan to a randomly chosen
 * different mode (but the same mode for all legs in that plan) given a list
 * of possible modes.
 *
 * @author mrieser
 */
public class ChooseRandomLegMode implements PlanAlgorithm {

	private final TransportMode[] possibleModes;
	private final Random rng;

	/**
	 * @param possibleModes
	 * @param rng The random number generator used to draw random numbers to select another mode.
	 *
	 * @see TransportMode
	 * @see MatsimRandom
	 */
	public ChooseRandomLegMode(final TransportMode[] possibleModes, final Random rng) {
		this.possibleModes = possibleModes.clone();
		this.rng = rng;
	}

	public void run(final Plan plan) {
		if (plan.getPlanElements().size() > 1) {
			final TransportMode currentMode = ((BasicLeg) (plan.getPlanElements().get(1))).getMode();
			int newModeIdx = this.rng.nextInt(this.possibleModes.length - 1);
			for (int i = 0; i <= newModeIdx; i++) {
				if (this.possibleModes[i].equals(currentMode)) {
					/* if the new Mode is after the currentMode in the list of possible
					 * modes, go one further, as we have to ignore the current mode in
					 * the list of possible modes. */
					newModeIdx++;
					break;
				}
			}
			TransportMode newMode = this.possibleModes[newModeIdx];
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof LegImpl) {
					((LegImpl) pe).setMode(newMode);
				}
			}
		}
	}

}
