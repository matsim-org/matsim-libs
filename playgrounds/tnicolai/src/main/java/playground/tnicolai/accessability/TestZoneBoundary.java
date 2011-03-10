/* *********************************************************************** *
 * project: org.matsim.*
 * TestZoneBoundary.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.tnicolai.accessability;

import java.io.IOException;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;

import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;

/**
 * @author thomas
 *
 */
public class TestZoneBoundary {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		ZoneLayer<Double> startZones = null;
		String psrcSHPFile = "/Users/thomas/Development/opus_home/data/seattle_parcel/shapefiles/zone.shp";
		
		try {
			startZones= ZoneLayerSHP.read(psrcSHPFile);	startZones.overwriteCRS(CRSUtils.getCRS(21781));
			
			int i = 0;
			for(Zone zone: startZones.getZones()){
				System.out.println(zone.getAttribute());
				i++;
			}
			System.out.println("number of zones = " + i);
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}

