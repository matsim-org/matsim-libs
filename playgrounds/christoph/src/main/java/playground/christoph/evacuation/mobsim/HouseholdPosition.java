/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdPosition.java
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;

import playground.christoph.evacuation.mobsim.Tracker.Position;

public class HouseholdPosition {
	
	private final List<AgentPosition> agentPositions;
	private boolean joined;
	private Position position;
	private Id id;
	
	public HouseholdPosition() {
		this.agentPositions = new ArrayList<AgentPosition>();
		this.joined = true;
		this.id = null;
	
		this.position = Position.UNDEFINED;
	}
	
	public void addAgentPosition(AgentPosition position) {
		this.agentPositions.add(position);
	}
	
	public boolean isHouseholdJoined() {
		return this.joined;
	}
	
	public Position getPositionType() {
		return this.position;
	}
	
	public Id getPositionId() {
		return this.id;
	}
	
	public void update() {
		/*
		 * If no agents assigned, no updated is needed.
		 */
		if (agentPositions.size() == 0) return;
		
		Set<Position> positions = new HashSet<Position>();
		Set<Id> ids = new HashSet<Id>();
		for (AgentPosition agentPosition : agentPositions) {
			positions.add(agentPosition.getPositionType());
			ids.add(agentPosition.getPositionId());
		}
		
		/*
		 * If the household is not joined, there is no household position.
		 * Otherwise, its position as well as the type of the position is defined.
		 */
		if (positions.size() > 1 || ids.size() > 1) {
			joined = false;
			id = null;
			position = Position.UNDEFINED;
		} else {
			AgentPosition agentPosition = agentPositions.get(0);
			joined = true;
			position = agentPosition.getPositionType();
			id = agentPosition.getPositionId();
		}
	}
}
