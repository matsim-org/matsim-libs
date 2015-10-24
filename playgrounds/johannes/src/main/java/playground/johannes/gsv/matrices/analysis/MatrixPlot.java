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

package playground.johannes.gsv.matrices.analysis;

import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.sna.gis.io.ZoneLayerSHP;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;

import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 * 
 */
public class MatrixPlot {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Matrix m = new Matrix("1", null);
		VisumMatrixReader reader = new VisumMatrixReader(m);
		reader.readFile("/home/johannes/gsv/matrices/miv.319.fma");

		ZoneLayer<Map<String, Object>> zonelayer = ZoneLayerSHP.read("/home/johannes/gsv/matrices/zones_zone.SHP");
		zonelayer.overwriteCRS(CRSUtils.getCRS(4326));
		Set<Zone<Map<String, Object>>> zones = new HashSet<>();
		
		double minS = Double.MAX_VALUE;
		double maxS = Double.MIN_VALUE;
		
		double minT = Double.MAX_VALUE;
		double maxT = Double.MIN_VALUE;
		
		for (Zone<Map<String, Object>> zone : zonelayer.getZones()) {
			String code = zone.getAttribute().get("ISO_CODE").toString();
			if (code.equalsIgnoreCase("DE")) {
				String id = zone.getAttribute().get("NO").toString();

				List<Entry> entries = m.getFromLocEntries(id);
				double sum = 0;
				if (entries != null) {
					for (Entry e : entries) {
//						if (!e.getFromLocation().equalsIgnoreCase(e.getToLocation())) {
							sum += e.getValue();
//						}
					}
				}
				minS = Math.min(minS, sum);
				maxS = Math.max(maxS, sum);
				
				Zone<Map<String, Object>> newZone = new Zone<>(zone.getGeometry());
				zones.add(newZone);
				newZone.setAttribute(new HashMap<String, Object>());
				newZone.getAttribute().put("SOURCE_VOL", sum);

				entries = m.getToLocEntries(id);
				sum = 0;
				if (entries != null) {
					for (Entry e : entries) {
//						if (!e.getFromLocation().equalsIgnoreCase(e.getToLocation())) {
							sum += e.getValue();
//						}
					}
				}
				minT = Math.min(minT, sum);
				maxT = Math.max(maxT, sum);
				
				newZone.getAttribute().put("TARGET_VOL", sum);
			}
		}
		for(Zone<Map<String, Object>> zone : zones) {
			double val = (Double) zone.getAttribute().get("SOURCE_VOL");
			zone.getAttribute().put("SOURCE_VOL", val/maxS);
			
			val = (Double) zone.getAttribute().get("TARGET_VOL");
			zone.getAttribute().put("TARGET_VOL", val/maxT);
		}
		
		ZoneLayer<Map<String, Object>> newLayer = new ZoneLayer<>(zones);
		newLayer.overwriteCRS(CRSUtils.getCRS(4326));
		ZoneLayerSHP.writeWithAttributes(newLayer, "/home/johannes/gsv/matrices/marginals.319.shp");

	}

}
