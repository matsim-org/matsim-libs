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
package playground.vsp.analysis.modules.boardingAlightingCount.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class VehicleLocation {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(VehicleLocation.class);
	private Id id;
	private Id locationId;

	public VehicleLocation(Id vehId) {
		this.id = vehId;
		this.locationId = null;
	}
	
	public void setLocationId(Id id){
		this.locationId = id;
	}
	
	public Id getLocationId(){
		return this.locationId;
	}
	
	
}

