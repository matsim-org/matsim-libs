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
package playground.dgrether.signalsystems;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.LaneLeaveEvent;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.core.events.handler.LaneLeaveEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;


/**
 * @author dgrether
 *
 */
public class DgSensorManager implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	AgentArrivalEventHandler, AgentWait2LinkEventHandler, LaneEnterEventHandler, LaneLeaveEventHandler{

	private static final Logger log = Logger.getLogger(DgSensorManager.class);
	
//	private Set<Id> monitoredLinkIds = new HashSet<Id>();
//	private Map<Id, Double> linkIdNumberOfCarsInDistanceMap = new HashMap<Id, Double>();
	private Map<Id, DgSensor> linkIdSensorMap = new HashMap<Id, DgSensor>();

	private Map<Id, Map<Id, DgLaneSensor>> linkIdLaneIdSensorMap = new HashMap<Id, Map<Id, DgLaneSensor>>();
	
	private Map<Id, Tuple<Double, Double>> linkFirstSecondDistanceMeterMap = new HashMap<Id, Tuple<Double, Double>>();

	private Network network;

	private LaneDefinitions laneDefinitions = null;
	
	public DgSensorManager(Network network){
		this.network = network;
	}
	
	public void registerNumberOfCarsMonitoring(Id linkId){
//		this.monitoredLinkIds.add(link.getId());
		if (!this.linkIdSensorMap.containsKey(linkId)){
			Link link = this.network.getLinks().get(linkId);
			if (link == null){
				throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
			}
			this.linkIdSensorMap.put(linkId, new DgSensor(link));
		}
	}
	
	/**
	 * 
	 * @param distanceMeter the distance in meter from the end of the monitored link
	 */
	public void registerNumberOfCarsInDistanceMonitoring(Id linkId, Double distanceMeter){
//		this.linkIdNumberOfCarsInDistanceMap.put(link.getId(), distanceMeter);
		if (!this.linkIdSensorMap.containsKey(linkId)){
			Link link = this.network.getLinks().get(linkId);
			if (link == null){
				throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
			}
			this.linkIdSensorMap.put(link.getId(), new DgSensor(link));
//			this.monitoredLinkIds.add(link.getId());
		}
		this.linkIdSensorMap.get(linkId).registerDistanceToMonitor(distanceMeter);
	}
	
	public void registerNumberOfCarsMonitoringOnLane(Id linkId, Id laneId) {
		Link link = this.network.getLinks().get(linkId);
		if (link == null){
			throw new IllegalStateException("Link with Id " + linkId + " is not in the network, can't register sensor");
		}
		if (this.laneDefinitions == null || this.laneDefinitions.getLanesToLinkAssignments().get(linkId) == null ||
				this.laneDefinitions.getLanesToLinkAssignments().get(linkId).getLanes().get(laneId) == null) {
			throw new IllegalStateException("No data found for lane  " + laneId + " on link  " + linkId + " is not in the network, can't register sensor");
		}
		if (! this.linkIdLaneIdSensorMap.containsKey(linkId)){
			this.linkIdLaneIdSensorMap.put(linkId, new HashMap<Id, DgLaneSensor>());
		}
		if (! this.linkIdLaneIdSensorMap.get(linkId).containsKey(laneId)){
			Lane lane = this.laneDefinitions.getLanesToLinkAssignments().get(linkId).getLanes().get(laneId);
			this.linkIdLaneIdSensorMap.get(linkId).put(laneId, new DgLaneSensor(link, lane));
		}
	
	}

	
	public void registerCarsAtDistancePerSecondMonitoring(Id linkId, Double distanceMeter){
		double firstDistanceMeter = distanceMeter;
		Link link = this.network.getLinks().get(linkId);
		double secondDistanceMeter = distanceMeter -  2 * link.getFreespeed();
		//TODO could also exceed link length
		if (secondDistanceMeter < 0.0){
			firstDistanceMeter = 2* link.getFreespeed();
			secondDistanceMeter = 0.0;
		}
		Tuple<Double, Double> tuple = new Tuple<Double, Double>(firstDistanceMeter, secondDistanceMeter);
		log.error("Link " + linkId + " first pos: " + tuple.getFirst() + " second pos: " + tuple.getSecond() + " length " + link.getLength());
		this.linkFirstSecondDistanceMeterMap .put(link.getId(), tuple);
		this.registerNumberOfCarsInDistanceMonitoring(linkId, firstDistanceMeter);
		this.registerNumberOfCarsInDistanceMonitoring(linkId, secondDistanceMeter);
	}
	
