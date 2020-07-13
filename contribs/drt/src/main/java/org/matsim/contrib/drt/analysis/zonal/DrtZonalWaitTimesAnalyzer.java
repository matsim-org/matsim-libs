/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.contrib.drt.analysis.zonal;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.analysis.DrtRequestAnalyzer;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DrtZonalWaitTimesAnalyzer implements IterationEndsListener, DrtRequestSubmittedEventHandler {

	private final DrtConfigGroup drtCfg;
	private final DrtRequestAnalyzer requestAnalyzer;
	private final DrtZonalSystem zones;
	private Map<Id<Request>, DrtRequestSubmittedEvent> submittedRequests = new HashMap<>();

	public DrtZonalWaitTimesAnalyzer(DrtConfigGroup configGroup, EventsManager eventsManager, DrtRequestAnalyzer requestAnalyzer, DrtZonalSystem zones){
		eventsManager.addHandler(this);
		this.drtCfg = configGroup;
		this.requestAnalyzer = requestAnalyzer;
		this.zones = zones;
	}

	@Override
	public void reset(int iteration) {
		submittedRequests.clear();
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		if (!event.getMode().equals(drtCfg.getMode())) {
			return;
		}
		this.submittedRequests.put(event.getRequestId(), event);
	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String fileName = event.getServices().getControlerIO()
				.getIterationFilename(event.getIteration(), "zonalWaitTimeComparison" + "_" + drtCfg.getMode() + ".csv");
		write(fileName);
	}

	public void write(String fileName) {
		String delimiter = ";";
		Map<String, DescriptiveStatistics> zoneStats = createZonalStats(delimiter);
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		try {
			DecimalFormat format = new DecimalFormat();
			format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
			format.setMinimumIntegerDigits(1);
			format.setMaximumFractionDigits(2);
			format.setGroupingUsed(false);
			bw.append("zone;centerX;centerY;sumWaitTime;meanWaitTime;min;max;p95;p90;p80;p75");
			for (Map.Entry<String, DescriptiveStatistics> zoneStatsEntry : zoneStats.entrySet()) {
				DescriptiveStatistics stats = zoneStatsEntry.getValue();
				bw.newLine();
				bw.append(zoneStatsEntry.getKey() +
						delimiter +
						format.format(stats.getSum()) +
						delimiter +
						stats.getMean() +
						delimiter +
						stats.getMin() +
						delimiter +
						stats.getMax() +
						delimiter +
						stats.getPercentile(95) +
						delimiter +
						stats.getPercentile(90) +
						delimiter +
						stats.getPercentile(80) +
						delimiter +
						stats.getPercentile(75)
						);
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Map<String, DescriptiveStatistics> createZonalStats(String delimiter) {
		Map<String, DescriptiveStatistics> zoneStats = new HashMap<>();
		for (Id<Request> requestId : requestAnalyzer.getWaitTimeCompare().keySet()) {
			DrtRequestSubmittedEvent submission = this.submittedRequests.get(requestId);
			String zoneStr = zones.getZoneForLinkId(submission.getFromLinkId());
			zoneStr += delimiter + zones.getZones().get(zoneStr).getCentroid().getX() +
					delimiter + zones.getZones().get(zoneStr).getCentroid().getY();
			DescriptiveStatistics waitingTimeStats = zoneStats.get(zoneStr);
			if(waitingTimeStats == null){
				waitingTimeStats = new DescriptiveStatistics();
			}
			waitingTimeStats.addValue(requestAnalyzer.getWaitTimeCompare().get(requestId).getFirst());
			zoneStats.put(zoneStr, waitingTimeStats);
		}
		return zoneStats;
	}


}
