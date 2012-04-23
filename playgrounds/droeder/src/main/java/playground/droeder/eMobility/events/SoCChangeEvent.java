/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.eMobility.events;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.GenericEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.GenericEventImpl;

/**
 * @author droeder
 *
 */
public class SoCChangeEvent extends GenericEventImpl{
	private static final Logger log = Logger.getLogger(SoCChangeEvent.class);

	public static final String TYPE = "SoCChangeEvent";
	public static final String SOC = "stateOfCharge";
	public static final String LINKID = "linkId";
	public static final String VEHID = "vehId";
	
	/**
	 * @param type
	 * @param time
	 */
	public SoCChangeEvent(Id vehId, double time, double soc, Id linkId) {
		super(TYPE, time);
		super.getAttributes().put(SOC, String.valueOf(soc));
		super.getAttributes().put(LINKID, linkId.toString());
		super.getAttributes().put(VEHID, vehId.toString());
	}

	/**
	 * @param event
	 */
	public SoCChangeEvent(GenericEvent event) {
		super(TYPE, event.getTime());
		if(!event.getAttributes().get("type").equals(TYPE)){
			log.error("can not create a SoCCahngeEvent from " + event.getAttributes().get("type"));
			throw new IllegalArgumentException();
		}else{
			super.getAttributes().put(SOC, event.getAttributes().get(SOC));
			super.getAttributes().put(LINKID, event.getAttributes().get(LINKID));
			super.getAttributes().put(VEHID, event.getAttributes().get(VEHID));
		}
	}

	public Id getLinkId(){
		return new IdImpl(super.getAttributes().get(LINKID));
	}
	
	public double getSoC(){
		return Double.parseDouble(super.getAttributes().get(SOC));
	}
	
	public Id getVehId(){
		return new IdImpl(super.getAttributes().get(VEHID));
	}
}
