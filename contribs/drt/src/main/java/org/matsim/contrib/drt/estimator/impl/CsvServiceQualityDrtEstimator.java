/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.estimator.impl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.OptionalTime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Looks up DRT wait and ride-time estimates from stop-to-stop service quality probe output.
 */
public class CsvServiceQualityDrtEstimator implements DrtEstimator {
	private static final char DEFAULT_DELIMITER = ';';

	private final Map<StopPair, NavigableMap<Double, ServiceQuality>> estimates = new HashMap<>();
	private final Map<Id<Link>, Id<DrtStopFacility>> stopByLinkId = new HashMap<>();

	record StopPair(Id<DrtStopFacility> originStop, Id<DrtStopFacility> destinationStop) {
	}

	record ServiceQuality(double waitTime, double directRideTime, double rideTimeWithDetour) {
	}

	public CsvServiceQualityDrtEstimator(String csvFile, DrtStopNetwork stopNetwork) {
		this(csvFile, stopNetwork, DEFAULT_DELIMITER);
	}

	public CsvServiceQualityDrtEstimator(String csvFile, DrtStopNetwork stopNetwork, char delimiter) {
		for (DrtStopFacility stop : stopNetwork.getDrtStops().values()) {
			stopByLinkId.putIfAbsent(stop.getLinkId(), stop.getId());
		}
		read(csvFile, delimiter);
	}

	@Override
	public Estimate estimate(DrtRoute route, OptionalTime departureTime) {
		Id<DrtStopFacility> originStop = stopByLinkId.get(route.getStartLinkId());
		Id<DrtStopFacility> destinationStop = stopByLinkId.get(route.getEndLinkId());
		if (originStop == null || destinationStop == null) {
			throw new IllegalArgumentException("Cannot map DRT route links to stop ids: fromLink=" + route.getStartLinkId()
				+ ", toLink=" + route.getEndLinkId());
		}

		StopPair stopPair = new StopPair(originStop, destinationStop);
		NavigableMap<Double, ServiceQuality> byTime = estimates.get(stopPair);
		if (byTime == null || byTime.isEmpty()) {
			throw new IllegalArgumentException("No DRT service quality estimate for stop pair " + originStop + " -> " + destinationStop);
		}

		double time = departureTime.isDefined() ? departureTime.seconds() : byTime.firstKey();
		ServiceQuality serviceQuality = nearest(byTime, time);
		if (!Double.isFinite(serviceQuality.waitTime) || !Double.isFinite(serviceQuality.rideTimeWithDetour)) {
			throw new IllegalStateException("DRT service quality estimate is not finite for stop pair " + originStop
				+ " -> " + destinationStop + " around time " + time);
		}

		return new Estimate(route.getDistance(), serviceQuality.rideTimeWithDetour, serviceQuality.waitTime, 0);
	}

	private void read(String csvFile, char delimiter) {
		CSVFormat format = CSVFormat.DEFAULT.builder()
			.setDelimiter(delimiter)
			.setHeader()
			.setSkipHeaderRecord(true)
			.build();
		try (CSVParser parser = new CSVParser(IOUtils.getBufferedReader(csvFile), format)) {
			for (CSVRecord record : parser) {
				double time = Double.parseDouble(record.get("time"));
				Id<DrtStopFacility> originStop = Id.create(record.get("originStop"), DrtStopFacility.class);
				Id<DrtStopFacility> destinationStop = Id.create(record.get("destinationStop"), DrtStopFacility.class);
				ServiceQuality serviceQuality = new ServiceQuality(
					Double.parseDouble(record.get("waitTime")),
					Double.parseDouble(record.get("directRideTime")),
					Double.parseDouble(record.get("rideTimeWithDetour")));
				estimates.computeIfAbsent(new StopPair(originStop, destinationStop), _ -> new TreeMap<>())
					.put(time, serviceQuality);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to read DRT service quality estimates from " + csvFile, e);
		}
	}

	private static ServiceQuality nearest(NavigableMap<Double, ServiceQuality> byTime, double time) {
		Map.Entry<Double, ServiceQuality> floor = byTime.floorEntry(time);
		Map.Entry<Double, ServiceQuality> ceiling = byTime.ceilingEntry(time);
		if (floor == null) {
			return ceiling.getValue();
		}
		if (ceiling == null) {
			return floor.getValue();
		}
		return time - floor.getKey() <= ceiling.getKey() - time ? floor.getValue() : ceiling.getValue();
	}
}
