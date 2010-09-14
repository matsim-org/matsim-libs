/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAnalyzeTourModeChoiceSet.java
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

package playground.meisterk.org.matsim.population.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.world.MappedLocation;

/**
 * Feasible mode chain analysis according to section 3.2 of
 *
 * Miller, E. J., M. J. Roorda and J. A. Carrasco (2005) A tour-based model of travel mode choice,
 * Transportation, 32 (4) 399-422, pp. 404 and 405.
 *
 * For more information, see documentation <a href=http://matsim.org/node/267">here</a>.
 *
 * @author meisterk
 *
 */
public class PlanAnalyzeTourModeChoiceSet implements PlanAlgorithm {

	private static Logger log = Logger.getLogger(PlanAnalyzeTourModeChoiceSet.class);

	private final Set<String> chainBasedModes;
	private final PlanomatConfigGroup.TripStructureAnalysisLayerOption tripStructureAnalysisLayer;
	private final ActivityFacilities facilities;
	private final Network network;

	public PlanAnalyzeTourModeChoiceSet(
			final Set<String> chainBasedModes,
			final PlanomatConfigGroup.TripStructureAnalysisLayerOption tripStructureAnalysisLayer,
			final ActivityFacilities facilities, final Network network) {
		super();
		this.chainBasedModes = chainBasedModes;
		this.tripStructureAnalysisLayer = tripStructureAnalysisLayer;
		this.facilities = facilities;
		this.network = network;
	}

	private ArrayList<String[]> choiceSet = null;

	public ArrayList<String[]> getChoiceSet() {
		return choiceSet;
	}

	private Set<String> modeSet = null;

	public Set<String> getModeSet() {
		return modeSet;
	}

	public void setModeSet(Set<String> modeSet) {
		this.modeSet = modeSet;
	}

	@Override
	public void run(Plan plan) {

		// how many mode combinations are possible?
		int numLegs = plan.getPlanElements().size() / 2;

		int numCombinations = (int) Math.pow(this.modeSet.size(), numLegs);

		this.choiceSet = new ArrayList<String[]>();

		for (int numCombination = 0; numCombination < numCombinations; numCombination++) {

			String[] candidate = new String[numLegs];

			/*
			 * TODO Replace this way to generate a permutation over modes by something without strings, but with Enum ordinals.
			 */
			String modeIndices = Integer.toString(numCombination, this.modeSet.size());
			while (modeIndices.length() < numLegs) {
				modeIndices = "0".concat(modeIndices);
			}
			for (int legNum = 0; legNum < candidate.length; legNum++) {
				String legMode = (String) this.modeSet.toArray()[Integer.parseInt(modeIndices.substring(legNum, legNum + 1))];
				candidate[legNum] = legMode;
			}
			/*
			 * Replace end.
			 */

			int legNum = PlanAnalyzeTourModeChoiceSet.analyzeModeChainFeasability(
					plan,
					candidate,
					this.chainBasedModes,
					this.tripStructureAnalysisLayer,
					this.facilities,
					this.network);

			if (this.doLogging) {
				log.info(numCombination + "\t");
				for (String mode : candidate) {
					log.info(mode + "\t");
				}
				log.info("returns: " + legNum);
			}
			if (legNum >= 0) {
				if (legNum == numLegs) {
					this.choiceSet.add(candidate);
				}
				numCombination += ((int) Math.pow(this.modeSet.size(), numLegs - legNum)) - 1;
			}

		}
	}

	/**
	 * Determines whether a particular set of modes is a feasible combination for a given sequence of activity locations in an activity plan.
	 *
	 * @param plan contains the activities and their locations
	 * @param candidate the mode chain whose feasibility is checked
	 * @param chainBasedModes the set of chain based modes
	 * @param tripStructureAnalysisLayer indicating whether facility or link is used as the location
	 * @return true if the mode chain is feasible, false if it is not
	 */
	public static boolean isModeChainFeasible(
			Plan plan,
			String[] candidate,
			Set<String> chainBasedModes,
			PlanomatConfigGroup.TripStructureAnalysisLayerOption tripStructureAnalysisLayer,
			ActivityFacilities facilities,
			Network network) {

		int numLegs = plan.getPlanElements().size() / 2;
		int lastFeasibleLegNum = analyzeModeChainFeasability(plan, candidate, chainBasedModes, tripStructureAnalysisLayer, facilities, network);

		return (numLegs == lastFeasibleLegNum);

	}

