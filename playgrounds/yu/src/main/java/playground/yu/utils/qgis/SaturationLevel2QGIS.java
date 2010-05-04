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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * @author yu
 *
 */
public class SaturationLevel2QGIS extends MATSimNet2QGIS {

	public SaturationLevel2QGIS(String netFilename, String coordRefSys) {
		super(netFilename, coordRefSys);
	}

	public static List<Map<Id, Double>> createSaturationLevels(
			NetworkLayer net, VolumesAnalyzer va) {
		List<Map<Id, Double>> saturationLevels = new ArrayList<Map<Id, Double>>(
				24);
		for (int i = 0; i < 24; i++) {
			saturationLevels.add(i, null);
		}
		double capPeriod = net.getCapacityPeriod() / 3600.0;
		for (Link link : (net.getLinks()).values()) {
			Id linkId = link.getId();
			int[] v = va.getVolumesForLink(linkId);
			for (int i = 0; i < 24; i++) {
				Map<Id, Double> m = saturationLevels.get(i);
				if (m == null)
					m = new HashMap<Id, Double>();
				m.put(linkId, Double.valueOf(((v != null) ? v[i] : 0) * 10.0
						/ link.getCapacity() * capPeriod));
				saturationLevels.set(i, m);
			}
		}
		return saturationLevels;
	}

	public static List<Map<Id, Double>> createSaturationLevels(
			NetworkLayer net, RoadPricingScheme rps, VolumesAnalyzer va) {
		List<Map<Id, Double>> saturationLevels = new ArrayList<Map<Id, Double>>(
				24);
		for (int i = 0; i < 24; i++) {
			saturationLevels.add(i, null);
		}
		double capPeriod = net.getCapacityPeriod() / 3600.0;
		for (Id linkId : rps.getLinkIdSet()) {
			int[] v = va.getVolumesForLink(linkId);
			for (int i = 0; i < 24; i++) {
				Map<Id, Double> m = saturationLevels.get(i);
				if (m == null)
					m = new HashMap<Id, Double>();
				m.put(linkId, Double.valueOf(((v != null) ? v[i] : 0)
						/ flowCapFactor / net.getLinks().get(linkId).getCapacity() * capPeriod));
				saturationLevels.set(i, m);
			}
		}
		return saturationLevels;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS(
				"../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml", ch1903);
		/*
		 * //////////////////////////////////////////////////////////////////////
		 * /Traffic saturation level and MATSim-network to Shp-file
		 * /////////////////////////////////////////////////////////////////////
		 */
		NetworkLayer net = mn2q.getNetwork();
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600 - 1, net);
		mn2q.readEvents("../matsimTests/Calibration/680.events.txt.gz",
				new EventHandler[] { va });
		List<Map<Id, Double>> sls = createSaturationLevels(net, va);
		for (int i = 0; i < 24; i++) {
			mn2q.addParameter("sl" + i + "-" + (i + 1) + "h", Double.class, sls
					.get(i));
		}
		mn2q
				.writeShapeFile("../matsimTests/Calibration/rop.1-24.680.saturationLevel.shp");
	}

}
