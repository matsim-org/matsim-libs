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

package playground.johannes.gsv.misc;

import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.sna.gis.io.ZoneLayerSHP;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author johannes
 * 
 */
public class InhabitantsStratifier {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		ZoneLayer<Map<String, Object>> zoneLayer = ZoneLayerSHP.read("/home/johannes/gsv/gis/vg250-ew_3112.gk3.shape.ebenen/vg250-ew_ebenen/vg250_gem.shp");

		SortedMap<Double, Zone<Map<String, Object>>> inhabs = new TreeMap<>();
		double total = 0;
		for (Zone<Map<String, Object>> zone : zoneLayer.getZones()) {
			Double ewz = (Double) zone.getAttribute().get("EWZ");
			if (ewz != null) {
				inhabs.put(ewz, zone);
				total += ewz;
			}
		}

		System.out.println("Total: " + total);
		
		double threshold = 0.0;
		int sum = 0;

		for (Zone<Map<String, Object>> zone : inhabs.values()) {
			sum += (Double) zone.getAttribute().get("EWZ");
			double p = sum / total;
			if (p > threshold) {
				threshold += 0.2;
			}
			zone.getAttribute().put("percentile", threshold);
		}

		zoneLayer.overwriteCRS(CRSUtils.getCRS(31467));
		ZoneLayerSHP.writeWithAttributes(zoneLayer, "/home/johannes/Schreibtisch/ger-perc.shp");
	}

}
