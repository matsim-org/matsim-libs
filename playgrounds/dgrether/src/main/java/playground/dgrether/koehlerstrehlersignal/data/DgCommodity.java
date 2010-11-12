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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class DgCommodity {

	private Id id;
	private Map<Id, Double> sourceNodes = new HashMap<Id, Double>();
	private Set<Id> drainNodes = new HashSet<Id>();
	
	public DgCommodity(Id id){
		this.id = id;
	}


	public Id getId() {
		return this.id;
	}


	public void addSourceNode(Id fromNodeId, Double flow) {
		this.sourceNodes.put(fromNodeId, flow);
	}
	
	public void addDrainNode(Id toNodeId){
		this.drainNodes.add(toNodeId);
	}
	
	
	public Set<Id> getDrainNodes(){
		return this.drainNodes;
	}
	
	public Map<Id, Double> getSourceNodesFlowMap(){
		return this.sourceNodes;
	}
	
}
