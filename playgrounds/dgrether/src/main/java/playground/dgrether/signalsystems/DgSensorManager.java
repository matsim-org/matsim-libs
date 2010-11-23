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
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;


/**
 * @author dgrether
 *
 */
public class DgSensorManager implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	AgentArrivalEventHandler, AgentWait2LinkEventHandler{

	private static final Logger log = Logger.getLogger(DgSensorManager.class);
	
//	private Set<Id> monitoredLinkIds = new HashSet<Id>();
//	private Map<Id, Double> linkIdNumberOfCarsInDistanceMap = new HashMap<Id, Double>();
	private Map<Id, DgSensor> linkIdSensorMap = new HashMap<Id, DgSensor>();
	
	public DgSensorManager(){}
	
	public void registerNumberOfCarsMonitoring(Link link){
//		this.monitoredLinkIds.add(link.getId());
		if (!this.linkIdSensorMap.containsKey(link.getId())){
			this.linkIdSensorMap.put(link.getId(), new DgSensor(link));
		}
	}
	
	public void registerNumberOfCarsInDistanceMonitoring(Link link, Double distanceMeter){
//		this.linkIdNumberOfCarsInDistanceMap.put(link.getId(), distanceMeter);
		if (!this.linkIdSensorMap.containsKey(link.getId())){
			this.linkIdSensorMap.put(link.getId(), new DgSensor(link));
//			this.monitoredLinkIds.add(link.getId());
		}
		this.linkIdSensorMap.get(link.getId()).registerDistanceToMonitor(distanceMeter);

	}

	public int getNumberOfCarsOnLink(Id linkId){
		if (!this.linkIdSensorMap.containsKey(linkId)){
			throw new IllegalStateException("No sensor on link " + linkId + "! Register measurement for this link by calling one of the 'register...' methods of this class first.");
		}
		return this.linkIdSensorMap.get(linkId).getNumberOfCarsOnLink();
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
		//TODO 
	}

	
}
