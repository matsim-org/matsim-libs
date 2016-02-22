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
package org.matsim.contrib.noise.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.data.ReceiverPoint;

/**
 * @author lkroeger, ikaddoura
 *
 */

public final class NoiseEventAffected extends Event {

	public final static String EVENT_TYPE = "noiseEventAffected";
	
	public final static String ATTRIBUTE_EMERGENCE_TIME = "emergenceTime";
	public final static String ATTRIBUTE_AGENT_ID = "affectedAgentId";
	public final static String ATTRIBUTE_AMOUNT_DOUBLE = "amount";
	public final static String ATTRIBUTE_RECEIVERPOINT_ID = "receiverPointId";
	public final static String ATTRIBUTE_ACTIVTITY_TYPE = "activityType";
	
	private final double emergenceTime;
	private final Id<Person> affectedAgentId;
	private final double amount;
	private final Id<ReceiverPoint> receiverPointId;
	private final String actType;
	
	public NoiseEventAffected(double time, double emergenceTime, Id<Person> affectedAgentId , double amount , Id<ReceiverPoint> receiverPointId , String actType) {
		super(time);
		this.emergenceTime = emergenceTime;
		this.affectedAgentId = affectedAgentId;
		this.amount = amount;
		this.receiverPointId = receiverPointId;
		this.actType = actType;
	}
	
	public Id<ReceiverPoint> getrReceiverPointId() {
		return receiverPointId;
	}
	
	public Id<Person> getAffectedAgentId() {
		return affectedAgentId;
	}
	
	public double getAmount() {
		return amount;
	}
		
	public String getActType() {
		return actType;
	}
	
	public double getEmergenceTime() {
		return emergenceTime;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();
		attrs.put(ATTRIBUTE_EMERGENCE_TIME, Double.toString(this.emergenceTime));
		attrs.put(ATTRIBUTE_AGENT_ID, this.affectedAgentId.toString());
		attrs.put(ATTRIBUTE_AMOUNT_DOUBLE, Double.toString(this.amount));
		attrs.put(ATTRIBUTE_RECEIVERPOINT_ID , this.receiverPointId.toString());
		attrs.put(ATTRIBUTE_ACTIVTITY_TYPE , this.actType.toString());
		return attrs;
	}
	
}