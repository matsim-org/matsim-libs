/* *********************************************************************** *
 * project: org.matsim.*
 * AvgSpeed2QGIS.java
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

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

import playground.yu.analysis.CalcLinkAvgSpeed;

/**
 * @author yu
 * 
 */
public class AvgSpeed2QGIS {
	public static String ch1903 = "PROJCS[\"CH1903_LV03\",GEOGCS[\"GCS_CH1903\",DATUM[\"D_CH1903\",SPHEROID[\"Bessel_1841\",6377397.155,299.1528128]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Hotine_Oblique_Mercator_Azimuth_Center\"],PARAMETER[\"False_Easting\",600000],PARAMETER[\"False_Northing\",200000],PARAMETER[\"Scale_Factor\",1],PARAMETER[\"Azimuth\",90],PARAMETER[\"Longitude_Of_Center\",7.439583333333333],PARAMETER[\"Latitude_Of_Center\",46.95240555555556],UNIT[\"Meter\",1],AUTHORITY[\"EPSG\",\"21781\"]]";

	private static List<Map<Id, Double>> createSpeeds(NetworkLayer net,
			CalcLinkAvgSpeed clas) {
		List<Map<Id, Double>> speeds = new ArrayList<Map<Id, Double>>(24);
		for (int i = 0; i < 24; i++) {
			speeds.add(i, null);
		}
		for (int i = 0; i < 24; i++) {
			Map<Id, Double> aSpeeds = speeds.get(i);
			for (Link link : (net.getLinks()).values()) {
				Id linkId = link.getId();
				if (aSpeeds != null) {
					aSpeeds.put(linkId, clas.getAvgSpeed(linkId,
							(double) i * 3600.0));
				} else if (aSpeeds == null) {
					aSpeeds = new HashMap<Id, Double>();
					aSpeeds.put(linkId, clas.getAvgSpeed(linkId,
							(double) i * 3600.0));
					speeds.add(i, aSpeeds);
				}
			}
		}
		return speeds;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS();
		/*
		 * ///////////////////////////////////////////////////////////////
		 * Traffic Volumes and MATSim-network to Shp-file // *
		 * ///////////////////////////////////////////////////////////////
		 */
		mn2q.readNetwork("../schweiz-ivtch/network/ivtch-osm-wu.xml");
		mn2q.setCrs(ch1903);
		NetworkLayer net = mn2q.getNetwork();
		CalcLinkAvgSpeed clas = new CalcLinkAvgSpeed(net);
		mn2q.readEvents("../runs/run466/500.events.txt.gz", clas);
		List<Map<Id, Double>> speeds = createSpeeds(net, clas);
		for (int i = 0; i < 24; i++) {
			mn2q.addParameter("aS" + i + "-" + (i + 1) + "h", Double.class,
					speeds.get(i));
		}
		mn2q.writeShapeFile("../runs/run466/466.500.avgSpeed.shp");
	}
}
