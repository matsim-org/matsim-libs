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

package org.matsim.core.population.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

/**
 * Changes the transportation mode of one leg in a plan to a randomly chosen
 * mode, given a list of possible modes. Insures that the newly chosen mode
 * is different from the existing mode (if possible).
 *
 * @author mrieser
 */
public final class ChooseRandomSingleLegMode implements PlanAlgorithm {

	private final String[] possibleModes;
	private boolean ignoreCarAvailability = true;
	private boolean allowSwitchFromListedModesOnly;
	private final Random rng;
	private final List<String> possibleFromModes = new ArrayList<>();


	/**
	 * @param possibleModes possible transportmodes to chose from
	 * @param rng The random number generator used to draw random numbers to select another mode.
	 * @see TransportMode
	 * @see MatsimRandom
	 */
	public ChooseRandomSingleLegMode(final String[] possibleModes, final Random rng, boolean allowSwitchFromListedModesOnly) {
		this.possibleModes = possibleModes.clone();
		this.allowSwitchFromListedModesOnly=allowSwitchFromListedModesOnly;
		if (allowSwitchFromListedModesOnly){
			this.possibleFromModes.addAll(Arrays.asList(possibleModes));
			}
		this.rng = rng;
	}

	public void setIgnoreCarAvailability(final boolean ignoreCarAvailability) {
		this.ignoreCarAvailability = ignoreCarAvailability;
	}

	@Override
	public void run(final Plan plan) {
		boolean forbidCar = false;
		if (!this.ignoreCarAvailability) {
			String carAvail = PersonUtils.getCarAvail(plan.getPerson());
			if ("never".equals(carAvail)) {
				forbidCar = true;
			}
		}

		ArrayList<Leg> legs = new ArrayList<>();
		int cnt = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				if (allowSwitchFromListedModesOnly){
					if (this.possibleFromModes.contains(((Leg) pe).getMode())) {
						legs.add((Leg) pe);
						cnt++;
					}
				}
				else {
				legs.add((Leg) pe);
				cnt++;
				}
			}
		}
		if (cnt == 0) {
			return;
		}
		int rndIdx = this.rng.nextInt(cnt);
		setRandomLegMode(legs.get(rndIdx), forbidCar);
	}

	private void setRandomLegMode(final Leg leg, final boolean forbidCar) {
		String newMode = chooseModeOtherThan(leg.getMode(), forbidCar);
		leg.setMode(newMode);
		TripStructureUtils.setRoutingMode(leg, newMode);
		Route route = leg.getRoute() ;
		if ( route != null && route instanceof NetworkRoute) {
			((NetworkRoute)route).setVehicleId(null);
		}
	}

	private String chooseModeOtherThan(final String currentMode, final boolean forbidCar) {
		String newMode;
		while (true) {
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
			newMode = this.possibleModes[newModeIdx];
			if (!(forbidCar && TransportMode.car.equals(newMode))) {
				break;
			} else {
				if (this.possibleModes.length == 2) {
					newMode = currentMode; // there is no other mode available
					break;
				}
			}
		}
		return newMode;
	}

}
