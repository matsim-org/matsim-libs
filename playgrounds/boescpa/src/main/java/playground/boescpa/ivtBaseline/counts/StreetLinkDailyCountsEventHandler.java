/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.ivtBaseline.counts;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static playground.boescpa.ivtBaseline.counts.CountsIVTBaseline.COUNTS_DELIMITER;

/**
 * Counts the daily vehicles on a given link.
 *
 * @author boescpa
 */
public class StreetLinkDailyCountsEventHandler implements LinkEnterEventHandler{

	private final Config config;

	private final Map<String, Tuple<String, Double>> linksToMonitor = new HashMap<>();
	private final Set<String> linksToMonitorCache = new HashSet<>();

	private final HashMap<String, Integer> linkCounts = new HashMap<>();

	@Inject
	private StreetLinkDailyCountsEventHandler(@Named("pathToStreetLinksDailyToMonitor") final String pathToLinksList, Config config) {
		setLinksToMonitor(pathToLinksList);
		this.config = config;
	}

	private void setLinksToMonitor(final String pathToLinksList) {
		this.linksToMonitor.clear();
		BufferedReader linkReader = IOUtils.getBufferedReader(pathToLinksList);
		try {
			linkReader.readLine(); // read header: linkId; countStationDescr; countVolume
			String line = linkReader.readLine();
			while (line != null) {
				String[] lineElements = line.split(COUNTS_DELIMITER);
				String linkToMonitor = lineElements[0].trim();
				String countStationDescr = lineElements[1].trim();
				double countVolume = Double.parseDouble(lineElements[2]);
				this.linksToMonitor.put(linkToMonitor, new Tuple<>(countStationDescr, countVolume));
				this.linksToMonitorCache.add(linkToMonitor);
				line = linkReader.readLine();
			}
			linkReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reset(int iteration) {
		linkCounts.clear();
		for (String linkId : linksToMonitorCache) {
			linkCounts.put(linkId, 0);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		String linkId = event.getLinkId().toString();
		if (linksToMonitorCache.contains(linkId)) {
			int count = linkCounts.get(linkId);
			count++;
			linkCounts.put(linkId, count);
		}
	}

	public void write(String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			// write file head
			writer.write("linkId"+ COUNTS_DELIMITER + "countStationDescr" + COUNTS_DELIMITER + "countVolume" + COUNTS_DELIMITER + "matsimVolume" + COUNTS_DELIMITER + "relativeVolume");
			writer.newLine();
			// write content
			for (String linkId : linksToMonitorCache) {
				String countStationDescr = linksToMonitor.get(linkId).getFirst();
				double countVolume = linksToMonitor.get(linkId).getSecond();
				double matsimVolume = linkCounts.get(linkId)*config.counts().getCountsScaleFactor();
				double relVolume = countVolume > 0 ? matsimVolume/countVolume : matsimVolume*100;
				writer.write(linkId + COUNTS_DELIMITER + countStationDescr + COUNTS_DELIMITER +
						Long.toString((long)countVolume) + COUNTS_DELIMITER +
						Long.toString((long)matsimVolume) + COUNTS_DELIMITER +
						Double.toString(relVolume));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
