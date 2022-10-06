/* *********************************************************************** *
 * project: org.matsim.*
 * DgSensorManager
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.sensor;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * @author dgrether
 *
 */
@Singleton
public final class LinkSensorManager implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	VehicleLeavesTrafficEventHandler, PersonEntersVehicleEventHandler, PersonDepartureEventHandler, LaneEnterEventHandler, LaneLeaveEventHandler{

	private static final Logger log = LogManager.getLogger(LinkSensorManager.class);
	
//	private Set<Id> monitoredLinkIds = new HashSet<Id>();
//	private Map<Id, Double> linkIdNumberOfCarsInDistanceMap = new HashMap<Id, Double>();
	private Map<Id<Link>, LinkSensor> linkIdSensorMap = new HashMap<>();

	private Map<Id<Link>, Map<Id<Lane>, LaneSensor>> linkIdLaneIdSensorMap = new HashMap<>();

	private Network network;
	private Lanes laneDefinitions = null;
	
	private Map<Id<Person>, Id<Link>> personDepartureLinks = new HashMap<>();
	
	@Inject
	public LinkSensorManager(Scenario scenario, EventsManager events){
		this.network = scenario.getNetwork();
		if (scenario.getConfig().network().getLaneDefinitionsFile() != null || scenario.getConfig().qsim().isUseLanes()) {
			laneDefinitions = scenario.getLanes();
		}
		events.addHandler(this);
	}
	
	public void registerNumberOfCarsMonitoring(Id<Link> linkId){
//		this.monitoredLinkIds.add(link.getId());
		if (!this.linkIdSensorMap.containsKey(linkId)){
			Link link = this.network.getLinks().get(linkId);
			if (link == null){
				throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
			}
			this.linkIdSensorMap.put(linkId, new LinkSensor(link));
		}
	}
	
	/**
	 * 
	 * @param distanceMeter the distance in meter from the end of the monitored link
	 */
	public void registerNumberOfCarsInDistanceMonitoring(Id<Link> linkId, Double distanceMeter){
//		this.linkIdNumberOfCarsInDistanceMap.put(link.getId(), distanceMeter);
		if (!this.linkIdSensorMap.containsKey(linkId)){
			Link link = this.network.getLinks().get(linkId);
			if (link == null){
				throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
			}
			this.linkIdSensorMap.put(link.getId(), new LinkSensor(link));
//			this.monitoredLinkIds.add(link.getId());
		}
		this.linkIdSensorMap.get(linkId).registerDistanceToMonitor(distanceMeter);
	}

	public void registerNumberOfCarsOnLaneInDistanceMonitoring(Id<Link> linkId, Id<Lane> laneId, Double distanceMeter) {
		Link link = this.network.getLinks().get(linkId);
		if (link == null){
			throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
		}
		if (this.laneDefinitions == null || this.laneDefinitions.getLanesToLinkAssignments().get(linkId) == null ||
				this.laneDefinitions.getLanesToLinkAssignments().get(linkId).getLanes().get(laneId) == null) {
			throw new IllegalStateException("No data found for lane  " + laneId + " on link  " + linkId + " is not in the network, can't register sensor");
		}
		if (! this.linkIdLaneIdSensorMap.containsKey(linkId)){
			this.linkIdLaneIdSensorMap.put(linkId, new HashMap<>());
		}
		if (! this.linkIdLaneIdSensorMap.get(linkId).containsKey(laneId)){
			Lane lane = this.laneDefinitions.getLanesToLinkAssignments().get(linkId).getLanes().get(laneId);
			this.linkIdLaneIdSensorMap.get(linkId).put(laneId, new LaneSensor(link, lane));
		}
		this.linkIdLaneIdSensorMap.get(linkId).get(laneId).registerDistanceToMonitor(distanceMeter);
	}
	
	public void registerNumberOfCarsMonitoringOnLane(Id<Link> linkId, Id<Lane> laneId) {
		Link link = this.network.getLinks().get(linkId);
		if (link == null){
			throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
		}
		if (this.laneDefinitions == null || this.laneDefinitions.getLanesToLinkAssignments().get(linkId) == null ||
				this.laneDefinitions.getLanesToLinkAssignments().get(linkId).getLanes().get(laneId) == null) {
			throw new IllegalStateException("No data found for lane  " + laneId + " on link  " + linkId + " is not in the network, can't register sensor");
		}
		if (! this.linkIdLaneIdSensorMap.containsKey(linkId)){
			this.linkIdLaneIdSensorMap.put(linkId, new HashMap<>());
		}
		if (! this.linkIdLaneIdSensorMap.get(linkId).containsKey(laneId)){
			Lane lane = this.laneDefinitions.getLanesToLinkAssignments().get(linkId).getLanes().get(laneId);
			this.linkIdLaneIdSensorMap.get(linkId).put(laneId, new LaneSensor(link, lane));
		}
	}

