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
package playground.yu.counts;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;

/**
 * makes new countsfile for new networks with the same countsdata, a .txt-file
 * containing the correlation information between old linkIds, where the
 * countsstations lie, und new linkIds must be given.
 * 
 * @author ychen
 * 
 */
public class CountsWithUpdatedNetwork {

	private static class CorrelationTableReaer implements TabularFileHandler {
		private Set<Tuple<Id, Id>> correlations = new HashSet<Tuple<Id, Id>>();

		public Set<Tuple<Id, Id>> getCorrelations() {
			return correlations;
		}

		public void startRow(String[] row) {
			if (row.length > 3)
				if (row[0].length() > 1 && row[3].length() > 1) {
					correlations.add(new Tuple<Id, Id>(new IdImpl(row[0]),
							new IdImpl(row[3])));
				}
		}

	}

	public static void main(String[] args) {
		String correlationFilename = "../berlin/counts/zuordnungslist.txt";
		String oldCountsFilename = "../berlin data/link_counts_PKW_hrs0-24.xml";
		String newCountsFilename = "../berlin data/counts4bb_cl.xml";

		TabularFileParserConfig tfpc = new TabularFileParserConfig();
		tfpc.setCommentTags(new String[] { "oldID" });
		tfpc.setDelimiterRegex("\t");
		tfpc.setFileName(correlationFilename);

		CorrelationTableReaer ctr = new CorrelationTableReaer();
		try {
			new TabularFileParser().parse(tfpc, ctr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Set<Tuple<Id, Id>> correlations = ctr.getCorrelations();

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(oldCountsFilename);

		Map<Id, Count> tmpCountsMap = new HashMap<Id, Count>();
		tmpCountsMap.putAll(counts.getCounts());
		counts.getCounts().clear();
		for (Tuple<Id, Id> correl : correlations) {
			Id oldCountLinkId = correl.getFirst();
			Count oldCount = tmpCountsMap.remove(oldCountLinkId);
			Count newCount = counts.createCount(correl.getSecond(),
					oldCountLinkId.toString());
			newCount.setCoord(oldCount.getCoord());
			for (Volume v : oldCount.getVolumes().values())
				newCount.createVolume(v.getHour(), v.getValue());
		}

		counts
				.setDescription(counts.getDescription()
						+ "This countsfile was also fitted to the new openStreetMap-network");
		new CountsWriter(counts).writeFile(newCountsFilename);
	}
	/*
	 * //old counts --> old links --> old coords --> nearest links --> new links
	 * --> new counts public static void main(String[] args) { String
	 * newNetFilename = "../berlin/network/bb_cl.xml.gz"; String
	 * oldCountsFilename = "../berlin data/link_counts_PKW_hrs0-24.xml"; String
	 * newCountsFilename = "../berlin data/counts4bb_cl.xml";
	 * 
	 * NetworkLayer newNetwork = new NetworkLayer(); new
	 * MatsimNetworkReader(newNetwork).readFile(newNetFilename);
	 * 
	 * Counts counts = new Counts(); new
	 * MatsimCountsReader(counts).readFile(oldCountsFilename);
	 * 
	 * Map<Id, Count> countsMap = counts.getCounts(); Map<Id, Count>
	 * countsMapCopy = new HashMap<Id, Count>(countsMap); for (Count oldCount :
	 * countsMapCopy.values()) { Id linkId = oldCount.getLocId(); Link link =
	 * newNetwork.getLink(linkId); if (link == null) {// linkId doesn't exist in
	 * the new network --> to // look for new linkId manually or with XY2links
	 * System.err.println("----->link " + linkId +
	 * " doesn't exist in the new network.");
	 * 
	 * Link newLink = newNetwork.getNearestLink(oldCount.getCoord()); Count
	 * newCount = counts.createCount(newLink.getId(), null);
	 * newCount.setCoord(oldCount.getCoord()); for (Volume oldV :
	 * oldCount.getVolumes().values()) newCount.createVolume(oldV.getHour(),
	 * oldV.getValue());
	 * 
	 * countsMap.remove(linkId); } else if
	 * (CoordUtils.calcDistance(oldCount.getCoord(), link .getCoord()) < 200.0)
	 * {// linkId exists in the new network, // the new // coordinate lies
	 * nearby
	 * 
	 * } else {// linkId exists in the new network, but the new coordinate //
	 * doesn't lie nearby System.err .println("----->link " + linkId +
	 * " exists in the new network, but the new coordinate doesn't lie nearby."
	 * ); countsMap.remove(linkId);
	 * 
	 * Link newLink = newNetwork.getNearestLink(oldCount.getCoord()); Id
	 * newLinkId = newLink.getId(); if (counts.getCount(newLinkId) != null) {
	 * countsMap.put(new IdImpl("n" + newLinkId.toString()),
	 * countsMap.remove(newLinkId)); } Count newCount =
	 * counts.createCount(newLinkId, null);
	 * newCount.setCoord(oldCount.getCoord()); for (Volume v :
	 * oldCount.getVolumes().values()) newCount.createVolume(v.getHour(),
	 * v.getValue()); } }
	 * 
	 * counts .setDescription(counts.getDescription() +
	 * "This countsfile was also fitted to the new openStreetMap-network"); new
	 * CountsWriter(counts, newCountsFilename).write(); }
	 */
}
