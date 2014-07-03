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
import org.w3c.dom.events.Event;

/**
 * @author lkroeger
 *
 */

public interface NoiseEvent_Interface extends Event {

	public final static String EVENT_TYPE = "noiseEvent";
	
	public final static String ATTRIBUTE_LINK_ID = "linkId";
	public final static String ATTRIBUTE_VEHICLE_ID = "vehicleId";
	public final static String ATTRIBUTE_AGENT_ID = "agentId";
	public final static String ATTRIBUTE_AMOUNT_DOUBLE = "amount";
	
	public double getTime();
	
	public Id getLinkId();
	
	public Id getVehicleId();
	
	public Id getAgentId();
	
	public double getAmount();
	
	public double setAmount(double amount);
	
	public String getEventType();

}