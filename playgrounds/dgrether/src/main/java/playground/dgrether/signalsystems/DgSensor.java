/* *********************************************************************** *
 * project: org.matsim.*
 * DgSensor
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;


/**
 * @author dgrether
 *
 */
public class DgSensor {
	
	private Link link = null;
	public int agentsOnLink = 0;
	
	private boolean doDistanceMonitoring = false;
	private Map<Double, Map<Id, CarLocator>> distanceMeterCarLocatorMap = null;
	private Map<Id, AgentArrivalEvent> personIdArrivalEventMap = null;
	private Map<Double, Map<Id,CarLocator>> inActivityDistanceCarLocatorMap = null;
	
	public DgSensor(Link link){
		this.link  = link;
	}
	
	/**
	 * 
	 * @param distanceMeter the distance in meter from the end of the monitored link
	 */
	public void registerDistanceToMonitor(Double distanceMeter){
		if (! this.doDistanceMonitoring) {
			this.enableDistanceMonitoring();
		}
		this.distanceMeterCarLocatorMap.put(distanceMeter, new HashMap<Id, CarLocator>());
		this.inActivityDistanceCarLocatorMap.put(distanceMeter, new HashMap<Id, CarLocator>());
	}
	
	private void enableDistanceMonitoring() {
		this.doDistanceMonitoring = true;
		this.distanceMeterCarLocatorMap = new HashMap<Double, Map<Id, CarLocator>>();
		this.personIdArrivalEventMap = new HashMap<Id, AgentArrivalEvent>();
		this.inActivityDistanceCarLocatorMap = new HashMap<Double, Map<Id, CarLocator>>();
	}

	public int getNumberOfCarsOnLink() {
		return this.agentsOnLink;
	}

	public int getNumberOfCarsInDistance(Double distanceMeter, double timeSeconds) {
		Map<Id, CarLocator> carLocators = this.distanceMeterCarLocatorMap.get(distanceMeter);
		int count = 0;
		for (CarLocator cl : carLocators.values()){
			if (cl.isCarinDistance(timeSeconds)){
				count++;
			}
		}
		return count;
	}

	public void handleEvent(LinkEnterEvent event) {
		this.agentsOnLink++;
		if (this.doDistanceMonitoring){
			for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
				Map<Id, CarLocator> personIdCarLocatorMap = this.distanceMeterCarLocatorMap.get(distance);
				personIdCarLocatorMap.put(event.getPersonId(), new CarLocator(this.link, event.getTime(), distance));
			}
		}
	}
	
	public void handleEvent(LinkLeaveEvent event) {
		this.agentsOnLink--;
		if (this.doDistanceMonitoring){
			for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
				Map<Id, CarLocator> personIdCarLocatorMap = this.distanceMeterCarLocatorMap.get(distance);
				personIdCarLocatorMap.remove(event.getPersonId());
			}
		}
	}
	
	public void handleEvent(AgentArrivalEvent event) {
		this.agentsOnLink--;
		if (this.doDistanceMonitoring){
			for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
				Map<Id, CarLocator> personIdCarLocatorMap = this.distanceMeterCarLocatorMap.get(distance);
				CarLocator cl = personIdCarLocatorMap.remove(event.getPersonId());
				this.inActivityDistanceCarLocatorMap.get(distance).put(event.getPersonId(), cl);
			}			
			this.personIdArrivalEventMap.put(event.getPersonId(), event);
		}
	}
	
	public void handleEvent(AgentWait2LinkEvent event) {
		this.agentsOnLink++;
		if (this.doDistanceMonitoring){
			double arrivalTime = this.personIdArrivalEventMap.remove(event.getPersonId()).getTime();
			double actTime = event.getTime() - arrivalTime;
			for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
				 Map<Id, CarLocator> inActPersonIdCarLocatorMap = this.inActivityDistanceCarLocatorMap.get(distance);
				 CarLocator cl = inActPersonIdCarLocatorMap.remove(event.getPersonId());
				 cl.setEarliestTimeInDistance(cl.getEarliestTimeInDistance() + actTime);
				 Map<Id, CarLocator> personIdCarLocatorMap = this.distanceMeterCarLocatorMap.get(distance);
				 personIdCarLocatorMap.put(event.getPersonId(), cl);
			}
		}
	}
	
}
