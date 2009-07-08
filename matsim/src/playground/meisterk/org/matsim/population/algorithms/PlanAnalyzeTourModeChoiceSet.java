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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.world.Location;

import playground.meisterk.org.matsim.config.groups.MeisterkConfigGroup;

/**
 * Feasible mode chain analysis accoring to sectio 3.2 of 
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

	private MeisterkConfigGroup meisterk = null;
	
	public PlanAnalyzeTourModeChoiceSet(MeisterkConfigGroup meisterk) {
		super();
		this.meisterk = meisterk;
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

		PlanomatConfigGroup.TripStructureAnalysisLayerOption subtourAnalysisLocationType = Gbl.getConfig().planomat().getTripStructureAnalysisLayer();
		Location currentLocation = null, requiredLocation = null, nextLocation = null;
		
		// how many mode combinations are possible?
		int numLegs = plan.getPlanElements().size() / 2;

		int numCombinations = (int) Math.pow(this.modeSet.size(), numLegs);

		this.result = new ArrayList<TransportMode[]>();

		for (int numCombination = 0; numCombination < numCombinations; numCombination++) {

			// setup the trackers for all chain-based modes, set all chain-based modes starting at the first location (usually home)
			HashMap<TransportMode, Location> modeTracker = new HashMap<TransportMode, Location>();
			for (TransportMode mode : this.modeSet) {
				if (meisterk.getChainBasedModes().contains(mode)) {
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
					if (meisterk.getChainBasedModes().contains(legMode)) {
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
			HashSet<Location> allowedLocations = new HashSet<Location>();
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

}
