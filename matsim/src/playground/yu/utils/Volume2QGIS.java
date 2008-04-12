/* *********************************************************************** *
 * project: org.matsim.*
 * Volume2QGIS.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

/**
 * @author yu
 * 
 */
public class Volume2QGIS {
	public static String ch1903 = "PROJCS[\"CH1903_LV03\",GEOGCS[\"GCS_CH1903\",DATUM[\"D_CH1903\",SPHEROID[\"Bessel_1841\",6377397.155,299.1528128]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Hotine_Oblique_Mercator_Azimuth_Center\"],PARAMETER[\"False_Easting\",600000],PARAMETER[\"False_Northing\",200000],PARAMETER[\"Scale_Factor\",1],PARAMETER[\"Azimuth\",90],PARAMETER[\"Longitude_Of_Center\",7.439583333333333],PARAMETER[\"Latitude_Of_Center\",46.95240555555556],UNIT[\"Meter\",1],AUTHORITY[\"EPSG\",\"21781\"]]";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MATSimNet2Shape mn2s = new MATSimNet2Shape();
		/*/////////////////////////////////////////////////////
		// write MATSim-network to Shp-file
		// /////////////////////////////////////////////////////
		// mn2s.readNetwork("test/yu/utils/ivtch-osm.1.2.xml");
		// mn2s.setCrs(ch1903);
		// mn2s.writeShapeFile("test/yu/utils/0.shp");
		// /////////////////////////////////////////////////////*/
		mn2s.readNetwork("../schweiz-ivtch/network/ivtch-osm.xml");
		mn2s.setCrs(ch1903);
		NetworkLayer net = mn2s.getNetwork();
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600 - 1, net);
		mn2s.readEvents("../runs/run445/100.events.txt.gz", va);

		List<Map<String, Integer>> vols = new ArrayList<Map<String, Integer>>(
				24);
		for (int i = 0; i < 24; i++) {
			vols.add(i, null);
		}
		for (Link ql : (net.getLinks()).values()) {
			String qlId = ql.getId().toString();
			int[] v = va.getVolumesForLink(qlId);
			for (int i = 0; i < 24; i++) {
				Map<String, Integer> m = vols.get(i);
				if (m != null) {
					m.put(qlId, ((v != null) ? v[i] : 0) * 10);
				} else if (m == null) {
					m = new HashMap<String, Integer>();
					m.put(qlId, ((v != null) ? v[i] : 0) * 10);
					vols.add(i, m);
				}
			}
		}
		for (int i = 0; i < 24; i++) {
			mn2s.addParameter("vol" + i + "-" + (i + 1) + "h", Integer.class,
					vols.get(i));
		}
		mn2s.writeShapeFile("test/yu/utils/445.shp");
	}

}
