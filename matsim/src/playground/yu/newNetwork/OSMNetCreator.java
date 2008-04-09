/* *********************************************************************** *
 * project: org.matsim.*
 * OSMNetCreator.java
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

package playground.yu.newNetwork;

import java.util.Set;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;

import playground.yu.analysis.NetworkLinkIdsInCircle;
import playground.yu.utils.OSMPatchPaser;

public class OSMNetCreator {
	private static double capperiod;

	private void resetCapacity(Link link) {
		if (link.getType().equals("80")) {
			if (link.getCapacity() / capperiod < 2000.0) {
				System.out.print("link " + link.getId().toString()
						+ " capacity from " + link.getCapacity() / capperiod
						+ " to ");
				link.setCapacity(2000.0 * capperiod);
				System.out.println(link.getCapacity() / capperiod);
			}
		} else if (link.getType().equals("81")) {
			if (link.getCapacity() / capperiod > 600.0) {
				System.out.print("link " + link.getId().toString()
						+ " capacity from " + link.getCapacity() / capperiod
						+ " to ");
				link.setCapacity(600.0 * capperiod);
				System.out.println(link.getCapacity() / capperiod);
			}
		}
	}

	public OSMNetCreator(NetworkLayer network) {
		capperiod = network.getCapacityPeriod() / 3600.0;// ss-->hh
	}

	public static void main(String[] args) {
		final String netFilename = "../schweiz-ivtch/network/ivtch.xml";
		final String OSMPatchFilename = "test/yu/utils/osmpatch.xml";
		// final String outputNetFilename =
		// "../schweiz-ivtch/network/ivtch-osm.xml";
		final String outputNetFilename = "test/yu/utils/ivtch-osm.1.3.xml";
		Config config = Gbl.createConfig(null);
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		// (1) -----------links in Circle
		Set<String> linkIdsInCircle = new NetworkLinkIdsInCircle(network)
				.getLinks(682845.0, 247388.0, 4000.0);
		// (2) -----------type=80->Cap=2000, type=81->cap=600
		OSMNetCreator osmNC = new OSMNetCreator(network);

		for (String linkId : linkIdsInCircle) {
			Link l = network.getLink(linkId);
			if (l != null) {
				osmNC.resetCapacity(l);
			}
		}
		// (3) -----------patch primary road (red links) in
		// OpenStreetMap.org-----
		OSMPatchPaser osmP = new OSMPatchPaser();
		osmP.readFile(OSMPatchFilename);
		int up = 0, upgraded = 0;
		for (String linkId : osmP.getUpgradeLinks()) {
			up++;
			if (linkIdsInCircle.contains(linkId)) {
				upgraded++;
				Link l = network.getLink(linkId);
				if (l != null) {
					if (l.getCapacity() / capperiod < 2000.0) {
						System.out.print("link " + l.getId().toString()
								+ " capacity from " + l.getCapacity()
								/ capperiod + " to ");
						l.setCapacity(2000.0 * capperiod);
						System.out.println(l.getCapacity() / capperiod);
					}
				}
			}
		}
		System.out.println(up + " links should be upgraded.");
		System.out.println(upgraded + " links were upgraded.");
		int down = 0, degraded = 0;
		for (String linkId : osmP.getDegradeLinks()) {
			down++;
			if (linkIdsInCircle.contains(linkId)) {
				degraded++;
				Link l = network.getLink(linkId);
				if (l != null) {
					if (l.getCapacity() / capperiod > 600.0) {
						System.out.print("link " + l.getId().toString()
								+ " capacity from " + l.getCapacity()
								/ capperiod + " to ");
						l.setCapacity(600.0 * capperiod);
						System.out.println(l.getCapacity() / capperiod);
					}
				}
			}
		}
		System.out.println(down + " links should be degraded.");
		System.out.println(degraded + " links were degraded.");
		new NetworkWriter(network, outputNetFilename).write();
	}
}
