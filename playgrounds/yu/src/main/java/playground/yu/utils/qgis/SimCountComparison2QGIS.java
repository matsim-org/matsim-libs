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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author yu
 * 
 */
public class SimCountComparison2QGIS extends MATSimNet2QGIS {
	private static class SimCountComparison2Polygon extends Volume2PolygonGraph {
		private static double linkWidthScaleFactor = 1d;

		public SimCountComparison2Polygon(Network network,
				CoordinateReferenceSystem crs, Set<Id> linkIds) {
			super(network, crs, linkIds);
		}

		@Override
		protected double getLinkWidth(Link link) {
			Integer value = (Integer) parameters.get(0).get(link.getId());
			if (value == null) {
				return 0d;
			}
			return value.doubleValue() * linkWidthScaleFactor;
		}

		public static void setLinkWidthScaleFactor(double linkWidthScaleFactor) {
			SimCountComparison2Polygon.linkWidthScaleFactor = linkWidthScaleFactor;
		}
	}

	public SimCountComparison2QGIS(String netFilename, String coordRefSys) {
		super(netFilename, coordRefSys);
	}

	/**
	 * for Berlin scenario
	 * 
	 * @param linkid
	 * @param network
	 * @return
	 */
	private static boolean isInRange(final Id linkid, final NetworkImpl net) {
		// Link l = network.getLinks().get(linkid);
		// if (l == null) {
		// System.out.println("Cannot find requested link: "
		// + linkid.toString());
		// return false;
		// }
		// return ((LinkImpl) l).calcDistance(network.getNodes().get(
		// new IdImpl("2531")).getCoord()) < 30000;
		return net.getLinks().containsKey(linkid);
	}

	public static List<Map<Id, Integer>> createVolumes(Collection<Id> linkIds,
			final VolumesAnalyzer va) {
		List<Map<Id, Integer>> volumes = new ArrayList<Map<Id, Integer>>(24);
		for (int i = 0; i < 24; i++) {
			volumes.add(i, null);
		}
		for (Id linkId : linkIds) {
			int[] v = va.getVolumesForLink(linkId);
			for (int i = 0; i < 24; i++) {
				Map<Id, Integer> m = volumes.get(i);
				if (m == null) {
					m = new HashMap<Id, Integer>();
					volumes.add(i, m);
				}
				m
						.put(
								linkId,
								(int) ((v != null ? (double) v[i] : 0d) / flowCapFactor));
			}
		}
		return volumes;
	}

	public void setLinkIds(Set<Id> linkIds) {
		setN2g(new SimCountComparison2Polygon(getNetwork(), crs, linkIds));
	}

	public static void main(final String[] args) {
		String netFilename, eventsFilename, countsFilename, shapeFilepath;
		double flowCapFactor, linkWidthScaleFactor;

		if (args.length == 6) {
			netFilename = args[0];
			eventsFilename = args[1];
			countsFilename = args[2];
			shapeFilepath = args[3];
			flowCapFactor = Double.parseDouble(args[4]);
			linkWidthScaleFactor = Double.parseDouble(args[5]);
		} else {
			netFilename = "../matsimTests/ParamCalibration/network.xml";
			eventsFilename = "../matsimTests/ParamCalibration/generalExpOutput/ITERS/it.400/400.events.txt.gz";
			countsFilename = "../matsimTests/ParamCalibration/counts200.xml";
			shapeFilepath = "../matsimTests/diverseRoutes/simCountCompare/";
			flowCapFactor = 0.1;
			linkWidthScaleFactor = 10d;
		}
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS(netFilename, gk4);
		// ////////////////////////////////////////
		MATSimNet2QGIS.setFlowCapFactor(flowCapFactor);

		Network network = mn2q.getNetwork();

		System.out.println("  reading the counts...");
		final Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		VolumesAnalyzer volumeAnalyzer = new VolumesAnalyzer(3600,
				24 * 3600 - 1, network);

		System.out.println("  reading the events...");
		mn2q.readEvents(eventsFilename, new EventHandler[] { volumeAnalyzer });

		Set<Id> linkIds = new HashSet<Id>();
		for (Id linkId : counts.getCounts().keySet()) {
			if (isInRange(linkId, (NetworkImpl) network)) {
				linkIds.add(linkId);
			}
		}

		List<Map<Id, Integer>> volumes = createVolumes(linkIds, volumeAnalyzer);

		SimCountComparison2Polygon
				.setLinkWidthScaleFactor(linkWidthScaleFactor);

		for (int i = 0; i < 24; i++) {
			SimCountComparison2QGIS scc2q = new SimCountComparison2QGIS(
					netFilename, gk4);
			scc2q.setLinkIds(linkIds);

			String index = i + "-" + (i + 1) + "h";
			Map<Id, Integer> diff = new TreeMap<Id, Integer>();
			Map<Id, Integer> sign = new TreeMap<Id, Integer>();

			// v2q.setCrs(ch1903, network, mn2q.crs, diff.keySet());

			for (Id linkId : volumes.get(i).keySet()) {
				Volume countVolume = counts.getCount(linkId).getVolume(i + 1);
				if (countVolume != null) {
					int volDiff = (int) (volumes.get(i).get(linkId).intValue()/* simValue */
					- countVolume.getValue()/* count */);
					if (volDiff < 0) {
						diff.put(linkId, -volDiff);
						sign.put(linkId, -1);
					} else if (volDiff > 0) {
						diff.put(linkId, volDiff);
						sign.put(linkId, 1);
					}
				}
			}
			scc2q.addParameter("vol" + index, Integer.class, diff);
			scc2q.addParameter("sign" + index, Integer.class, sign);
			if (diff.size() > 0 && sign.size() > 0) {
				scc2q.writeShapeFile(shapeFilepath + index + ".shp");
			}
		}
	}
}
