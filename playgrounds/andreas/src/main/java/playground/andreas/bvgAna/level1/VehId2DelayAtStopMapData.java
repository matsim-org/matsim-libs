/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgAna.level1;

import java.util.LinkedList;

import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;

/**
 * Simple data container to store the <code>VehicleDepartsAtFacilityEvent</code> following a <code>TransitDriverStartsEvent</code>.
 * 
 * @author aneumann
 *
 */
public class VehId2DelayAtStopMapData {
	
	private final TransitDriverStartsEvent transitDriverStartsEvent;
	private LinkedList<VehicleDepartsAtFacilityEvent> vehicleDepartsAtFacilityEventList = new LinkedList<VehicleDepartsAtFacilityEvent>();
	
	public VehId2DelayAtStopMapData(TransitDriverStartsEvent transitDriverStartsEvent){
		this.transitDriverStartsEvent = transitDriverStartsEvent;
	}

	protected void addVehicleDepartsAtFacilityEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehicleDepartsAtFacilityEventList.add(event);
	}
	
	@Override
	public String toString() {
		StringBuffer strB = new StringBuffer();
		strB.append(this.transitDriverStartsEvent);
		for (VehicleDepartsAtFacilityEvent event : this.vehicleDepartsAtFacilityEventList) {
			strB.append(" - ");
			strB.append(event);
		}		
		return strB.toString();
	}
}
