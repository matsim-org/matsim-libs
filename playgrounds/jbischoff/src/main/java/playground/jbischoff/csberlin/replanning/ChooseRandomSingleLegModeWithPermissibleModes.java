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

package playground.jbischoff.csberlin.replanning;

import java.util.ArrayList;
import java.util.Random;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PlanAlgorithm;

/**
 * Changes the transportation mode of one leg in a plan to a randomly chosen
 * mode, given a list of possible modes. Insures that the newly chosen mode
 * is different from the existing mode (if possible).
 *
 * @author mrieser, jbischoff
 */
public class ChooseRandomSingleLegModeWithPermissibleModes implements PlanAlgorithm {


	private final Random rng;
	private PermissibleModesCalculator modesCalculator;

	/**
	 * @param possibleModes
	 * @param rng The random number generator used to draw random numbers to select another mode.
	 * @see TransportMode
	 * @see MatsimRandom
	 */
	public ChooseRandomSingleLegModeWithPermissibleModes(PermissibleModesCalculator modesCalculator, final Random rng) {
		this.modesCalculator = modesCalculator;
		this.rng = rng;
	}

	

	@Override
	public void run(final Plan plan) {
	
		ArrayList<Leg> legs = new ArrayList<Leg>();
		int cnt = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				legs.add((Leg) pe);
				cnt++;
			}
		}
		if (cnt == 0) {
			return;
		}
		int rndIdx = this.rng.nextInt(cnt);
	
		setRandomLegMode(legs.get(rndIdx), 	(String[]) modesCalculator.getPermissibleModes(plan).toArray());
	}

	private void setRandomLegMode(final Leg leg, String[] possibleModes) {
		leg.setMode(chooseModeOtherThan(leg.getMode(), possibleModes));
	}

	private String chooseModeOtherThan(final String currentMode, String[] possibleModes) {
		String newMode;
			int newModeIdx = this.rng.nextInt(possibleModes.length - 1);
			for (int i = 0; i <= newModeIdx; i++) {
				if (possibleModes[i].equals(currentMode)) {
					/* if the new Mode is after the currentMode in the list of possible
					 * modes, go one further, as we have to ignore the current mode in
					 * the list of possible modes. */
					newModeIdx++;
					break;
				}
			}
			newMode = possibleModes[newModeIdx];
					
		return newMode;
	}

}
