/* *********************************************************************** *
 * project: org.matsim.*
 * Cell.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.ca2;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.mobsim.framework.MobsimAgent;

public class Cell {

	private MobsimAgent agentInCell;

	private final int id;
	private final Coord coord;
	private final double length;
	
	public Cell(int id, Coord coord, double length) {
		this.id = id;
		this.coord = coord;
		this.length = length;
		
		this.agentInCell = null;
	}
	
	public int getId() {
		return this.id;
	}
	
	public Coord getCoord() {
		return this.coord;
	}
	
	public boolean setAgent(MobsimAgent agentInCell) {
		this.agentInCell = agentInCell;
		return true;
	}
	
	public void reset() {
		this.agentInCell = null;
	}

	public double getLength() {
		return this.length;
	}
        
	public MobsimAgent getAgent() {
		return this.agentInCell;
	}
	
	public boolean hasAgent() {
		return this.agentInCell != null;
	}
}