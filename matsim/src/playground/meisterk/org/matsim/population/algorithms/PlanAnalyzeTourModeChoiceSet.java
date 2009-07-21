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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanomatConfigGroup;
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

	private final EnumSet<TransportMode> chainBasedModes;
	private final PlanomatConfigGroup.TripStructureAnalysisLayerOption tripStructureAnalysisLayer;
	
	public PlanAnalyzeTourModeChoiceSet(
			final EnumSet<TransportMode> chainBasedModes, 
			final PlanomatConfigGroup.TripStructureAnalysisLayerOption tripStructureAnalysisLayer) {
		super();
		this.chainBasedModes = chainBasedModes;
		this.tripStructureAnalysisLayer = tripStructureAnalysisLayer;
	}

	private EnumSet<TransportMode> modeSet = null;

	private ArrayList<TransportMode[]> result = null;

	public ArrayList<TransportMode[]> getResult() {
		return result;
	}

	public EnumSet<TransportMode> getModeSet() {
		return modeSet;
	}

	public void setModeSet(EnumSet<TransportMode> modeSet) {
		this.modeSet = modeSet;
	}

	public void run(PlanImpl plan) {
		
		PlanomatConfigGroup.TripStructureAnalysisLayerOption subtourAnalysisLocationType = this.tripStructureAnalysisLayer;
		MappedLocation currentLocation = null, requiredLocation = null, nextLocation = null;
		
		// how many mode combinations are possible?
		int numLegs = plan.getPlanElements().size() / 2;

		int numCombinations = (int) Math.pow(this.modeSet.size(), numLegs);

		this.result = new ArrayList<TransportMode[]>();

		for (int numCombination = 0; numCombination < numCombinations; numCombination++) {

			// setup the trackers for all chain-based modes, set all chain-based modes starting at the first location (usually home)
			HashMap<TransportMode, MappedLocation> modeTracker = new HashMap<TransportMode, MappedLocation>();
			for (TransportMode mode : this.modeSet) {
				if (this.chainBasedModes.contains(mode)) {
					if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(subtourAnalysisLocationType)) {
						currentLocation = plan.getFirstActivity().getFacility();
					} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(subtourAnalysisLocationType)) {
						currentLocation = plan.getFirstActivity().getLink();
					}
					modeTracker.put(mode, currentLocation);
				}
			}

			TransportMode[] candidate = new TransportMode[numLegs]; 

			String modeIndices = Integer.toString(numCombination, this.modeSet.size());
			while (modeIndices.length() < numLegs) {
				modeIndices = "0".concat(modeIndices);
			}
			boolean modeChainIsFeasible = true;
			int legNum = 0;
			Iterator<PlanElement> peIterator = plan.getPlanElements().iterator();
			while (modeChainIsFeasible && peIterator.hasNext()) {
				PlanElement pe = peIterator.next();
				if (pe instanceof LegImpl) {
					LegImpl currentLeg = (LegImpl) pe;
	
					TransportMode legMode = (TransportMode) this.modeSet.toArray()[Integer.parseInt(modeIndices.substring(legNum, legNum + 1))];
					if (this.chainBasedModes.contains(legMode)) {
						currentLocation = modeTracker.get(legMode);
						if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(subtourAnalysisLocationType)) {
							requiredLocation = plan.getPreviousActivity(currentLeg).getFacility();
						} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(subtourAnalysisLocationType)) {
							requiredLocation = plan.getPreviousActivity(currentLeg).getLink();
						}
						if (currentLocation.equals(requiredLocation)) {
							candidate[legNum] = legMode;
							if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(subtourAnalysisLocationType)) {
								nextLocation = plan.getNextActivity(currentLeg).getFacility();
							} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(subtourAnalysisLocationType)) {
								nextLocation = plan.getNextActivity(currentLeg).getLink();
							}
							modeTracker.put(legMode, nextLocation);
						} else {
							modeChainIsFeasible = false;
							// compute number of next candidate for a feasible combination, that is, omit the detected branch of infeasible combinations
							numCombination += ((int) Math.pow(this.modeSet.size(), (numLegs - legNum - 1))) - 1;
						}
	
					} else {
						candidate[legNum] = legMode;
					}
					legNum++;
				}
			}
			// chain-based modes must finish at the location of the last activity of the plan
			HashSet<MappedLocation> allowedLocations = new HashSet<MappedLocation>();
			if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(subtourAnalysisLocationType)) {
				allowedLocations.add(plan.getFirstActivity().getFacility());
				allowedLocations.add(plan.getLastActivity().getFacility());
			} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(subtourAnalysisLocationType)) {
				allowedLocations.add(plan.getFirstActivity().getLink());
				allowedLocations.add(plan.getLastActivity().getLink());
			}
			Iterator<TransportMode> modeTrackerCheck = modeTracker.keySet().iterator();
			while(modeChainIsFeasible && modeTrackerCheck.hasNext()) {
				TransportMode mode = modeTrackerCheck.next();
				currentLocation = modeTracker.get(mode);
				if (!allowedLocations.contains(currentLocation)) {
					modeChainIsFeasible = false;
				}
			}
			if (modeChainIsFeasible) {
				this.result.add(candidate);
			}

		}
	}

	/**
	 * Determines whether a particular set of modes is a feasible combination for a given sequence of activity locations in an activity plan.
	 * 
	 * @param plan contains the activities and their locations
	 * @param modeChain the mode chain whose feasibility is checked
	 * @return 
	 */
	public static boolean isModeChainFeasible(PlanImpl plan, TransportMode[] modeChain) {
		
		boolean isModeChainFeasible = false;
		
		return isModeChainFeasible;
		
	}
	
}
