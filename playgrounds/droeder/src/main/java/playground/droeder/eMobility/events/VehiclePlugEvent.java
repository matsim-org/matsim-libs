/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.eMobility.events;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.GenericEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.GenericEventImpl;

/**
 * @author droeder
 *
 */
public class VehiclePlugEvent extends GenericEventImpl{
	
	public static final String TYPE = "ParkingEvent";
	public static final String PLUGGED = "plugged";
	public static final String PARKINGLOTID = "parkingLotId";
	

	/**
	 * @param type
	 * @param time
	 */
	public VehiclePlugEvent(double time, boolean plugged, Id parkingLotId) {
		super(TYPE, time);
		super.getAttributes().put(PLUGGED, String.valueOf(plugged));
		super.getAttributes().put(PARKINGLOTID, parkingLotId.toString());
	}
	
	public VehiclePlugEvent(GenericEvent e){
		super(e.getAttributes().get("type"), e.getTime());
		super.getAttributes().put(PLUGGED, e.getAttributes().get(PLUGGED));
		super.getAttributes().put(PARKINGLOTID, e.getAttributes().get(PARKINGLOTID));
	}
	
	public Id getParkingLotId(){
		return new IdImpl(super.getAttributes().get(PARKINGLOTID));
	}
	
	public boolean isPlugged(){
		return new Boolean(super.getAttributes().get(PLUGGED));
	}

}
