/* *********************************************************************** *
 * project: org.matsim.*
 * PersonDecisionData.java
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

package playground.christoph.evacuation.mobsim.decisiondata;

import org.matsim.api.core.v01.Id;

import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.Participating;
import playground.christoph.evacuation.mobsim.decisionmodel.PickupModel.PickupDecision;

/**
 * Data structure containing information used by a person for the decision
 * "evacuate directly vs. meet at home first".
 * 
 * @author cdobler
 */
public class PersonDecisionData {
		
	private AgentPosition agentPosition;
	
	/*
	 * Model results
	 */
	private boolean inPanic = false;
	private Participating participating = Participating.UNDEFINED;
	private PickupDecision pickupDecision;
	
	/*
	 * Model input data
	 */
	private final Id personId;
	private Id householdId = null;
	private boolean isAffected = false;
	private boolean hasChildren = false;
	
	/*
	 * Time when the agent would arrive at home when traveling there to
	 * evacuate jointly with its other household members.
	 */
	private double agentReturnHomeTime = Double.MAX_VALUE;
	
	/*
	 * Time when the agent would arrive at a secure facility when evacuating
	 * directly.
	 */	
	private double agentDirectEvacuationTime = Double.MAX_VALUE;
	
	private String agentTransportMode = null;
	private Id agentReturnHomeVehicleId = null;
	
	public PersonDecisionData(Id personId) {
		this.personId = personId;
	}
	
	public Id getPersonId() {
		return this.personId;
	}
	
	public Id getHouseholdId() {
		return householdId;
	}

	public void setHouseholdId(Id householdId) {
		this.householdId = householdId;
	}

	public AgentPosition getAgentPosition() {
		return agentPosition;
	}

	public void setAgentPosition(AgentPosition agentPosition) {
		this.agentPosition = agentPosition;
	}

	public boolean isInPanic() {
		return inPanic;
	}

	public void setInPanic(boolean inPanic) {
		this.inPanic = inPanic;
	}
	
	public Participating getParticipating() {
		return participating;
	}

	public void setParticipating(Participating participating) {
		this.participating = participating;
	}
	
	public PickupDecision getPickupDecision() {
		return pickupDecision;
	}

	public void setPickupDecision(PickupDecision pickupDecision) {
		this.pickupDecision = pickupDecision;
	}

	public boolean isAffected() {
		return isAffected;
	}

	public void setAffected(boolean isAffected) {
		this.isAffected = isAffected;
	}

	public boolean hasChildren() {
		return hasChildren;
	}

	public void setChildren(boolean hasChildren) {
		this.hasChildren = hasChildren;
	}

	public double getAgentReturnHomeTime() {
		return agentReturnHomeTime;
	}

	public void setAgentReturnHomeTime(double agentReturnHomeTime) {
		this.agentReturnHomeTime = agentReturnHomeTime;
	}

	public double getAgentDirectEvacuationTime() {
		return agentDirectEvacuationTime;
	}

	public void setAgentDirectEvacuationTime(double agentDirectEvacuationTime) {
		this.agentDirectEvacuationTime = agentDirectEvacuationTime;
	}

	public String getAgentTransportMode() {
		return agentTransportMode;
	}

	public void setAgentTransportMode(String agentTransportMode) {
		this.agentTransportMode = agentTransportMode;
	}

	public Id getAgentReturnHomeVehicleId() {
		return agentReturnHomeVehicleId;
	}

	public void setAgentReturnHomeVehicleId(Id agentReturnHomeVehicleId) {
		this.agentReturnHomeVehicleId = agentReturnHomeVehicleId;
	}

}