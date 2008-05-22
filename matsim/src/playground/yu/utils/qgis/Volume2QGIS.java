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
package playground.yu.utils.qgis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author yu
 * 
 */
public class Volume2QGIS extends MATSimNet2QGIS implements X2QGIS {

	public static List<Map<Id, Integer>> createVolumes(NetworkLayer net,
			VolumesAnalyzer va) {
		List<Map<Id, Integer>> volumes = new ArrayList<Map<Id, Integer>>(24);
		for (int i = 0; i < 24; i++) {
			volumes.add(i, null);
		}
		for (Link link : (net.getLinks()).values()) {
			Id linkId = link.getId();
			int[] v = va.getVolumesForLink(linkId.toString());
			for (int i = 0; i < 24; i++) {
				Map<Id, Integer> m = volumes.get(i);
				// if (m != null) {
				// m.put(linkId, (int)(((double)((v != null) ? v[i] :
				// 0))/flowCapFactor));
				// } else if (m == null) {
				// m = new HashMap<Id, Integer>();
				// m.put(linkId, ((v != null) ? v[i] : 0) * 10);
				// volumes.add(i, m);
				// }
				if (m == null) {
					m = new HashMap<Id, Integer>();
					volumes.add(i, m);
				}
				m
						.put(
								linkId,
								(int) (((double) ((v != null) ? v[i] : 0)) / flowCapFactor));
			}
		}
		return volumes;
	}

	public void setCrs(String wkt, NetworkLayer network,
			CoordinateReferenceSystem crs) {
		super.setCrs(wkt);
		setN2g(new Volume2PolygonGraph(network, crs));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS();
		// String netFilename = "../schweiz-ivtch/network/ivtch-osm.xml";
		String netFilename = "test/yu/test/equil_net.xml";
		/*
		 * ///////////////////////////////////////////////////////////////
		 * Traffic Volumes and MATSim-network to Shp-file // *
		 * ///////////////////////////////////////////////////////////////
		 */
		// mn2q
		// .readNetwork("../schweiz-ivtch/network/ivtch-osm-wu-flama-noUetli.xml");
		mn2q.readNetwork(netFilename);
		mn2q.setCrs(ch1903);
		NetworkLayer net = mn2q.getNetwork();
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600 - 1, net);
		// MATSimNet2QGIS.setFlowCapFactor(0.1);
		mn2q.readEvents("test/yu/test/events.txt", va);
		List<Map<Id, Integer>> vols = createVolumes(net, va);
		for (int i = 0; i < 24; i++) {
			mn2q.addParameter("vol" + i + "-" + (i + 1) + "h", Integer.class,
					vols.get(i));
		}
		mn2q.writeShapeFile("test/yu/test/gunnar-volumes.shp");
		/*
		 * //////////////////////////////////////////////////////////////
		 * Differenz of Traffic Volumes and MATSim-network to Shp-file
		 * /////////////////////////////////////////////////////////////
		 */

		// mn2q
		// .readNetwork("../schweiz-ivtch/network/ivtch-osm-wu-flama.xml");
		// mn2q.setCrs(ch1903);
		// NetworkLayer net = mn2q.getNetwork();
		// VolumesAnalyzer vaA = new VolumesAnalyzer(3600, 24 * 3600 - 1, net);
		// mn2q.readEvents("../runs/run468/500.events.txt.gz", vaA);
		// List<Map<Id, Integer>> volsA = createVolumes(net, vaA);
		// VolumesAnalyzer vaB = new VolumesAnalyzer(3600, 24 * 3600 - 1, net);
		// mn2q.readEvents("../runs/run467/500.events.txt.gz", vaB);
		// List<Map<Id, Integer>> volsB = createVolumes(net, vaB);
		// for (int i = 0; i < 24; i++) {
		// Map<Id, Integer> diff = new TreeMap<Id, Integer>();
		// for (Id linkId : volsB.get(i).keySet()) {
		// diff.put(linkId, volsA.get(i).get(linkId).intValue()
		// - volsB.get(i).get(linkId).intValue());
		// }
		// mn2q.addParameter("vol" + i + "-" + (i + 1) + "h", Integer.class,
		// diff);
		// }
		// mn2q.writeShapeFile("test/yu/ivtch/468.500-467.500.shp");
		/*
		 * ////////////////////////////////////////////////////////////////////////////////
		 * Shp-file with 25 Layers //////////////////////////
		 */
		// mn2q.readNetwork(netFilename);
		// mn2q.setCrs(ch1903);
		// mn2q.writeShapeFile("test/yu/test/1000.net.shp");
		// VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600 - 1,
		// mn2q.network);
		// mn2q.readEvents("test/yu/test/1000.events.txt.gz", va);
		// List<Map<Id, Integer>> vols = createVolumes(mn2q.network, va);
		// for (int i = 0; i < 24; i++) {
		// Volume2QGIS v2q = new Volume2QGIS();
		// v2q.setCrs(ch1903, mn2q.network, mn2q.crs);
		// String index = "vol" + i + "-" + (i + 1) + "h";
		// v2q.addParameter(index, Integer.class, vols.get(i));
		// v2q.writeShapeFile("test/yu/test/1000." + index + ".shp");
		// }
	}
}
