/* *********************************************************************** *
 * project: org.matsim.*
 * CountsIdUpdater.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.yu.utils.counts;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;

public class CountsIdUpdater {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		String oldNetFile = "../berlin data/newOSM/bb_cl_old.xml.gz";
		String newNetFile = "../berlin/network/bb_cl.xml.gz";
		String oldCountsFile = "../berlin/counts/counts4bb_cl_old.xml";
		String newCountsFile = "../berlin/counts/counts4bb_cl.xml";

		int cnt = 0;

		NetworkLayer oldNet = new NetworkLayer();
		new MatsimNetworkReader(oldNet).readFile(oldNetFile);

		Counts oldCounts = new Counts();
		new MatsimCountsReader(oldCounts).readFile(oldCountsFile);

		NetworkLayer newNet = new NetworkLayer();
		new MatsimNetworkReader(newNet).readFile(newNetFile);

		Counts newCounts = new Counts();
		newCounts.setYear(2000);
		newCounts.setName("berlin counts");
		newCounts.setLayer("0");
		newCounts
				.setDescription("extracted from vsp-cvs/studies/berlin-wip/external-data/counts/senstadt-hand/link_counts_PKW_hrs0-24.att, countIds also were changed according to the new OSM-network https://svn.vsp.tu-berlin.de/repos/shared-svn/studies/countries/de/berlin/network/bb_cl.xml.gz");
		for (Id oldCountId : oldCounts.getCounts().keySet()) {
			Link oldLink = oldNet.getLink(oldCountId);
			System.out.println("oldCountId :\t" + oldCountId);
			Id newLinkId = searchLinkPerNodeIdPair(oldLink.getFromNode()
					.getId(), oldLink.getToNode().getId(), newNet);
			if (newLinkId != null) {
				System.out.println(++cnt + "\toldCountId\t"
						+ oldCountId.toString() + "\tnewCountId\t"
						+ newLinkId.toString());
				Count oldCount = oldCounts.getCount(oldCountId);
				Count newCount = newCounts.createCount(newLinkId, oldCount
						.getCsId());
				newCount.setCoord(newNet.getLink(newLinkId).getCoord());
				for (Volume volume : oldCount.getVolumes().values())
					newCount.createVolume(volume.getHour(), volume.getValue());
			}
		}
		new CountsWriter(newCounts, newCountsFile).write();
	}

	private static Id searchLinkPerNodeIdPair(final Id fromNodeId,
			final Id toNodeId, final NetworkLayer net) {
		Node fromNode = net.getNode(fromNodeId);
		if (fromNode == null) {
			System.err.println("Node with Id " + fromNodeId.toString()
					+ " doesn't exist in the new network!");
			return null;
		}
		for (Link link : fromNode.getOutLinks().values())
			if (Integer.parseInt(link.getToNode().getId().toString()) == Integer
					.parseInt(toNodeId.toString())) {
				System.out.println("Link with fromNodeId "
						+ fromNodeId.toString() + " and toNodeId "
						+ toNodeId.toString() + " was found (linkId = "
						+ link.getId() + ") in the new network!");
				return link.getId();
			}
		System.err.println("Link with fromNodeId " + fromNodeId.toString()
				+ " and toNodeId " + toNodeId.toString()
				+ " doesn't exist in the new network!");
		return null;
	}
}
