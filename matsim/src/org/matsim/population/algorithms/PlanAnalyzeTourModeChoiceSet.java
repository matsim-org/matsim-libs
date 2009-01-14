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

package org.matsim.population.algorithms;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.world.Location;

public class PlanAnalyzeTourModeChoiceSet implements PlanAlgorithm {

	private EnumSet<BasicLeg.Mode> modeSet = null;

	private ArrayList<BasicLeg.Mode[]> result = null;
	
	public ArrayList<BasicLeg.Mode[]> getResult() {
		return result;
	}

	public EnumSet<BasicLeg.Mode> getModeSet() {
		return modeSet;
	}

	public void setModeSet(EnumSet<BasicLeg.Mode> modeSet) {
		this.modeSet = modeSet;
	}

	public void run(Plan plan) {
		
		// how many mode combinations are possible?
		int numLegs = plan.getActsLegs().size() / 2;
		
		int numCombinations = (int) Math.pow(numLegs, this.modeSet.size());
//		System.out.println(Integer.toString(numCombinations));
//
//		System.out.println();
		
		this.result = new ArrayList<BasicLeg.Mode[]>();

		for (int numCombination = 0; numCombination < numCombinations; numCombination++) {

			// setup the trackers for all chain-based modes, set all chain-based modes starting at the first location (usually home)
			HashMap<BasicLeg.Mode, Location> modeTracker = new HashMap<BasicLeg.Mode, Location>();
			for (BasicLeg.Mode mode : this.modeSet) {
				if (mode.isChainBased()) {
					modeTracker.put(mode, plan.getFirstActivity().getFacility());
//					System.out.println(mode + " " + modeTracker.get(mode).getId());
				}
			}

//			System.out.println();

			BasicLeg.Mode[] candidate = new BasicLeg.Mode[numLegs]; 

			String modeIndices = Integer.toString(numCombination, this.modeSet.size());
			for (int ii=0; ii < (numLegs - modeIndices.length()); ii++) {
				modeIndices = "0".concat(modeIndices);
			}
//			System.out.println(modeIndices);
			LegIterator legIterator = plan.getIteratorLeg();
			boolean isFeasible = true;
			int legNum = 0;
			while (isFeasible && legIterator.hasNext()) {

				Leg currentLeg = (Leg) legIterator.next();

				BasicLeg.Mode legMode = (BasicLeg.Mode) this.modeSet.toArray()[Integer.parseInt(modeIndices.substring(legNum, legNum + 1))];
//				System.out.println("Mode test for leg num " + Integer.toString(legNum) + ": " + legMode);
				if (legMode.isChainBased()) {
					Location currentLocation = modeTracker.get(legMode);
					Location requiredLocation = plan.getPreviousActivity(currentLeg).getFacility();
					if (currentLocation.equals(requiredLocation)) {
						candidate[legNum] = legMode;
						modeTracker.put(legMode, plan.getNextActivity(currentLeg).getFacility());

					} else {
//						System.out.println("Mode chain not fesible. Aborting...");
						isFeasible = false;
					}
					
				} else {
					candidate[legNum] = legMode;
				}
				legNum++;
			}
			// chain-based modes must finish at the location of the last activity of the plan
			// TODO this is not entirely correct: they must finish there IF theey were used. fix that tomorrow!
			for (BasicLeg.Mode mode : modeTracker.keySet()) {
				Location currentLocation = modeTracker.get(mode);
				Location requiredLocation = plan.getLastActivity().getFacility();
				if (!currentLocation.equals(requiredLocation)) {
					isFeasible = false;
					continue;
				}
			}
			if (isFeasible) {
				this.result.add(candidate);
			}
//			System.out.println();
//			System.out.flush();

		}
	}

}
