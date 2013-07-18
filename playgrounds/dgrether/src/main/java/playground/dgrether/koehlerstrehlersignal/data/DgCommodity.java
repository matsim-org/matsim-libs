/* *********************************************************************** *
 * project: org.matsim.*
 * DgCommodity
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
public class DgCommodity {

	private Id id;
	private Id sourceNode; 
	private Id drainNodes;
	private Double flow;
	
	public DgCommodity(Id id){
		this.id = id;
	}

	public Id getId() {
		return this.id;
	}


	public void setSourceNode(Id fromNodeId, Double flow) {
		this.sourceNode = fromNodeId;
		this.flow = flow;
	}
	
	public void setDrainNode(Id toNodeId){
		this.drainNodes = toNodeId;
	}
	
	
	public Id getDrainNodeId(){
		return this.drainNodes;
	}
	
	public Id getSourceNodeId(){
		return this.sourceNode;
	}
	
	public double getFlow(){
		return this.flow;
	}
	
}
