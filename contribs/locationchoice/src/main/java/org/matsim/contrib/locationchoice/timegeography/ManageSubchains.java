/* *********************************************************************** *
 * project: org.matsim.*
 * ManageSubchains.java
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

package org.matsim.contrib.locationchoice.timegeography;

import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;

public class ManageSubchains {

	private List<SubChain> subChains = new Vector<SubChain>();
	private int subChainIndex = -1;
	boolean chainStarted = false;
	boolean secondaryActFound = false;

	private double ttBudget = 0.0;
	private double totalTravelDistance = 0.0;

	public void secondaryActivityFound(Activity act, Leg leg) {
		/*
		 * No plan starts with secondary activity!
		 */
		this.subChains.get(subChainIndex).defineMode(leg.getMode());
		this.subChains.get(subChainIndex).addAct(act);
		this.secondaryActFound = true;
		this.ttBudget += leg.getTravelTime();
		this.totalTravelDistance += leg.getRoute().getDistance();
	}

	public void primaryActivityFound(Activity act, Leg leg) {
		/*
		 * close chain
		 */
		if (chainStarted) {
			if (secondaryActFound) {
				this.subChains.get(subChainIndex).setTotalTravelDistance(this.totalTravelDistance);
				this.subChains.get(subChainIndex).setTtBudget(this.ttBudget);
				this.subChains.get(subChainIndex).setEndCoord(act.getCoord());
				this.subChains.get(subChainIndex).setLastPrimAct(act);
			}
			else {
				this.subChains.remove(subChainIndex);
				this.subChainIndex--;
			}
		}

		// it is not the second home act
		if (!(leg == null)) {
			//open chain
			this.subChains.add(new SubChain());
			this.subChainIndex++;
			this.subChains.get(subChainIndex).setFirstPrimAct(act);
			this.subChains.get(subChainIndex).setStartCoord(act.getCoord());
			this.chainStarted = true;
			this.secondaryActFound = false;
			this.ttBudget = leg.getTravelTime();
			this.totalTravelDistance = leg.getRoute().getDistance();
			this.subChains.get(subChainIndex).defineMode(leg.getMode());
		}
	}

	public List<SubChain> getSubChains() {
		return subChains;
	}

	public void setSubChains(List<SubChain> subChains) {
		this.subChains = subChains;
	}
}
