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
package playground.droeder.eMobility.v3.poi;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class PoiInfo {
	
	Map<Id, POI> poiMap;
	
	public PoiInfo(Map<Id, POI> pois){
		this.poiMap = pois;
	}
	
	public void add(POI poi){
		this.poiMap.put(poi.getId(), poi);
	}
	
	public PoiInfo(){
		this.poiMap = new HashMap<Id, POI>();
	}
	
	/**
	 * returns true if there is free charging space and false otherwise
	 * 
	 * @param id
	 * @param time
	 * @return
	 */
	public boolean plugVehicle(Id id, double time){
		return this.poiMap.get(id).plugVehicle(time);
	}
}