	public int getNumberOfCarsAtDistancePerSecond(Id linkId, Double distanceMeter, double timeSeconds){
		Tuple<Double, Double> tuple = this.linkFirstSecondDistanceMeterMap.get(linkId);
		int numberOfCarsFirstDetector = this.getNumberOfCarsInDistance(linkId, tuple.getFirst(), timeSeconds);
		int numberOfCarsSecondDetector = this.getNumberOfCarsInDistance(linkId, tuple.getSecond(), timeSeconds);
		log.error("Link " + linkId + " first pos: " + tuple.getFirst() + " second pos: " + tuple.getSecond());
		log.error("NumberOfCars SecondDetector: " + numberOfCarsSecondDetector + " first detector: " + numberOfCarsFirstDetector);
		return numberOfCarsFirstDetector - numberOfCarsSecondDetector  ;
	}

	public int getNumberOfCarsOnLink(Id linkId){
		if (!this.linkIdSensorMap.containsKey(linkId)){
			throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
		}
		return this.linkIdSensorMap.get(linkId).getNumberOfCarsOnLink();
	}
	
	public int getNumberOfCarsOnLane(Id linkId, Id laneId) {
		if (!this.linkIdLaneIdSensorMap.containsKey(linkId)){
			throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
		}
		Map<Id, DgLaneSensor> map = this.linkIdLaneIdSensorMap.get(linkId);
		if (map == null || !map.containsKey(laneId)){
			throw new IllegalStateException("No sensor on lane " + laneId + " of link " + linkId + "! Register measurement for this link lane pair!");
		}
		return map.get(laneId).getNumberOfCarsOnLink();
	}

	
	public int getNumberOfCarsInDistance(Id linkId, Double distanceMeter, double timeSeconds){
		if (!this.linkIdSensorMap.containsKey(linkId)){
			throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
		}
		//TODO add further check
		return this.linkIdSensorMap.get(linkId).getNumberOfCarsInDistance(distanceMeter, timeSeconds);
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
	public void handleEvent(AgentArrivalEvent event) {
		if (this.linkIdSensorMap.containsKey(event.getLinkId())){
			this.linkIdSensorMap.get(event.getLinkId()).handleEvent(event);
		}
	}
	
	@Override
	public void handleEvent(AgentWait2LinkEvent event) {
		if (this.linkIdSensorMap.containsKey(event.getLinkId())){
			this.linkIdSensorMap.get(event.getLinkId()).handleEvent(event);		
		}
	}
	
	@Override
	public void reset(int iteration) {
		this.linkIdSensorMap.clear();
		this.linkFirstSecondDistanceMeterMap.clear();
	}

	public void setLaneDefinitions(LaneDefinitions laneDefinitions) {
		this.laneDefinitions  = laneDefinitions;
	}

	@Override
	public void handleEvent(LaneLeaveEvent event) {
		if (this.linkIdLaneIdSensorMap.containsKey(event.getLinkId())){
			Map<Id, DgLaneSensor> map = this.linkIdLaneIdSensorMap.get(event.getLinkId());
			if (map.containsKey(event.getLaneId())){
				map.get(event.getLaneId()).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(LaneEnterEvent event) {
		if (this.linkIdLaneIdSensorMap.containsKey(event.getLinkId())){
			Map<Id, DgLaneSensor> map = this.linkIdLaneIdSensorMap.get(event.getLinkId());
			if (map.containsKey(event.getLaneId())){
				map.get(event.getLaneId()).handleEvent(event);
			}
		}
	}

	
}
