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


package playground.artemc.crowding.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.internal.HasPersonId;

/**
 * The CrowdedPenaltyEvent's only purpose is to keep track of penalties
 * for crowdedness in PT-Vehicles within the Event log.
 * 
 * @author pbouman
 *
 * "time" and "vehicleId" and "externality" have been added at the original class 
 * @author grerat
 * 
 */

public class CrowdedPenaltyEvent extends Event implements HasPersonId {


	private final double time;
	private final Id personId;
	private final Id vehicleId;
	private final double penalty;
	private final double externality;
	
	/**
	 * The only thing we store is the time, the id of the person who got
	 * the penalty, and the penalty itself.
	 * @param time
	 * @param personId
	 * @param penalty
	 */
	
	public CrowdedPenaltyEvent(double time, Id personId, Id vehicleId, double penalty, double externality) {
		super(time);
		this.time = time;
		this.personId = personId;
		this.vehicleId = vehicleId;
		this.penalty = penalty;
		this.externality = externality;
	}


	@Override
	public String getEventType()
	{
		return "CrowdedPenaltyEvent";
	}

	@Override
	public Map<String,String> getAttributes()
	{
		Map<String,String> attributes = super.getAttributes();
		attributes.put("person", personId.toString());
		attributes.put("penalty", Double.toString(penalty));
		attributes.put("externalities", Double.toString(externality));
		return attributes;
	}
	
	public double getTime() {
		return time;
	}
	
	@Override
	public Id getPersonId() {
		return personId;
	}
	
	public Id getVehicleId() {
		return vehicleId;
	}
	
	public double getPenalty()
	{
		return penalty;
	}
	
	public double getExternality() {
		return externality;
	}

}
