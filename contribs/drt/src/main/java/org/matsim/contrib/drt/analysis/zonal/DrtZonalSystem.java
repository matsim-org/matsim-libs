/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.analysis.zonal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class DrtZonalSystem {

	private final Map<Id<Link>,String> link2zone = new HashMap<>();
	private final Network network;
	private final Map<String,Geometry> zones;
	
	/**
	 * 
	 */
	public DrtZonalSystem(Network network, double cellsize) {
		this.network = network;
		zones = DrtGridUtils.createGridFromNetwork(network, cellsize);
		
	}
	
	public DrtZonalSystem(Network network, Map<String,Geometry> zones) {
		this.network = network;
		this.zones = zones;
		
	}
	

	public Geometry getZone(String zone){
		return zones.get(zone);
	}
	
	public String getZoneForLinkId(Id<Link> linkId){
		if (this.link2zone.containsKey(linkId)){
			return link2zone.get(linkId);
		}
		
		Point linkCoord = MGC.coord2Point(network.getLinks().get(linkId).getCoord());
		
		for (Entry<String, Geometry> e : zones.entrySet()){
			if (e.getValue().contains(linkCoord)){
				link2zone.put(linkId, e.getKey());
				return e.getKey();
			}
		}
		link2zone.put(linkId, null);
		return null;
		
	}
/**
 * @return the zones
 */
public Map<String, Geometry> getZones() {
	return zones;
}

public Coord getZoneCentroid(String zoneId){
		
		Geometry zone = zones.get(zoneId);
		if (zone == null){
			Logger.getLogger(getClass()).error("Zone "+zoneId+" not found.");
			return null;
		}
		Coord c = MGC.point2Coord(zone.getCentroid());
		return c;
	}
}
