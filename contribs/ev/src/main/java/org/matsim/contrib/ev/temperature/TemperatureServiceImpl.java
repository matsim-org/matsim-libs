/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package org.matsim.contrib.ev.temperature;/*
 * created by jbischoff, 15.08.2018
 */

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.util.DistanceUtils;

public class TemperatureServiceImpl implements TemperatureService, TemperatureChangeEventHandler {
	private final Network network;
	private final Map<Link, Double> temperatures = new LinkedHashMap<>();

	@Inject
	TemperatureServiceImpl(Network network) {
		this.network = network;
	}

	@Override
	public double getCurrentTemperature(Id<Link> linkId) {
		Link l = network.getLinks().get(linkId);
		double closestdistance = Double.POSITIVE_INFINITY;
		double closestTemperature = Double.NaN;
		for (Map.Entry<Link, Double> e : temperatures.entrySet()) {
			double dist = DistanceUtils.calculateSquaredDistance(e.getKey().getCoord(), l.getCoord());
			if (dist < closestdistance) {
				closestdistance = dist;
				closestTemperature = e.getValue();
			}
		}
		if (Double.isNaN(closestTemperature)) {
			throw new RuntimeException("No temperature information provided so far");
		}
		return closestTemperature;
	}

	@Override
	public void handleEvent(TemperatureChangeEvent event) {
		Link l = network.getLinks().get(event.getLinkId());
		temperatures.put(l, event.getNewTemperatureC());
	}

	@Override
	public void reset(int iteration) {
		temperatures.clear();
	}
}
