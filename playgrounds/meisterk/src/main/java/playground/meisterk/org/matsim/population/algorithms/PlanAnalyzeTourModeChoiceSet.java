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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.world.MappedLocation;

/**
 * Feasible mode chain analysis according to section 3.2 of 
 * 
 * Miller, E. J., M. J. Roorda and J. A. Carrasco (2005) A tour-based model of travel mode choice,
 * Transportation, 32 (4) 399–422, pp. 404 and 405.
 * 
 * For more information, see documentation <a href=http://matsim.org/node/267">here</a>.
 * 
 * @author meisterk
 *
 */
public class PlanAnalyzeTourModeChoiceSet implements PlanAlgorithm {

	private static Logger log = Logger.getLogger(PlanAnalyzeTourModeChoiceSet.class);

	private final EnumSet<TransportMode> chainBasedModes;
	private final PlanomatConfigGroup.TripStructureAnalysisLayerOption tripStructureAnalysisLayer;
	private final ActivityFacilities facilities;
	
	public PlanAnalyzeTourModeChoiceSet(
			final EnumSet<TransportMode> chainBasedModes, 
			final PlanomatConfigGroup.TripStructureAnalysisLayerOption tripStructureAnalysisLayer,
			final ActivityFacilities facilities) {
		super();
		this.chainBasedModes = chainBasedModes;
		this.tripStructureAnalysisLayer = tripStructureAnalysisLayer;
		this.facilities = facilities;
	}

	private ArrayList<TransportMode[]> choiceSet = null;

	public ArrayList<TransportMode[]> getChoiceSet() {
		return choiceSet;
	}

	private EnumSet<TransportMode> modeSet = null;

	public EnumSet<TransportMode> getModeSet() {
		return modeSet;
	}

	public void setModeSet(EnumSet<TransportMode> modeSet) {
		this.modeSet = modeSet;
	}

	public void run(Plan plan) {
		
		// how many mode combinations are possible?
		int numLegs = plan.getPlanElements().size() / 2;

		int numCombinations = (int) Math.pow(this.modeSet.size(), numLegs);

		this.choiceSet = new ArrayList<TransportMode[]>();

		for (int numCombination = 0; numCombination < numCombinations; numCombination++) {

			TransportMode[] candidate = new TransportMode[numLegs]; 

			/*
			 * TODO Replace this way to generate a permutation over modes by something without strings, but with Enum ordinals.
			 */
			String modeIndices = Integer.toString(numCombination, this.modeSet.size());
			while (modeIndices.length() < numLegs) {
				modeIndices = "0".concat(modeIndices);
			}
			for (int legNum = 0; legNum < candidate.length; legNum++) {
				TransportMode legMode = (TransportMode) this.modeSet.toArray()[Integer.parseInt(modeIndices.substring(legNum, legNum + 1))];
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
					this.facilities);
			
			if (this.doLogging) {
				log.info(numCombination + "\t");
				for (TransportMode mode : candidate) {
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
			TransportMode[] candidate, 
			EnumSet<TransportMode> chainBasedModes,
			PlanomatConfigGroup.TripStructureAnalysisLayerOption tripStructureAnalysisLayer,
			ActivityFacilities facilities) {
		
		int numLegs = plan.getPlanElements().size() / 2;
		int lastFeasibleLegNum = analyzeModeChainFeasability(plan, candidate, chainBasedModes, tripStructureAnalysisLayer, facilities);
		
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
			TransportMode[] candidate, 
			EnumSet<TransportMode> chainBasedModes,
			PlanomatConfigGroup.TripStructureAnalysisLayerOption tripStructureAnalysisLayer,
			ActivityFacilities facilities) {

		boolean isModeChainFeasible = true;

		MappedLocation currentLocation = null, requiredLocation = null, nextLocation = null;
		
		// setup the trackers for all chain-based modes, set all chain-based modes starting at the first location (usually home)
		HashMap<TransportMode, MappedLocation> modeTracker = new HashMap<TransportMode, MappedLocation>();
		for (TransportMode mode : candidate) {
			if (!modeTracker.containsKey(mode)) {
				if (chainBasedModes.contains(mode)) {
					if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(tripStructureAnalysisLayer)) {
						currentLocation = (ActivityFacilityImpl) facilities.getFacilities().get(((PlanImpl) plan).getFirstActivity().getFacilityId());
					} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(tripStructureAnalysisLayer)) {
						currentLocation = (LinkImpl) ((PlanImpl) plan).getFirstActivity().getLink();
					}
					modeTracker.put(mode, currentLocation);
				}
			}
		}
		
		int legNum = 0;
		Iterator<PlanElement> peIterator = plan.getPlanElements().iterator();
		while (isModeChainFeasible && peIterator.hasNext()) {
			PlanElement pe = peIterator.next();
			if (pe instanceof LegImpl) {
				LegImpl currentLeg = (LegImpl) pe;

				TransportMode legMode = candidate[legNum];
				
				if (chainBasedModes.contains(legMode)) {
					currentLocation = modeTracker.get(legMode);
					if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(tripStructureAnalysisLayer)) {
						requiredLocation = (ActivityFacilityImpl) facilities.getFacilities().get(((PlanImpl) plan).getPreviousActivity(currentLeg).getFacilityId());
					} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(tripStructureAnalysisLayer)) {
						requiredLocation = (LinkImpl) ((PlanImpl) plan).getPreviousActivity(currentLeg).getLink();
					}
					if (currentLocation.equals(requiredLocation)) {
						if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(tripStructureAnalysisLayer)) {
							nextLocation = (ActivityFacilityImpl) facilities.getFacilities().get(((PlanImpl) plan).getNextActivity(currentLeg).getFacilityId());
						} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(tripStructureAnalysisLayer)) {
							nextLocation = (LinkImpl) ((PlanImpl) plan).getNextActivity(currentLeg).getLink();
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
				allowedLocations.add((ActivityFacilityImpl) facilities.getFacilities().get(((PlanImpl) plan).getFirstActivity().getFacilityId()));
				allowedLocations.add((ActivityFacilityImpl) facilities.getFacilities().get(((PlanImpl) plan).getLastActivity().getFacilityId()));
			} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link
					.equals(tripStructureAnalysisLayer)) {
				allowedLocations.add((LinkImpl) ((PlanImpl) plan).getFirstActivity().getLink());
				allowedLocations.add((LinkImpl) ((PlanImpl) plan).getLastActivity().getLink());
			}
			
			Iterator<TransportMode> modeTrackerCheck = modeTracker.keySet().iterator();
			while (isModeChainFeasible && modeTrackerCheck.hasNext()) {
				TransportMode mode = modeTrackerCheck.next();
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
