/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.gis;

import com.vividsolutions.jts.geom.Geometry;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class JointZones {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.gk3.geojson")));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		data = null;
		zones.setPrimaryKey("gsvId");

		Map<String, Set<Zone>> aggZones = new HashMap<>();
		for(Zone zone : zones.getZones()) {
			String code = zone.getAttribute("nuts2_code");
			Set<Zone> set = aggZones.get(code);
			if(set == null) {
				set = new HashSet<>();
				aggZones.put(code, set);
			}
			set.add(zone);
		}
		
		Set<Zone> newZones = new HashSet<>();
		
		for(Entry<String, Set<Zone>> entry : aggZones.entrySet()) {
			Set<Zone> set = entry.getValue();
			Geometry refGeo = null;
			for(Zone zone : set) {
				if(refGeo != null) {
					
					Geometry geo2 = zone.getGeometry();
					
					refGeo  = refGeo.union(geo2);
				} else {
					refGeo = zone.getGeometry();
				}
			}
			
			Zone zone = new Zone(refGeo);
			zone.setAttribute("nuts2_code", entry.getKey());
			newZones.add(zone);
		}
		
		data = ZoneGeoJsonIO.toJson(newZones);
		Files.write(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts2.gk3.geojson"), data.getBytes(), StandardOpenOption.CREATE);
	}

}
