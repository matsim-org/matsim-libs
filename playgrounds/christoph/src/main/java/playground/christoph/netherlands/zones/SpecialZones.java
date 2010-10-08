/* *********************************************************************** *
 * project: org.matsim.*
 * SpecialZones.java
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

package playground.christoph.netherlands.zones;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.feature.Feature;

public class SpecialZones {

	/*package*/ static List<Integer> zonesToRemove;
	/*package*/ static List<Integer> invalidZones;
	
	/*
	 * Numbers are PostCodes
	 */
	static {
		/*
		 * Islands which are not connected to the main network
		 */
		zonesToRemove = new ArrayList<Integer>();
		zonesToRemove.add(1791);
		zonesToRemove.add(1792);
		zonesToRemove.add(1793);
		zonesToRemove.add(1794);
		zonesToRemove.add(1795);
		zonesToRemove.add(1796);
		zonesToRemove.add(1797);
		zonesToRemove.add(8881);
		zonesToRemove.add(8882);
		zonesToRemove.add(8883);
		zonesToRemove.add(8884);
		zonesToRemove.add(8885);
		zonesToRemove.add(8891);
		zonesToRemove.add(8892);
		zonesToRemove.add(8893);
		zonesToRemove.add(8894);
		zonesToRemove.add(8895);
		zonesToRemove.add(8896);
		zonesToRemove.add(8897);
		zonesToRemove.add(8899);
		zonesToRemove.add(9161);
		zonesToRemove.add(9162);
		zonesToRemove.add(9163);
		zonesToRemove.add(9164);
		zonesToRemove.add(9166);
		
		/*
		 * Zone which seems to have an invalid shape
		 */
		invalidZones = new ArrayList<Integer>();
		invalidZones.add(1601);
	}
	
	/*
	 * The shape file contains a zone which seems to cause problem when checking
	 * whether a coordinate is located inside the zone or not. The zone is a dam,
	 * therefore I assume that it should be no problem if there are no links /
	 * facilities assigned to that zone...
	 */
	/*package*/ static boolean skipZone(Feature zone) {
//		int id = Integer.valueOf(zone.getID().replace("postcode4.", ""));	// Object Id
//		int id = ((Long)zone.getAttribute(1)).intValue();	// Zone Id
		int id = ((Long)zone.getAttribute(3)).intValue();	// PostCode
		if (zonesToRemove.contains(id)) return true;
		if (invalidZones.contains(id)) return true;
		else return false;
	}

	/*
	 * Drop some zones that should not be respected by the model.
	 * Examples are the islands which are not connected to the road network. 
	 */
	/*package*/ static void dropZones(Map<Integer, Feature> zonesMap) {
		
		for (int zoneId : zonesToRemove) {
			zonesMap.remove(zoneId);
		}
		for (int zoneId : invalidZones) {
			zonesMap.remove(zoneId);
		}
	}

}
