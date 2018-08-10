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
package org.matsim.contrib.signals.sensor;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.sensor.LinkSensorManager;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author tthunig
 *
 */
public final class DownstreamSensor {

	private final LinkSensorManager sensorManager;
	private final Network network;
	private final Lanes lanes;
	private final double carSize;
	private final double storageCapacityFactor;
	private final SignalsData signalsData;
	
	private Map<Id<Link>, Integer> linkMaxNoCarsForStorage = new HashMap<>();
	private Map<Id<Link>, Integer> linkMaxNoCarsForFreeSpeed = new HashMap<>();
	
	/* the controller will allow a delay of at most this factor times the free speed travel time */
	private final double maxDelayFactor = 1;
	/* the controller will allow a link occupation of at most this factor times the maximum number of vehicles regarding to the storage capacity */
	private final double maxStorageFactor = 0.75;
	// TODO make this adjustable per downstream config?! (add config to constructor parameter)
	
	@Inject
	public DownstreamSensor(LinkSensorManager sensorManager, Scenario scenario) {
		this.sensorManager = sensorManager;
		this.network = scenario.getNetwork();
		this.lanes = scenario.getLanes();
		this.carSize = scenario.getConfig().jdeqSim().getCarSize();
		this.storageCapacityFactor = scenario.getConfig().qsim().getStorageCapFactor();
		this.signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		init();
	}
	
	private void init() {
		// determine different occupancy criterion for links, i.e. maximum number of vehicles
		linkMaxNoCarsForStorage = new HashMap<>();
		linkMaxNoCarsForFreeSpeed = new HashMap<>();
		for (Link link : network.getLinks().values()) {
			// maximum number = storage capacity * factor
			linkMaxNoCarsForStorage.put(link.getId(), (int) ((link.getLength() / carSize) * maxStorageFactor * storageCapacityFactor));

			// maximum number such that (free speed travel time * factor) can be reached (when vehicles are distributed uniformly over time)
			int maxNoCarsForFreeSpeedTT = (int) Math.ceil((link.getLength() / link.getFreespeed()) * maxDelayFactor * (link.getCapacity() / 3600));
			linkMaxNoCarsForFreeSpeed.put(link.getId(), maxNoCarsForFreeSpeedTT);
//			log.info("setting max number of cars for free speed travel time to " + maxNoCarsForFreeSpeedTT);
		}
	}
	
	public void registerDownstreamSensor(Id<Link> downstreamLinkId){
		this.sensorManager.registerNumberOfCarsMonitoring(downstreamLinkId);
	}
	
	public void registerDownstreamSensors(Set<Id<Link>> downstreamLinkIds){
		for (Id<Link> downstreamLinkId : downstreamLinkIds){
			registerDownstreamSensor(downstreamLinkId);
		}
	}
	
	public void registerDownstreamSensors(SignalSystem signalSystem){
		for (Signal signal : signalSystem.getSignals().values()){
			Node systemNode = this.network.getLinks().get(signal.getLinkId()).getToNode();
			for (Id<Link> outgoingLinkId : systemNode.getOutLinks().keySet()) {
				this.sensorManager.registerNumberOfCarsMonitoring(outgoingLinkId);
			}
			break; // systemNode is the same for all signals of the system
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
	
	public boolean allLinksEmpty(Set<Id<Link>> downstreamLinkIds) {
		for (Id<Link> downstreamLinkId : downstreamLinkIds){
			if (!linkEmpty(downstreamLinkId))
				return false;
		}
		return true;			
	}
	
	public boolean allDownstreamLinksEmpty(SignalData signal){
		if (signal.getTurningMoveRestrictions() != null) {
			return allLinksEmpty(signal.getTurningMoveRestrictions());
		} // else:
		if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
			Set<Id<Link>> toLinks = new HashSet<>();
			for (Id<Lane> laneId : signal.getLaneIds()) {
				Lane lane = lanes.getLanesToLinkAssignments().get(signal.getLinkId()).getLanes().get(laneId);
				toLinks.addAll(lane.getToLinkIds());
			}
			return allLinksEmpty(toLinks);
		} // else:
		// if no turning move restrictions and no lanes with to links are set, turning is allowed to all outgoing links
		Node systemNode = this.network.getLinks().get(signal.getLinkId()).getToNode();
		return allLinksEmpty(systemNode.getOutLinks().keySet());
	}
	
	public boolean allDownstreamLinksEmpty(Id<SignalSystem> signalSystemId, Id<SignalGroup> signalGroupId){
		for (Id<Signal> signalId : signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(signalSystemId).get(signalGroupId).getSignalIds()){
			SignalData signal = signalsData.getSignalSystemsData().getSignalSystemData().get(signalSystemId).getSignalData().get(signalId);
			if (!allDownstreamLinksEmpty(signal))
				return false;
		}
		return true;
	}
	
}
