/* *********************************************************************** *
 * project: org.matsim.*
 * SubChain.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;

class SubChain {

	private Activity firstPrimAct = null;
	private Activity lastPrimAct = null;

	private List<Activity> secondaryActs = null;
	private double ttBudget = 0.0;
	private double totalTravelDistance = 0.0;

	private Coord startCoord = null;
	private Coord endCoord = null;

	//car, walk, mixed
	private String mode = null;

	public void defineMode(final String mode) {
		if (this.mode == null) {
			this.mode = mode;
			return;
		}
		if (this.mode.equals("mixed")) {
			return;
		}
		if (!this.mode.equals(mode)) {
			this.mode = "mixed";
		}
	}

	public SubChain() {
		secondaryActs = new Vector<Activity>();
	}

	public void addAct(Activity act) {
		this.secondaryActs.add(act);
	}


	public double getTtBudget() {
		return ttBudget;
	}


	public void setTtBudget(double ttBudget) {
		this.ttBudget = ttBudget;
	}

	public Coord getStartCoord() {
		return startCoord;
	}

	public void setStartCoord(Coord startCoord) {
		this.startCoord = startCoord;
	}

	public Coord getEndCoord() {
		return endCoord;
	}

	public void setEndCoord(Coord endCoord) {
		this.endCoord = endCoord;
	}

	public List<Activity> getSlActs() {
		return secondaryActs;
	}

	public void setSlActs(List<Activity> slActs) {
		this.secondaryActs = slActs;
	}

	public Activity getFirstPrimAct() {
		return firstPrimAct;
	}

	public void setFirstPrimAct(Activity firstPrimAct) {
		this.firstPrimAct = firstPrimAct;
	}

	public Activity getLastPrimAct() {
		return lastPrimAct;
	}

	public void setLastPrimAct(Activity lastPrimAct) {
		this.lastPrimAct = lastPrimAct;
	}

	public double getTotalTravelDistance() {
		return totalTravelDistance;
	}

	public void setTotalTravelDistance(double totalTravelDistance) {
		this.totalTravelDistance = totalTravelDistance;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

}
