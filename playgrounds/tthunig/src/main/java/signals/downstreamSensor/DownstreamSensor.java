/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package signals.downstreamSensor;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup;

import com.google.inject.Inject;

import signals.sensor.LinkSensorManager;

/**
 * @author tthunig
 *
 */
public final class DownstreamSensor {

	private final LinkSensorManager sensorManager;
	private final Network network;
	private final JDEQSimConfigGroup jdeQSim;
	
	private Map<Id<Link>, Integer> linkMaxNoCarsForStorage = new HashMap<>();
	private Map<Id<Link>, Integer> linkMaxNoCarsForFreeSpeed = new HashMap<>();
	
	/* the controller will allow a delay of at most this factor times the free speed travel time */
	private double delayFactor = 1;
	/* the controller will allow a link occupation of at most this factor times the maximum number of vehicles regarding to the storage capacity */
	private double storageFactor = 0.75;
	// TODO make this adjustable per downstream config?! (add config to constructor parameter)
	
	@Inject
	public DownstreamSensor(LinkSensorManager sensorManager, Scenario scenario) {
		this.sensorManager = sensorManager;
		this.network = scenario.getNetwork();
		this.jdeQSim = scenario.getConfig().jdeqSim();
		init();
	}
	
	private void init() {
		// determine different occupancy criterion for links, i.e. maximum number of vehicles
		linkMaxNoCarsForStorage = new HashMap<>();
		linkMaxNoCarsForFreeSpeed = new HashMap<>();
		for (Link link : network.getLinks().values()) {
			// maximum number = storage capacity * factor
			linkMaxNoCarsForStorage.put(link.getId(), (int) ((link.getLength() / jdeQSim.getCarSize()) * storageFactor));

			// maximum number such that (free speed travel time * factor) can be reached (when vehicles are distributed uniformly over time)
			int maxNoCarsForFreeSpeedTT = (int) Math.ceil((link.getLength() / link.getFreespeed()) * delayFactor * (link.getCapacity() / 3600));
			linkMaxNoCarsForFreeSpeed.put(link.getId(), maxNoCarsForFreeSpeedTT);
//			log.info("setting max number of cars for free speed travel time to " + maxNoCarsForFreeSpeedTT);
		}
	}
	
	public boolean linkEmpty(Id<Link> downstreamLinkId) {
		// TODO try around with different regimes
		// stop green if one of the numbers is exceeded
		// if (this.sensorManager.getNumberOfCarsOnLink(downstreamLinkId) > linkMaxNoCarsForFreeSpeed.get(downstreamLinkId)) {
		int numberOfCarsOnLink = this.sensorManager.getNumberOfCarsOnLink(downstreamLinkId);
		int maxNoCarsForStorage = linkMaxNoCarsForStorage.get(downstreamLinkId);
		int maxNoCarsForFreespeed = linkMaxNoCarsForFreeSpeed.get(downstreamLinkId);
		if (numberOfCarsOnLink > Math.min(maxNoCarsForStorage, maxNoCarsForFreespeed)) {
			return false;
		}
		return true;
	}
	
}
