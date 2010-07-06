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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanomatConfigGroup.TripStructureAnalysisLayerOption;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Changes the transportation mode of all legs in a plan to a randomly chosen
 * different mode (but the same mode for all legs in that plan) given a list
 * of possible modes.
 *
 * @author mrieser
 */
public class ChooseRandomLegMode implements PlanAlgorithm {

	private static class Candidate {
		Integer subTourIndex;
		String newTransportMode;
	}

	private final static Collection<String> CHAIN_BASED_MODES;
	private final String[] possibleModes;
	private final Random rng;
	private boolean changeOnlyOneSubtour = false;
	private PlanAnalyzeSubtours planAnalyzeSubtours;

	static {
		CHAIN_BASED_MODES = new HashSet<String>();
		CHAIN_BASED_MODES.add(TransportMode.car);
		CHAIN_BASED_MODES.add("miv");
		CHAIN_BASED_MODES.add(TransportMode.bike);
		CHAIN_BASED_MODES.add("motorbike");
	}

	/**
	 * @param possibleModes
	 * @param rng The random number generator used to draw random numbers to select another mode.
	 * @see TransportMode
	 * @see MatsimRandom
	 */
	public ChooseRandomLegMode(final String[] possibleModes, final Random rng) {
		this.possibleModes = possibleModes.clone();
		this.rng = rng;
		this.planAnalyzeSubtours = new PlanAnalyzeSubtours();
	}

	@Override
	public void run(final Plan plan) {
		if (plan.getPlanElements().size() > 1) {
			if (changeOnlyOneSubtour) {
				planAnalyzeSubtours.run(plan);
				List<Candidate> candidates = determineChangeCandidates();
				if (!candidates.isEmpty()) {
					Candidate whatToDo = candidates.get(rng.nextInt(candidates
							.size()));
					List<PlanElement> subTour = planAnalyzeSubtours.getSubtours().get(whatToDo.subTourIndex);
					changeLegModeTo(subTour, whatToDo.newTransportMode);
				}
			} else {
				List<PlanElement> tour = plan.getPlanElements();
				changeToRandomLegMode(tour);
			}
//		} else {
			// Nothing to do - the whole plan does not contain a
			// subtour. It isn't even its own subtour because it isn't a
			// tour.
		}
	}

	private List<Candidate> determineChangeCandidates() {
		ArrayList<Candidate> candidates = new ArrayList<Candidate>();
		for (Integer subTourIndex : planAnalyzeSubtours.getSubtourIndexation()) {
			if (subTourIndex < 0) {
				continue;
			}
			List<PlanElement> subTour = planAnalyzeSubtours.getSubtours().get(subTourIndex);
			Integer parentSubtourIndex = planAnalyzeSubtours.getParentTours().get(subTourIndex);
			Set<String> usableChainBasedModes = new HashSet<String>();
			if (parentSubtourIndex == null) {
				usableChainBasedModes.addAll(CHAIN_BASED_MODES);
			} else {
				List<PlanElement> parentSubtour = planAnalyzeSubtours.getSubtours().get(parentSubtourIndex);
				String mode = getTransportMode(parentSubtour);
				usableChainBasedModes.add(mode);
			}
			Set<String> usableModes = new HashSet<String>();
			for (String candidate : possibleModes) {
				if (CHAIN_BASED_MODES.contains(candidate)) {
					if (usableChainBasedModes.contains(candidate)) {
						usableModes.add(candidate);
					}
				} else {
					usableModes.add(candidate);
				}
			}
			usableModes.remove(getTransportMode(subTour));
			for (String transportMode : usableModes) {
				Candidate candidate = new Candidate();
				candidate.subTourIndex = subTourIndex;
				candidate.newTransportMode = transportMode;
				candidates.add(candidate);
			}
		}
		return candidates;
	}

	private void changeToRandomLegMode(List<PlanElement> tour) {
		final String currentMode = getTransportMode(tour);
		int newModeIdx = chooseModeOtherThan(currentMode);
		String newMode = this.possibleModes[newModeIdx];
		changeLegModeTo(tour, newMode);
	}

	private String getTransportMode(final List<PlanElement> tour) {
		return ((Leg) (tour.get(1))).getMode();
	}

	private void changeLegModeTo(final List<PlanElement> tour, final String newMode) {
		for (PlanElement pe : tour) {
			if (pe instanceof Leg) {
				((Leg) pe).setMode(newMode);
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
				newModeIdx++;
				break;
			}
		}
		return newModeIdx;
	}

	public boolean isChangeOnlyOneSubtour() {
		return changeOnlyOneSubtour;
	}

	public void setChangeOnlyOneSubtour(boolean changeOnlyOneSubtour) {
		this.changeOnlyOneSubtour = changeOnlyOneSubtour;
	}

	public void setTripStructureAnalysisLayer(
			TripStructureAnalysisLayerOption tripStructureAnalysisLayer) {
		planAnalyzeSubtours
				.setTripStructureAnalysisLayer(tripStructureAnalysisLayer);
	}


}
