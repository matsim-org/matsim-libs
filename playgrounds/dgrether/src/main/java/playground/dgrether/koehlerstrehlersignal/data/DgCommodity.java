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

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


/**
 * @author dgrether
 *
 */
public class DgCommodity {

	private Id<DgCommodity> id;
	private Id<DgCrossingNode> sourceNode; 
	private Id<DgCrossingNode> drainNode;
	private Id<Link> sourceLink;
	private Id<Link> drainLink;
	private double flow;
	private List<Id<DgStreet>> route; 
	
	public DgCommodity(Id<DgCommodity> id){
		this.id = id;
	}

	public DgCommodity(Id<DgCommodity> id, Id<DgCrossingNode> sourceNode,
			Id<DgCrossingNode> drainNode, double flow) {
		this.id = id;
		this.sourceNode = sourceNode;
		this.drainNode = drainNode;
		this.flow = flow;
	}
	
	public DgCommodity(Id<DgCommodity> id, Id<DgCrossingNode> sourceNode,
			Id<DgCrossingNode> drainNode, double flow, List<Id<DgStreet>> route) {
		this.id = id;
		this.sourceNode = sourceNode;
		this.drainNode = drainNode;
		this.flow = flow;
		this.route = route;
	}

	public Id<DgCommodity> getId() {
		return this.id;
	}

	
	public boolean hasRoute(){
		if (this.route == null || this.route.isEmpty())
			return false;
		return true;
	}

	public void setSourceNode(Id<DgCrossingNode> fromNodeId, Id<Link> fromLinkId, double flow) {
		this.sourceNode = fromNodeId;
		this.sourceLink = fromLinkId;
		this.flow = flow;
	}
	
	public void setDrainNode(Id<DgCrossingNode> toNodeId, Id<Link> toLinkId){
		this.drainNode = toNodeId;
		this.drainLink = toLinkId;
	}
	
	public void setFlow(double flow){
		this.flow = flow;
	}
	
	public void setRoute(List<Id<DgStreet>> route) {
		this.route = route;
	}
	
	public void addStreetToRoute(Id<DgStreet> street){
		this.route.add(street);
	}

	public List<Id<DgStreet>> getRoute() {
		return route;
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
		return this.flow;
	}
	
}
