/* *********************************************************************** *
 * project: org.matsim.*
 * CommonUtilies.java
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

/**
 *
 */
package org.matsim.contrib.matsim4urbansim.gis;

import java.util.Iterator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4urbansim.utils.helperobjects.ZoneObject;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.network.NetworkImpl;
import org.matsim.facilities.ActivityFacilitiesImpl;


/**
 * @author thomas
 *
 */
public class ZoneUtil {

	/**
	 * Mapping zone centroids to their nearest network node and
	 * creating an array containing the follwoing information:
	 * zone id, zone coordinate (centroid) and nearest node 
	 * 
	 * @param network
	 */
	public static ZoneObject[] mapZoneCentroid2NearestNode(final ActivityFacilitiesImpl zones, final NetworkImpl network) {
		
		assert( network != null );
		assert( zones != null );
		int numberOfZones = zones.getFacilities().values().size();
		ZoneObject zoneArray[] = new ZoneObject[numberOfZones];
		Iterator<? extends ActivityFacility> zonesIterator = zones.getFacilities().values().iterator();

		int counter = 0;
		while( zonesIterator.hasNext() ){

			ActivityFacility zone = zonesIterator.next();
			assert (zone != null );
			assert( zone.getCoord() != null );
			Coord zoneCoordinate = zone.getCoord();
			Node networkNode = network.getNearestNode( zoneCoordinate );
			assert( networkNode != null );
				
			zoneArray[counter] = new ZoneObject(zone.getId(), zoneCoordinate, networkNode);
			counter++;
		}
		return zoneArray;
	}
	
}

