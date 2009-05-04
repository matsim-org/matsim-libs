/* *********************************************************************** *
 * project: org.matsim.*
 * CountsWithUpdatedNetwork.java
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

/**
 * 
 */
package playground.yu.utils.counts;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;

/**
 * makes new countsfile for new networks with the same countsdata
 * 
 * @author ychen
 * 
 */
public class CountsWithUpdatedNetwork {
	public static void main(String[] args) {
		String newNetFilename = "../berlin/network/bb_cl.xml.gz";
		String oldCountsFilename = "../berlin data/link_counts_PKW_hrs0-24.xml";
		String newCountsFilename = "../berlin data/counts4bb_cl.xml";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(newNetFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(oldCountsFilename);

		Map<Id, Count> countsMap = counts.getCounts();
		Map<Id, Count> countsMapCopy = new HashMap<Id, Count>(countsMap);
		for (Count oldCount : countsMapCopy.values()) {
			Id linkId = oldCount.getLocId();
			Link link = network.getLink(linkId);
			if (link == null) {// linkId doesn't exist in the new network --> to
				// look for new linkId manually or with XY2links
				System.err.println("----->link " + linkId
						+ " doesn't exist in the new network.");

				Link newLink = network.getNearestLink(oldCount.getCoord());
				Count newCount = counts.createCount(newLink.getId(), null);
				newCount.setCoord(oldCount.getCoord());
				for (Volume oldV : oldCount.getVolumes().values())
					newCount.createVolume(oldV.getHour(), oldV.getValue());

				countsMap.remove(linkId);
			} else if (CoordUtils.calcDistance(oldCount.getCoord(), link
					.getCoord()) < 500) {// linkId exists in the new network,
				// the new
				// coordinate lies nearby

			} else {// linkId exists in the new network, but the new coordinate
				// doesn't lie nearby
				System.err
						.println("----->link "
								+ linkId
								+ " exists in the new network, but the new coordinate doesn't lie nearby.");
				countsMap.remove(linkId);

				Link newLink = network.getNearestLink(oldCount.getCoord());
				Id newLinkId = newLink.getId();
				if (counts.getCount(newLinkId) != null)
					countsMap.remove(newLinkId);
				Count newCount = counts.createCount(newLinkId, null);
				newCount.setCoord(oldCount.getCoord());
				for (Volume v : oldCount.getVolumes().values())
					newCount.createVolume(v.getHour(), v.getValue());
			}
		}

		counts
				.setDescription(counts.getDescription()
						+ "This countsfile was also fitted to the new openStreetMap-network");
		new CountsWriter(counts, newCountsFilename).write();
	}
}
