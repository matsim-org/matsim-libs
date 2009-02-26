/* *********************************************************************** *
 * project: org.matsim.*
 * Saturation2QGIS.java
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
package playground.yu.utils.qgis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.NetworkLayer;

/**
 * @author yu
 * 
 */
public class SaturationLevel2QGIS implements X2QGIS {

	public static List<Map<Id, Double>> createSaturationLevels(
			NetworkLayer net, VolumesAnalyzer va) {
		List<Map<Id, Double>> saturationLevels = new ArrayList<Map<Id, Double>>(
				24);
		for (int i = 0; i < 24; i++) {
			saturationLevels.add(i, null);
		}
		for (Link link : (net.getLinks()).values()) {
			Id linkId = link.getId();
			int[] v = va.getVolumesForLink(linkId.toString());
			for (int i = 0; i < 24; i++) {
				Map<Id, Double> m = saturationLevels.get(i);
				if (m != null) {
					m
							.put(
									linkId,
									(double) ((v != null) ? v[i] : 0)
											* 10.0
											/ link
													.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME)
											* (double) net.getCapacityPeriod()
											/ 3600.0);
				} else if (m == null) {
					m = new HashMap<Id, Double>();
					m
							.put(
									linkId,
									(double) ((v != null) ? v[i] : 0)
											* 10.0
											/ link
													.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME)
											* (double) net.getCapacityPeriod()
											/ 3600.0);
					saturationLevels.add(i, m);
				}
			}
		}
		return saturationLevels;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS();
		/*
		 * //////////////////////////////////////////////////////////////////////////////////
		 * Traffic saturation level and MATSim-network to Shp-file
		 * /////////////////////////////////////////////////////////////////////////////////
		 */
		mn2q
				.readNetwork("../schweiz-ivtch/network/ivtch-osm-wu-flama-noUetli.xml");
		mn2q.setCrs(ch1903);
		NetworkLayer net = mn2q.getNetwork();
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600 - 1, net);
		mn2q.readEvents("../runs/run468/500.events.txt.gz", va);
		List<Map<Id, Double>> sls = createSaturationLevels(net, va);
		for (int i = 0; i < 24; i++) {
			mn2q.addParameter("sl" + i + "-" + (i + 1) + "h", Double.class, sls
					.get(i));
		}
		mn2q.writeShapeFile("../runs/run468/468.500.saturationLevel.shp");
	}

}
