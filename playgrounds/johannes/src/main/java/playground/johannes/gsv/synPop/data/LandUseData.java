/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.data;

import java.util.Map;

import playground.johannes.sna.gis.ZoneLayer;

/**
 * @author johannes
 *
 */
public class LandUseData {

	public static final String POPULATION_KEY = "population";
	
	private ZoneLayer<Map<String, Object>> zoneLayer;
	
	public LandUseData(ZoneLayer<Map<String, Object>> zoneLayer) {
		this.zoneLayer = zoneLayer;
	}
	
	public ZoneLayer<Map<String, Object>> getZoneLayer() {
		return zoneLayer;
	}
}
