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
package playground.yu.utils.qgis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkLayer;

import playground.yu.analysis.CalcLinksAvgSpeed;

/**
 * @author yu
 *
 */
public class AvgSpeed2QGIS implements X2QGIS {

	public static List<Map<Id, Double>> createSpeeds(NetworkLayer net,
			CalcLinksAvgSpeed clas) {
		List<Map<Id, Double>> speeds = new ArrayList<Map<Id, Double>>(24);
		for (int i = 0; i < 24; i++) {
			speeds.add(i, null);
		}
		for (int i = 0; i < 24; i++) {
			Map<Id, Double> aSpeeds = speeds.get(i);
			if (aSpeeds != null)
				for (Link link : (net.getLinks()).values()) {
					Id linkId = link.getId();
					aSpeeds.put(linkId, clas.getAvgSpeed(linkId,
							i * 3600.0));
				}
			else
				for (Link link : (net.getLinks()).values()) {
					Id linkId = link.getId();
					aSpeeds = new HashMap<Id, Double>();
					aSpeeds.put(linkId, clas.getAvgSpeed(linkId,
							i * 3600.0));
					speeds.add(i, aSpeeds);
				}
		}
		return speeds;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS("test/yu/test/equil_net.xml",
				ch1903);
		/*
		 * ///////////////////////////////////////////////////////////////
		 * Traffic Volumes and MATSim-network to Shp-file // *
		 * ///////////////////////////////////////////////////////////////
		 */
		NetworkLayer net = mn2q.getNetwork();
		CalcLinksAvgSpeed clas = new CalcLinksAvgSpeed(net);
		mn2q.readEvents("test/yu/test/events.txt", new EventHandler[] { clas });
		List<Map<Id, Double>> speeds = createSpeeds(net, clas);
		for (int i = 0; i < 24; i++) {
			mn2q.addParameter("aS" + i + "-" + (i + 1) + "h", Double.class,
					speeds.get(i));
		}
		mn2q.writeShapeFile("test/yu/test/gunnar-avgSpeed.shp");
	}
}
