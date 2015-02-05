/* *********************************************************************** *
 * project: org.matsim.*
 * DgStreet
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.data;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class DgStreet {

	private DgCrossingNode toNode;
	private DgCrossingNode fromNode;
	private Id<DgStreet> id;
	private long cost;
	private double capacity;
	

	public DgStreet(Id<DgStreet> id, DgCrossingNode fromNode, DgCrossingNode toNode) {
		if (id == null){
			throw new IllegalStateException("Id null not permitted!");
		}
		if (fromNode == null){
			throw new IllegalStateException("fromNode null not permitted!");
		}
		if (toNode == null){
			throw new IllegalStateException("toNode null not permitted! (id: " + id + ")");
		}
		this.toNode = toNode;
		this.fromNode = fromNode;
		this.id = id;
	}

	public DgCrossingNode getToNode(){
		return toNode;
	}
	
	public DgCrossingNode getFromNode() {
		return fromNode;
	}

	public Id<DgStreet> getId() {
		return this.id;
	}

	
	public long getCost() {
		return cost;
	}

	
	public void setCost(long cost) {
		this.cost = cost;
	}

	
	public double getCapacity() {
		return capacity;
	}

	
	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

}