	/**
	 * Determines whether a particular set of modes is a feasible combination for a given sequence of activity locations in an activity plan.
	 *
	 * @param plan contains the activities and their locations
	 * @param candidate the mode chain whose feasibility is checked
	 * @param chainBasedModes the set of chain based modes
	 * @param tripStructureAnalysisLayer indicating whether facility or link is used as the location
	 * @return An integer value in the range [-numLegs; numLegs].
	 * The absolute of the return value indicates the leg number which is not feasible because the chain based mode is not available.
	 * The sign indicates whether the chain based modes are at the location of the first or the last activity if the plan is completed,
	 * thus if the mode chain is feasible or not.
	 */
	public static int analyzeModeChainFeasability(
			Plan plan,
			String[] candidate,
			Set<String> chainBasedModes,
			PlanomatConfigGroup.TripStructureAnalysisLayerOption tripStructureAnalysisLayer,
			ActivityFacilities facilities,
			Network network) {

		boolean isModeChainFeasible = true;

		MappedLocation currentLocation = null, requiredLocation = null, nextLocation = null;

		// setup the trackers for all chain-based modes, set all chain-based modes starting at the first location (usually home)
		HashMap<String, MappedLocation> modeTracker = new HashMap<String, MappedLocation>();
		for (String mode : candidate) {
			if (!modeTracker.containsKey(mode)) {
				if (chainBasedModes.contains(mode)) {
					if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(tripStructureAnalysisLayer)) {
						currentLocation = (MappedLocation) facilities.getFacilities().get(((PlanImpl) plan).getFirstActivity().getFacilityId());
					} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(tripStructureAnalysisLayer)) {
						currentLocation = (MappedLocation) network.getLinks().get(((PlanImpl) plan).getFirstActivity().getLinkId());
					}
					modeTracker.put(mode, currentLocation);
				}
			}
		}

		int legNum = 0;
		Iterator<PlanElement> peIterator = plan.getPlanElements().iterator();
		while (isModeChainFeasible && peIterator.hasNext()) {
			PlanElement pe = peIterator.next();
			if (pe instanceof Leg) {
				Leg currentLeg = (Leg) pe;

				String legMode = candidate[legNum];

				if (chainBasedModes.contains(legMode)) {
					currentLocation = modeTracker.get(legMode);
					if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(tripStructureAnalysisLayer)) {
						requiredLocation = (MappedLocation) facilities.getFacilities().get(((PlanImpl) plan).getPreviousActivity(currentLeg).getFacilityId());
					} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(tripStructureAnalysisLayer)) {
						requiredLocation = (MappedLocation) network.getLinks().get(((PlanImpl) plan).getPreviousActivity(currentLeg).getLinkId());
					}
					if (currentLocation.equals(requiredLocation)) {
						if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(tripStructureAnalysisLayer)) {
							nextLocation = (MappedLocation) facilities.getFacilities().get(((PlanImpl) plan).getNextActivity(currentLeg).getFacilityId());
						} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(tripStructureAnalysisLayer)) {
							nextLocation = (MappedLocation) network.getLinks().get(((PlanImpl) plan).getNextActivity(currentLeg).getLinkId());
						}
						modeTracker.put(legMode, nextLocation);
					} else {
						isModeChainFeasible = false;
					}

				}
				if (isModeChainFeasible) {
					legNum++;
				}
			}
		}

		if (isModeChainFeasible) {

			// chain-based modes must finish at the location of the last activity of the plan
			HashSet<MappedLocation> allowedLocations = new HashSet<MappedLocation>();
			if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility
					.equals(tripStructureAnalysisLayer)) {
				allowedLocations.add((MappedLocation) facilities.getFacilities().get(((PlanImpl) plan).getFirstActivity().getFacilityId()));
				allowedLocations.add((MappedLocation) facilities.getFacilities().get(((PlanImpl) plan).getLastActivity().getFacilityId()));
			} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link
					.equals(tripStructureAnalysisLayer)) {
				allowedLocations.add((MappedLocation) network.getLinks().get(((PlanImpl) plan).getFirstActivity().getLinkId()));
				allowedLocations.add((MappedLocation) network.getLinks().get(((PlanImpl) plan).getLastActivity().getLinkId()));
			}

			Iterator<String> modeTrackerCheck = modeTracker.keySet().iterator();
			while (isModeChainFeasible && modeTrackerCheck.hasNext()) {
				String mode = modeTrackerCheck.next();
				currentLocation = modeTracker.get(mode);
				if (!allowedLocations.contains(currentLocation)) {
					isModeChainFeasible = false;
				}
			}

		}

		if (!isModeChainFeasible) {
			legNum = -legNum;
		}

		return legNum;

	}

	private boolean doLogging = false;

	public void setDoLogging(boolean doLogging) {
		this.doLogging = doLogging;
	}

}
