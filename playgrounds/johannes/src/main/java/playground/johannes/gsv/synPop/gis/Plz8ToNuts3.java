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

package playground.johannes.gsv.synPop.gis;

import com.vividsolutions.jts.geom.Point;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.sna.gis.io.ZoneLayerSHP;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class Plz8ToNuts3 {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		ZoneLayer<Map<String, Object>> plz8 = ZoneLayerSHP.read("/home/johannes/gsv/synpop/data/gis/nuts/pop.plz8.shp");
		ZoneLayer<Map<String, Object>> nuts3 = ZoneLayerSHP.read("/home/johannes/gsv/synpop/data/gis/nuts/de.nuts3.shp");
		
		Set<Zone<Double>> zones = new HashSet<Zone<Double>>();
		for(Zone<Map<String, Object>> nuts3Zone : nuts3.getZones()) {
			zones.add(new Zone<Double>(nuts3Zone.getGeometry()));
		}
		
		ZoneLayer<Double> zoneLayer = new ZoneLayer<Double>(zones);
		
		for(Zone<Map<String, Object>> plz8Zone : plz8.getZones()) {
			Point p = plz8Zone.getGeometry().getCentroid();
			Zone<Double> nuts3Zone = zoneLayer.getZone(p);
			
			if(nuts3Zone != null) {
			Long inhabsPlz8 = (Long) plz8Zone.getAttribute().get("A_GESAMT");
			Double inhabsNuts3 = nuts3Zone.getAttribute(); 
			if(inhabsNuts3 == null) {
				inhabsNuts3 = new Double(0);
			}
			
			inhabsNuts3 += inhabsPlz8;
			nuts3Zone.setAttribute(inhabsNuts3);
			} else {
				System.err.println("No nuts3 zone found.");
			}
		}
		
		ZoneLayerSHP.write(zoneLayer, "/home/johannes/gsv/synpop/data/gis/nuts/pop.nuts3.shp");

	}

}
