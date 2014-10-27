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
package playground.ikaddoura.noise2;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

/**
 * @author lkroeger
 *
 */

public final class NoiseEventAffected extends Event {

	public final static String EVENT_TYPE = "noiseEventAffected";
	
	public final static String ATTRIBUTE_LINK_ID = "linkId";
	public final static String ATTRIBUTE_VEHICLE_ID = "vehicleId";
	public final static String ATTRIBUTE_AGENT_ID = "agentId";
	public final static String ATTRIBUTE_AMOUNT_DOUBLE = "amount";
	
	private final Id affectedAgentId;
	private double amount;
	private final Id receiverPointId;
	private String actType;
	
	public NoiseEventAffected(double time , Id affectedAgentId , double amount , Id receiverPointId , String actType) {
		super(time);
		this.affectedAgentId = affectedAgentId;
		this.amount = amount;
		this.receiverPointId = receiverPointId;
		this.actType = actType;
	}
	
	public Id getrReceiverPointId() {
		return receiverPointId;
	}
	
	public Id getAffectedAgentId() {
		return affectedAgentId;
	}
	
	public double getAmount() {
		return amount;
	}
	
	public void setAmount(double amount) {
		this.amount = amount;
	}
	
	public String getActType() {
		return actType;
	}
	
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

}