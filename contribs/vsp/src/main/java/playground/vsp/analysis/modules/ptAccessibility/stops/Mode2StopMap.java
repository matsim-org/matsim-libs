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
package playground.vsp.analysis.modules.ptAccessibility.stops;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * @author droeder
 *
 */
public class Mode2StopMap {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Mode2StopMap.class);
	private HashMap<String, PtStopMap> mode2StopMap;
	private Map<String, Circle> distCluster;

	public Mode2StopMap(Map<String, Circle> distCluster) {
		this.mode2StopMap = new HashMap<String, PtStopMap>();
		this.distCluster = distCluster;
		this.mode2StopMap.put(TransportMode.pt, new PtStopMap(TransportMode.pt, this.distCluster));
	}
	
	public void addStop(String mode, TransitStopFacility facility){
		if(!this.mode2StopMap.containsKey(mode)){
			this.mode2StopMap.put(mode, new PtStopMap(mode, this.distCluster));
		}
		this.mode2StopMap.get(mode).addStop(facility);
		this.mode2StopMap.get(TransportMode.pt).addStop(facility);
	}
	
	public Map<String, PtStopMap> getStopMaps(){
		return this.mode2StopMap;
	}
	
	
}

