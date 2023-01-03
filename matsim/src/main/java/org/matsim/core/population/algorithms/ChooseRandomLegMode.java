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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;
import java.util.Random;

/**
 * Changes the transportation mode of all legs in a plan to a randomly chosen
 * different mode (but the same mode for all legs in that plan) given a list
 * of possible modes.
 *
 * @author mrieser
 */
public final class ChooseRandomLegMode implements PlanAlgorithm {

	private final String[] possibleModes;
	private boolean ignoreCarAvailability = true;
	private boolean allowSwitchFromListedModesOnly;
	private final Random rng;

	/**
	 * @param possibleModes modes to switch to
	 * @param rng The random number generator used to draw random numbers to select another mode.
	 * @param allowSwitchFromListedModesOnly allows a change only in between the modes listed
	 * @see TransportMode
	 * @see MatsimRandom
	 */
	public ChooseRandomLegMode(final String[] possibleModes, final Random rng, boolean allowSwitchFromListedModesOnly) {
		this.possibleModes = possibleModes.clone();
		this.allowSwitchFromListedModesOnly = allowSwitchFromListedModesOnly;
		this.rng = rng;
	}

	public void setIgnoreCarAvailability(final boolean ignoreCarAvailability) {
		this.ignoreCarAvailability = ignoreCarAvailability;
	}

	@Override
	public void run(final Plan plan) {
		List<PlanElement> tour = plan.getPlanElements();
		changeToRandomLegMode(tour, plan);
	}

	private void changeToRandomLegMode(final List<PlanElement> tour, final Plan plan) {
		if (tour.size() > 1) {
			boolean forbidCar = false;
			if (!this.ignoreCarAvailability) {
				String carAvail = PersonUtils.getCarAvail(plan.getPerson());
				if ("never".equals(carAvail)) {
					forbidCar = true;
				}
			}

			final String currentMode = getTransportMode(tour);
			if (this.allowSwitchFromListedModesOnly){
				if (!contains(this.possibleModes, currentMode)) {
					return;
				}
			}
			String newMode;

			while (true) {
				int newModeIdx = chooseModeOtherThan(currentMode);
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

			changeLegModeTo(tour, newMode);
		}
	}

	private <T> boolean contains(T[] array, T value) {
		for (T t : array) {
			if (t.equals(value)) {
				return true;
			}
		}
		return false;
	}

	private String getTransportMode(final List<PlanElement> tour) {
		return ((Leg) (tour.get(1))).getMode();
	}

	private void changeLegModeTo(final List<PlanElement> tour, final String newMode) {
		for (PlanElement pe : tour) {
			if (pe instanceof Leg) {
				Leg leg = ((Leg) pe);
				leg.setMode(newMode);
				TripStructureUtils.setRoutingMode(leg, newMode);
				Route route = leg.getRoute();
				if (route instanceof NetworkRoute) {
					((NetworkRoute) route).setVehicleId(null);
				}
			}
		}
	}

	private int chooseModeOtherThan(final String currentMode) {
		int newModeIdx = this.rng.nextInt(this.possibleModes.length - 1);
		for (int i = 0; i <= newModeIdx; i++) {
			if (this.possibleModes[i].equals(currentMode)) {
				/* if the new Mode is after the currentMode in the list of possible
				 * modes, go one further, as we have to ignore the current mode in
				 * the list of possible modes. */
				// This gives the mode after the current mode twice the weight. Not good.  kai, feb'18
				// No, it does not. It's good. We choose between 0 and possibleModes.length - 2
				// (it's length - 1, but the upper bound of nextInt() is exclusive, thus it's essentially -2)
				// This gives us exactly the number of possibilities of possibleModes with the current mode excluded.
				// Instead of just accessing this.possibleModes[newModeIdx] we loop through the possible modes
				// to figure out if currentMode is before or after the new mode. If it is before, we skip it
				// by increasing the newModeIdx. In other words: If newModeIndex < currentModeIndex, we account for
				// currentMode which we want to ignore by doing newModeIndex++.  mrieser, feb'19
				newModeIdx++;
				break;
			}
		}
		return newModeIdx;
	}

}
