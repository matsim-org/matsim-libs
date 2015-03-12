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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


/**
 * @author dgrether
 * @author tthunig
 */
public class DgCommodity {

	private static final Logger log = Logger.getLogger(DgCommodity.class);

	private Id<DgCommodity> id;
	// source and drain in the ks model
	private Id<DgCrossingNode> sourceNode; 
	private Id<DgCrossingNode> drainNode;
	// source and drain in the matsim model
	private Id<Link> sourceLink;
	private Id<Link> drainLink;
	// total flow of the commodity (sum of all path flows)
	private double totalFlow;
	// container for the paths. Can stay empty.
	private Map<Id<TtPath>, TtPath> paths = new HashMap<>();
	
	public DgCommodity(Id<DgCommodity> id){
		this.id = id;
	}

	public DgCommodity(Id<DgCommodity> id, Id<DgCrossingNode> sourceNode,
			Id<DgCrossingNode> drainNode, double flow) {
		this.id = id;
		this.sourceNode = sourceNode;
		this.drainNode = drainNode;
		this.totalFlow = flow;
	}

	public Id<DgCommodity> getId() {
		return this.id;
	}

	
	public boolean hasPaths(){
		if (this.paths == null || this.paths.isEmpty())
			return false;
		return true;
	}

	public void setSourceNode(Id<DgCrossingNode> fromNodeId, Id<Link> fromLinkId, double flow) {
		this.sourceNode = fromNodeId;
		this.sourceLink = fromLinkId;
		this.totalFlow = flow;
	}
	
	public void setDrainNode(Id<DgCrossingNode> toNodeId, Id<Link> toLinkId){
		this.drainNode = toNodeId;
		this.drainLink = toLinkId;
	}
	
	public void setFlow(double flow){
		this.totalFlow = flow;
	}
	
	/**
	 * Adds the path to the collection of paths or add its flow value, if it already exists.
	 * Also increases the total flow value of the commodity.
	 * 
	 * @param pathId
	 * @param flowValue
	 * @param route
	 */
	public void addPath(Id<TtPath> pathId, List<Id<DgStreet>> path, double flowValue){
		this.totalFlow+= flowValue;
		if (!this.paths.containsKey(pathId)){
			this.paths.put(pathId, new TtPath(pathId, path, 0.0));
		}
		this.paths.get(pathId).increaseFlow(flowValue);
	}
	
	/**
	 * Increases the flow value of the single path AND the total flow value of the commodity
	 * 
	 * @param pathId
	 * @param flow the value to increase
	 */
	public void increaseFlowOfPath(Id<TtPath> pathId, double flow){
		this.totalFlow+= flow;
		this.paths.get(pathId).increaseFlow(flow);
	}
	
	public boolean containsPath (Id<TtPath> pathId){
		return this.paths.containsKey(pathId);
	}
	
	public Map<Id<TtPath>, TtPath> getPaths (){
		return this.paths;
	}

	public Id<DgCrossingNode> getDrainNodeId(){
		return this.drainNode;
	}
	
	public Id<DgCrossingNode> getSourceNodeId(){
		return this.sourceNode;
	}
	
	public Id<Link> getSourceLinkId() {
		return sourceLink;
	}

	public Id<Link> getDrainLinkId() {
		return drainLink;
	}

	public double getFlow(){
		return this.totalFlow;
	}
	
}
