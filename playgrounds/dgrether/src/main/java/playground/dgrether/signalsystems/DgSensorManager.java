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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;

import com.google.inject.Inject;


/**
 * @author dgrether
 *
 */
public class DgSensorManager implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler, LaneEnterEventHandler, LaneLeaveEventHandler{

	private static final Logger log = Logger.getLogger(DgSensorManager.class);
	
//	private Set<Id> monitoredLinkIds = new HashSet<Id>();
//	private Map<Id, Double> linkIdNumberOfCarsInDistanceMap = new HashMap<Id, Double>();
	private Map<Id<Link>, DgSensor> linkIdSensorMap = new HashMap<>();

	private Map<Id<Link>, Map<Id<Lane>, DgLaneSensor>> linkIdLaneIdSensorMap = new HashMap<>();
	
	@Deprecated // not tested
	private Map<Id<Link>, Tuple<Double, Double>> linkFirstSecondDistanceMeterMap = new HashMap<>();

	private Network network;
	private Lanes laneDefinitions = null;
	
	@Inject
	public DgSensorManager(Scenario scenario){
		this.network = scenario.getNetwork();
		if (scenario.getConfig().network().getLaneDefinitionsFile() != null || scenario.getConfig().qsim().isUseLanes()) {
			laneDefinitions = scenario.getLanes();
		}
	}
	
	public void registerNumberOfCarsMonitoring(Id<Link> linkId){
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
	public void registerNumberOfCarsInDistanceMonitoring(Id<Link> linkId, Double distanceMeter){
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
			this.linkIdLaneIdSensorMap.get(linkId).put(laneId, new DgLaneSensor(link, lane));
		}
	
	}

	@Deprecated //not tested
	private void registerCarsAtDistancePerSecondMonitoring(Id<Link> linkId, Double distanceMeter){
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
		this.linkFirstSecondDistanceMeterMap.put(link.getId(), tuple);
		this.registerNumberOfCarsInDistanceMonitoring(linkId, firstDistanceMeter);
		this.registerNumberOfCarsInDistanceMonitoring(linkId, secondDistanceMeter);
	}
	
	@Deprecated //not tested
	private int getNumberOfCarsAtDistancePerSecond(Id<Link> linkId, Double distanceMeter, double timeSeconds){
		Tuple<Double, Double> tuple = this.linkFirstSecondDistanceMeterMap.get(linkId);
		int numberOfCarsFirstDetector = this.getNumberOfCarsInDistance(linkId, tuple.getFirst(), timeSeconds);
		int numberOfCarsSecondDetector = this.getNumberOfCarsInDistance(linkId, tuple.getSecond(), timeSeconds);
		log.error("Link " + linkId + " first pos: " + tuple.getFirst() + " second pos: " + tuple.getSecond());
		log.error("NumberOfCars SecondDetector: " + numberOfCarsSecondDetector + " first detector: " + numberOfCarsFirstDetector);
		return numberOfCarsFirstDetector - numberOfCarsSecondDetector  ;
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
		Map<Id<Lane>, DgLaneSensor> map = this.linkIdLaneIdSensorMap.get(linkId);
		if (map == null || !map.containsKey(laneId)){
			throw new IllegalStateException("No sensor on lane " + laneId + " of link " + linkId + "! Register measurement for this link lane pair!");
		}
		return map.get(laneId).getNumberOfCarsOnLink();
	}

	
	public int getNumberOfCarsInDistance(Id<Link> linkId, Double distanceMeter, double timeSeconds){
		if (!this.linkIdSensorMap.containsKey(linkId)){
			throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
		}
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
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (this.linkIdSensorMap.containsKey(event.getLinkId())){
			this.linkIdSensorMap.get(event.getLinkId()).handleEvent(event);
		}
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (this.linkIdSensorMap.containsKey(event.getLinkId())){
			this.linkIdSensorMap.get(event.getLinkId()).handleEvent(event);		
		}
	}
	
	@Override
	public void reset(int iteration) {
		this.linkIdSensorMap.clear();
		this.linkFirstSecondDistanceMeterMap.clear();
	}

	@Override
	public void handleEvent(LaneLeaveEvent event) {
		if (this.linkIdLaneIdSensorMap.containsKey(event.getLinkId())){
			Map<Id<Lane>, DgLaneSensor> map = this.linkIdLaneIdSensorMap.get(event.getLinkId());
			if (map.containsKey(event.getLaneId())){
				map.get(event.getLaneId()).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(LaneEnterEvent event) {
		if (this.linkIdLaneIdSensorMap.containsKey(event.getLinkId())){
			Map<Id<Lane>, DgLaneSensor> map = this.linkIdLaneIdSensorMap.get(event.getLinkId());
			if (map.containsKey(event.getLaneId())){
				map.get(event.getLaneId()).handleEvent(event);
			}
		}
	}

	
}