	public void registerAverageNumberOfCarsPerSecondMonitoringOnLane(Id<Link> linkId, Id<Lane> laneId) {
		registerAverageNumberOfCarsPerSecondMonitoringOnLane(linkId, laneId, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
	}
	public void registerAverageNumberOfCarsPerSecondMonitoringOnLane(Id<Link> linkId, Id<Lane> laneId, double lookBackTime, double timeBucketCollectionDuration) {
		Link link = this.network.getLinks().get(linkId);
		//check if link is in the network
		if (link == null){
			throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
		}
		//check if lane exists
		if (this.laneDefinitions == null || this.laneDefinitions.getLanesToLinkAssignments().get(linkId) == null ||
				this.laneDefinitions.getLanesToLinkAssignments().get(linkId).getLanes().get(laneId) == null) {
			throw new IllegalStateException("No data found for lane  " + laneId + " on link  " + linkId + " is not in the network, can't register sensor");
		}
		//check if the sensor-map already contains an entry for this link
		if (! this.linkIdLaneIdSensorMap.containsKey(linkId)){
			this.linkIdLaneIdSensorMap.put(linkId, new HashMap<>());
		}
		//check if the entry in sensor-map for this link has already a value for this lane
		if (! this.linkIdLaneIdSensorMap.get(linkId).containsKey(laneId)){
			Lane lane = this.laneDefinitions.getLanesToLinkAssignments().get(linkId).getLanes().get(laneId);
			this.linkIdLaneIdSensorMap.get(linkId).put(laneId, new LaneSensor(link, lane));
		}
		//register AvgVehPerSecond monitor for this lane
		linkIdLaneIdSensorMap.get(linkId).get(laneId).registerAverageVehiclesPerSecondToMonitor(lookBackTime, timeBucketCollectionDuration);

	}

	public void registerAverageNumberOfCarsPerSecondMonitoring(Id<Link> linkId) {
		registerAverageNumberOfCarsPerSecondMonitoring(linkId, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
	}
	
	public void registerAverageNumberOfCarsPerSecondMonitoring(Id<Link> linkId, double lookBackTime, double timeBucketCollectionDuration) {
		if (!this.linkIdSensorMap.containsKey(linkId)){
			Link link = this.network.getLinks().get(linkId);
			if (link == null){
				throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
			}
			this.linkIdSensorMap.put(link.getId(), new LinkSensor(link));
		}
		this.linkIdSensorMap.get(linkId).registerAverageVehiclesPerSecondToMonitor(lookBackTime, timeBucketCollectionDuration);
	}

	public int getNumberOfCarsOnLink(Id<Link> linkId){
		if (!this.linkIdSensorMap.containsKey(linkId)){
			throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
		}
		return this.linkIdSensorMap.get(linkId).getNumberOfCarsOnLink();
	}
	
	public int getNumberOfCarsOnLane(Id<Link> linkId, Id<Lane> laneId) {
		if (!this.linkIdLaneIdSensorMap.containsKey(linkId)){
			throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
		}
		Map<Id<Lane>, LaneSensor> map = this.linkIdLaneIdSensorMap.get(linkId);
		if (map == null || !map.containsKey(laneId)){
			throw new IllegalStateException("No sensor on lane " + laneId + " of link " + linkId + "! Register measurement for this link lane pair!");
		}
		return map.get(laneId).getNumberOfCarsOnLane();
	}

	
	public int getNumberOfCarsInDistance(Id<Link> linkId, Double distanceMeter, double timeSeconds){
		if (!this.linkIdSensorMap.containsKey(linkId)){
			throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
		}
		return this.linkIdSensorMap.get(linkId).getNumberOfCarsInDistance(distanceMeter, timeSeconds);
	}

	public int getNumberOfCarsInDistanceOnLane(Id<Link> linkId, Id<Lane> laneId, Double distanceMeter, double timeSeconds){
        if (!this.linkIdLaneIdSensorMap.containsKey(linkId)){
            throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
        }
        Map<Id<Lane>, LaneSensor> map = this.linkIdLaneIdSensorMap.get(linkId);
        if (map == null || !map.containsKey(laneId)){
            throw new IllegalStateException("No sensor on lane " + laneId + " of link " + linkId + "! Register measurement for this link lane pair!");
        }
		return map.get(laneId).getNumberOfCarsInDistance(distanceMeter, timeSeconds);
	}

	public double getAverageArrivalRateOnLink(Id<Link> linkId, double now) {
		return this.linkIdSensorMap.get(linkId).getAvgVehiclesPerSecond(now);
	}

	public double getAverageArrivalRateOnLane(Id<Link> linkId, Id<Lane> laneId, double now) {
		return this.linkIdLaneIdSensorMap.get(linkId).get(laneId).getAvgVehiclesPerSecond(now);
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.linkIdSensorMap.containsKey(event.getLinkId())){
			this.linkIdSensorMap.get(event.getLinkId()).handleEvent(event);
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.linkIdSensorMap.containsKey(event.getLinkId())){
			this.linkIdSensorMap.get(event.getLinkId()).handleEvent(event);
		}
	}
	
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (this.linkIdSensorMap.containsKey(event.getLinkId())){
			this.linkIdSensorMap.get(event.getLinkId()).handleEvent(event);
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		personDepartureLinks.put(event.getPersonId(), event.getLinkId());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.linkIdSensorMap.containsKey(personDepartureLinks.get(event.getPersonId()))){
			this.linkIdSensorMap.get(personDepartureLinks.get(event.getPersonId())).handleEvent(event);		
		}
	}
	
	@Override
	public void reset(int iteration) {
		this.linkIdSensorMap.clear();
		this.linkIdLaneIdSensorMap.clear();
	}

	@Override
	public void handleEvent(LaneLeaveEvent event) {
		if (this.linkIdLaneIdSensorMap.containsKey(event.getLinkId())){
			Map<Id<Lane>, LaneSensor> map = this.linkIdLaneIdSensorMap.get(event.getLinkId());
			if (map.containsKey(event.getLaneId())){
				map.get(event.getLaneId()).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(LaneEnterEvent event) {
		if (this.linkIdLaneIdSensorMap.containsKey(event.getLinkId())){
			Map<Id<Lane>, LaneSensor> map = this.linkIdLaneIdSensorMap.get(event.getLinkId());
			if (map.containsKey(event.getLaneId())){
				map.get(event.getLaneId()).handleEvent(event);
			}
		}
	}
}
