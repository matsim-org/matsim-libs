/* *********************************************************************** *
 * project: org.matsim.*
 * AgentPosition.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.mobsim;

import org.matsim.api.core.v01.Id;

import playground.christoph.evacuation.mobsim.Tracker.Position;

public class AgentPosition {
	
	private Id agentId;
	private Position position;
	private Id id;	// agent's current positionId, i.e. vehicleId, linkId or facilityId
	private String transportMode;
	
	public AgentPosition(Id agentId, Id id, Position position) {
		this.agentId = agentId;
		this.position = position;
		this.id = id;
		this.transportMode = null;
	}
	
	public Id getAgentId() {
		return this.agentId;
	}
	
	public Position getPositionType() {
		return this.position;
	}
	
	public Id getPositionId() {
		return this.id;
	}
	
	public String getTransportMode() {
		return this.transportMode;
	}
	
	public void setTransportMode(String transportMode) {
		this.transportMode = transportMode;
	}
	
	public void entersLink(Id id) {
		/*
		 * If the agent is traveling by car, we keep the vehicleId
		 * as position information.
		 */
		if (this.position == Position.VEHICLE) return;
		
		this.id = id;
		this.position = Position.LINK;
	}
	
	public void leavesLink() {
		/*
		 * If the agent is traveling by car, we keep the vehicleId
		 * as position information.
		 */
		if (this.position == Position.VEHICLE) return;

		this.id = null;
		this.position = Position.UNDEFINED;
	}
	
	public void entersVehicle(Id id) {
		this.id = id;
		this.position = Position.VEHICLE;
	}
	
	public void leavesVehicle() {
		this.id = null;
		this.position = Position.UNDEFINED;
	}
	
	public void entersFacility(Id id) {
		this.id = id;
		this.position = Position.FACILITY;
	}
	
	public void leavesFacility() {
		this.id = null;
		this.position = Position.UNDEFINED;
	}
}
