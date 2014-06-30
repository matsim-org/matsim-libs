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
package playground.ikaddoura.noise;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

public final class NoiseEvent extends Event {

	public final static String EVENT_TYPE = "noiseEvent";
	
	public final static String ATTRIBUTE_LINK_ID = "linkId";
	public final static String ATTRIBUTE_VEHICLE_ID = "vehicleId";
	public final static String ATTRIBUTE_AGENT_ID = "agentId";
	public final static String ATTRIBUTE_AMOUNT_DOUBLE = "amount";
	
	private final Id agentId;
	private final Id vehicleId;
	private double amount;
	private final Id linkId;
	
	public NoiseEvent(double time , Id agentId , Id vehicleId , double amount , Id linkId) {
		super(time);
		this.agentId = agentId;
		this.vehicleId = vehicleId;
		this.amount = amount;
		this.linkId = linkId;
	}
	
//	public double getTime() {
//		return getTime();
//	}
	
	public Id getLinkId() {
		return linkId;
	}
	
	public Id getVehicleId() {
		return vehicleId;
	}
	
	public Id getAgentId() {
		return agentId;
	}
	
	public double getAmount() {
		return amount;
	}
	
	public void setAmount(double amount) {
		this.amount = amount;
	}
	
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

}