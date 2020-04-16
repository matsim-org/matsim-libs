/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingproxy;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * Class to identify access and egress walk legs from/to a certain "main" leg mode (e.g. car or pt)
 * 
 * @author tkohl / Senozon
 *
 */
class AccessEgressFinder {
	
	/**
	 * Simple container class holding a leg and an act. Could be replaced by a Tuple, but I think the explicit
	 * field names result in easier to read code.
	 */
	public static final class LegActPair {
		private LegActPair() {}
		public Leg leg;
		public Activity act;
	}
	
	private static enum EgressStage {leg, walk, activity}
	private static enum AccessStage {activity, walk, leg}

	private final String legmode;
	
	/**
	 * Initiates the class by defining the relevant mode you want to know access and egress walks of.
	 * 
	 * @param legmode the mode as used in the plans
	 */
	public AccessEgressFinder(String legmode) {
		this.legmode = legmode;
	}
	
	/**
	 * Finds all egress walks corresponding to the legmode and returns them together with the activity <b>following</b> that walk.
	 * 
	 * @param plan The plan in which to search for egress walks
	 * @return A chronological list of {@linkplain LegActPair}s each containing the egress Leg and the following Activity.
	 */
	public List<LegActPair> findEgressWalks(Plan plan) {
		List<LegActPair> foundEgressWalks = new LinkedList<LegActPair>();
		
		// we need to go through a bit of a hussle here to differentiate between egress walks
		// from cars from those from pt. We basically emulate a state machine cycling through
		// states defined in the EgressStage enum.
		EgressStage nextStage = EgressStage.leg; //first state is a leg, because we never start with an egress_walk.
		LegActPair pair = new LegActPair();
		for (PlanElement element : plan.getPlanElements()) {
			if (element instanceof Activity) {
				Activity act = (Activity) element;
				if (nextStage == EgressStage.activity) {
					// we only come here after an egress walk, i.e. that's our activity to store!
					pair.act = act;
					foundEgressWalks.add(pair);
					pair = new LegActPair();
					nextStage = EgressStage.leg;
				}
			} else if (element instanceof Leg) {
				Leg leg = (Leg) element;
				if (nextStage == EgressStage.leg && leg.getMode().equals(this.legmode)) {
					// when we're going with the relevant mode the next leg will be an appropriate egress leg.
					nextStage = EgressStage.walk;
				} else if (nextStage == EgressStage.walk && leg.getMode().equals(TransportMode.walk)) {
					// this is our egress walk (we wouldn't be here after another mode interaction). Save this leg (we'll modify it soon).
					// Next up is the activity we came for.
					pair.leg = leg;
					nextStage = EgressStage.activity;
				}
			}
		}
		
		return foundEgressWalks;
	}
	
	/**
	 * Finds all access walks corresponding to the legmode and returns them together with the activity <b>before</b> that walk.
	 * 
	 * @param plan The plan in which to search for access walks
	 * @return A chronological list of {@linkplain LegActPair}s each containing the access Leg and the previous Activity.
	 */
	public List<LegActPair> findAccessWalks(Plan plan) {
		List<LegActPair> foundAccessWalks = new LinkedList<LegActPair>();
		
		// we need to go through a bit of a hussle here to differentiate between access walks
		// to cars from those to pt. We basically emulate a state machine cycling through
		// states defined in the AccessStage enum.
		AccessStage nextStage = AccessStage.activity; //first state is an act, because we don't have access walks from nowhere.
		LegActPair pair = new LegActPair();
		for (PlanElement element : plan.getPlanElements()) {
			if (element instanceof Activity) {
				Activity act = (Activity) element;
				if (nextStage == AccessStage.activity) {
					// we preemptively store all activities because we don't know anything about how we leave from here.
					// if it's not be the relevant leg, we'll just forget it again.
					pair.act = act;
					nextStage = AccessStage.walk;
				}
			} else if (element instanceof Leg) {
				Leg leg = (Leg) element;
				if (nextStage == AccessStage.walk) {
					if (leg.getMode().equals(TransportMode.walk)) {
						// this may be our access walk. It's an access walk to somewhere, we'll find out about the mode soon enough.
						pair.leg = leg;
						nextStage = AccessStage.leg;
					} else {
						// false alarm, we're leaving by e.g. foot
						pair = new LegActPair();
						nextStage = AccessStage.activity;
					}
				} else if (nextStage == AccessStage.leg) {
					if (leg.getMode().equals(this.legmode)) {
						// bingo!
						foundAccessWalks.add(pair);
						pair = new LegActPair();
						nextStage = AccessStage.activity;
					} else {
						// false alarm again, we're leaving with another mode
						pair = new LegActPair();
						nextStage = AccessStage.activity;
					}
				}
			}
		}
		
		return foundAccessWalks;
	}
}
