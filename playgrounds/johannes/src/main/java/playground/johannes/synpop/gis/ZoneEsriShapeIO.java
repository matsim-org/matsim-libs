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

package playground.johannes.synpop.gis;

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class ZoneEsriShapeIO {

	public static ZoneCollection read(String filename, String primaryKey) {
		ZoneCollection zones = read(filename);
		zones.setPrimaryKey(primaryKey);
		return zones;
	}

	public static ZoneCollection read(String filename) {
		ZoneCollection zones = new ZoneCollection(null);
		Set<Zone> zoneSet = new HashSet<>();
		
		try {
			for(SimpleFeature feature : EsriShapeIO.readFeatures(filename)) {
				Zone zone = new Zone((Geometry) feature.getDefaultGeometry());
		
				for(Property prop : feature.getProperties()) {
					String name = prop.getName().getLocalPart();
					Object valueObj = prop.getValue();
					String value = null;
					if(valueObj != null) value = valueObj.toString();
					zone.setAttribute(name, value);
					
				}
				
				zoneSet.add(zone);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		zones.addAll(zoneSet);
		
		return zones;
	}
}
