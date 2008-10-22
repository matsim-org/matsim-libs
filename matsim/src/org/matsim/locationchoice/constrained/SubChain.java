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

package org.matsim.locationchoice.constrained;

import java.util.List;
import java.util.Vector;

import org.matsim.population.Act;
import org.matsim.utils.geometry.Coord;

public class SubChain {
	
	private Act firstPrimAct = null;
	private Act lastPrimAct = null;
	
	private List<Act> secondaryActs = null;
	private double ttBudget = 0.0;
	private double totalTravelDistance = 0.0;
	
	private Coord startCoord = null;
	private Coord endCoord = null;

	
	public SubChain() {
		secondaryActs = new Vector<Act>();		
	}
	
	public void addAct(Act act) {
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

	public List<Act> getSlActs() {
		return secondaryActs;
	}

	public void setSlActs(List<Act> slActs) {
		this.secondaryActs = slActs;
	}

	public Act getFirstPrimAct() {
		return firstPrimAct;
	}

	public void setFirstPrimAct(Act firstPrimAct) {
		this.firstPrimAct = firstPrimAct;
	}

	public Act getLastPrimAct() {
		return lastPrimAct;
	}

	public void setLastPrimAct(Act lastPrimAct) {
		this.lastPrimAct = lastPrimAct;
	}

	public double getTotalTravelDistance() {
		return totalTravelDistance;
	}

	public void setTotalTravelDistance(double totalTravelDistance) {
		this.totalTravelDistance = totalTravelDistance;
	}
		
}
