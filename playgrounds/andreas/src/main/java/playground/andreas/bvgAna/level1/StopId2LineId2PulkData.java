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

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;

/**
 * Collects the <code>VehicleDepartsAtFacilityEvent</code> of one vehicle and the event of its following vehicle to calculate their planned and realized headway.
 * 
 * @author aneumann
 *
 */
public class StopId2LineId2PulkData {
	
	private final Id lineId;
	private final Id stopId;
	
	private final VehicleDepartsAtFacilityEvent vehDepOld;
	private final VehicleDepartsAtFacilityEvent vehDepNew;
	
	private double headwayIST;
	private double headwaySOLL;
	
	protected StopId2LineId2PulkData(Id lineId, VehicleDepartsAtFacilityEvent oldEvent, VehicleDepartsAtFacilityEvent newEvent){
		this.lineId = lineId;
		this.stopId = newEvent.getFacilityId();
		this.vehDepOld = oldEvent;
		this.vehDepNew = newEvent;
		this.headwayIST = newEvent.getTime() - oldEvent.getTime();
		this.headwaySOLL = (newEvent.getTime() - newEvent.getDelay()) - (oldEvent.getTime() - oldEvent.getDelay());
	}
	
	/**
	 * @return Returns true if the two vehicles are considered to bunch
	 */
	public boolean isPulk(){
		if(this.headwayIST < 0.5 * this.headwaySOLL){
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		StringBuffer strB = new StringBuffer();
		strB.append("Line " + this.lineId);
		strB.append(" at stop " + this.stopId);
		strB.append(" with headway IST " + this.headwayIST);
		strB.append(" and headway SOLL " + this.headwaySOLL);
		strB.append(" - veh dep event one " + this.vehDepOld);
		strB.append(" - veh dep event two " + this.vehDepNew);
		return strB.toString();
	}
}
