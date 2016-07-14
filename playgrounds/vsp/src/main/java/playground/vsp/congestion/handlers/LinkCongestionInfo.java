/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.vsp.congestion.handlers;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import playground.vsp.congestion.AgentOnLinkInfo;
import playground.vsp.congestion.DelayInfo;


/**
 * Collects all link specific informations which are required for the calculation of external congestion effects.
 * 
 * @author ikaddoura
 *
 */
public final class LinkCongestionInfo {
	public static class Builder {
		private Id<Link> linkId;
		private double freeTravelTime;
		private double marginalDelayPerLeavingVehicle_sec;
		private double storageCapacityCars;
		public Builder setLinkId( Id<Link> id ) {
			this.linkId = id ; return this ;
		}
		public Builder setFreeTravelTime( double val ) {
			this.freeTravelTime = val ; return this ;
		}
		public Builder setMarginalDelayPerLeavingVehicle_sec( double val ){
			this.marginalDelayPerLeavingVehicle_sec = val ; return this ;
		}
		public Builder setStorageCapacityCars( double val ) {
			this.storageCapacityCars = val ; return this ;
		}
		public LinkCongestionInfo build() {
			return new LinkCongestionInfo( this.linkId, this.freeTravelTime, this.marginalDelayPerLeavingVehicle_sec, this.storageCapacityCars ) ;
		}
	}
	private LinkCongestionInfo( Id<Link> linkId, double freeTravelTime, double marginalDelayPerLeavingVehicle_sec, double storageCapacityCars ) {
		this.linkId = linkId ; 
		this.freeTravelTime = freeTravelTime ;
		this.marginalDelayPerLeavingVehicle_sec = marginalDelayPerLeavingVehicle_sec ;
		this.storageCapacityCars = storageCapacityCars ;
	}

	private final Id<Link> linkId;
	private final double freeTravelTime;
	private final double marginalDelayPerLeavingVehicle_sec;
	private final double storageCapacityCars;

//	private final Map<Id<Person>, Double> personId2freeSpeedLeaveTime = new HashMap<>();
//	private final Map<Id<Person>, Double> personId2linkEnterTime = new LinkedHashMap<>();

	private final Deque<DelayInfo> flowQueue = new LinkedList<>();
	private final LinkedHashMap<Id<Person>, AgentOnLinkInfo> agentsOnLink = new LinkedHashMap<>() ;

	private LinkLeaveEvent lastLeavingAgent;

	public Id<Link> getLinkId() {
		return linkId;
	}
	public double getMarginalDelayPerLeavingVehicle_sec() {
		return marginalDelayPerLeavingVehicle_sec;
	}
	public double getFreeTravelTime() {
		return freeTravelTime;
	}
//	public Map<Id<Person>, Double> getPersonId2freeSpeedLeaveTime() {
//		return personId2freeSpeedLeaveTime;
//	}
	public LinkLeaveEvent getLastLeaveEvent() {
		return lastLeavingAgent;
	}
	public void memorizeLastLinkLeaveEvent(LinkLeaveEvent event) {
		this.lastLeavingAgent = event;
	}

//	public Map<Id<Person>, Double> getPersonId2linkEnterTime() {
//		return personId2linkEnterTime;
//	}

	public double getStorageCapacityCars() {
		return storageCapacityCars;
	}

	/**
	 * Vehicles that have left the link in previous time steps, while the bottleneck was "active".  
	 * The flow queue is in consequence interrupted when the bottleneck is not active, which is when time headway > 1/cap + eps
	 */
	public Deque<DelayInfo> getFlowQueue() {
		return flowQueue;
	}
	public LinkedHashMap<Id<Person>, AgentOnLinkInfo> getAgentsOnLink() {
		return this.agentsOnLink ;
	}

}
