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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Link2WaitEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;


/**
 * @author dgrether
 *
 */
public class DgSensor {
	
	private static final Logger log = Logger.getLogger(DgSensor.class);
	
	private Link link = null;
	public int vehiclesOnLink = 0;
	
	private boolean doDistanceMonitoring = false;
	private Map<Double, Map<Id<Vehicle>, CarLocator>> distanceMeterCarLocatorMap = null;
	private Map<Id<Vehicle>, Link2WaitEvent> link2WaitEventPerVehicleId = null;
	private Map<Double, Map<Id<Vehicle>, CarLocator>> inActivityDistanceCarLocatorMap = null;
	
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
		this.distanceMeterCarLocatorMap.put(distanceMeter, new HashMap<Id<Vehicle>, CarLocator>());
		this.inActivityDistanceCarLocatorMap.put(distanceMeter, new HashMap<Id<Vehicle>, CarLocator>());
	}
	
	private void enableDistanceMonitoring() {
		this.doDistanceMonitoring = true;
		this.distanceMeterCarLocatorMap = new HashMap<Double, Map<Id<Vehicle>, CarLocator>>();
		this.link2WaitEventPerVehicleId = new HashMap<Id<Vehicle>, Link2WaitEvent>();
		this.inActivityDistanceCarLocatorMap = new HashMap<Double, Map<Id<Vehicle>, CarLocator>>();
	}

	public int getNumberOfCarsOnLink() {
		return this.vehiclesOnLink;
	}

	public int getNumberOfCarsInDistance(Double distanceMeter, double timeSeconds) {
		Map<Id<Vehicle>, CarLocator> carLocators = this.distanceMeterCarLocatorMap.get(distanceMeter);
		int count = 0;
		for (CarLocator cl : carLocators.values()){
			if (cl.isCarinDistance(timeSeconds)){
				count++;
			}
		}
		return count;
	}

	public void handleEvent(LinkEnterEvent event) {
		this.vehiclesOnLink++;
		if (this.doDistanceMonitoring){
			for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
				Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = this.distanceMeterCarLocatorMap.get(distance);
				carLocatorPerVehicleId.put(event.getVehicleId(), new CarLocator(this.link, event.getTime(), distance));
			}
		}
	}
	
	public void handleEvent(LinkLeaveEvent event) {
		this.vehiclesOnLink--;
		if (this.doDistanceMonitoring){
			for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
				Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = this.distanceMeterCarLocatorMap.get(distance);
				carLocatorPerVehicleId.remove(event.getVehicleId());
			}
		}
	}
	
	public void handleEvent(Link2WaitEvent event) {
		this.vehiclesOnLink--;
		if (this.doDistanceMonitoring){
			for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
				Map<Id<Vehicle>, CarLocator> vehicleIdCarLocatorMap = this.distanceMeterCarLocatorMap.get(distance);
				CarLocator cl = vehicleIdCarLocatorMap.remove(event.getVehicleId());
				this.inActivityDistanceCarLocatorMap.get(distance).put(event.getVehicleId(), cl);
			}			
			this.link2WaitEventPerVehicleId.put(event.getVehicleId(), event);
		}
	}
	
	public void handleEvent(Wait2LinkEvent event) {
		this.vehiclesOnLink++;
		if (this.doDistanceMonitoring){
			// the vehicle leaves its first act on this link --> add a car locator
			if (! this.link2WaitEventPerVehicleId.containsKey(event.getVehicleId())){ 
//				log.debug("wait2link at: " + event.getTime() + " vehicle: "+ event.getVehicleId() + " link: " + event.getLinkId());
				for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
					Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = this.distanceMeterCarLocatorMap.get(distance);
					//as the position now is not clear, assume enter time was freespeed travel time ago
					double fs_tt = this.link.getLength() / this.link.getFreespeed();
					carLocatorPerVehicleId.put(event.getVehicleId(), new CarLocator(this.link, event.getTime() - fs_tt, distance));
				}				
			}
			else {
				double arrivalTime = this.link2WaitEventPerVehicleId.remove(event.getVehicleId()).getTime();
				double actTime = event.getTime() - arrivalTime;
				for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
					Map<Id<Vehicle>, CarLocator> carLocatorPerInActVehicleId = this.inActivityDistanceCarLocatorMap.get(distance);
					CarLocator cl = carLocatorPerInActVehicleId.remove(event.getVehicleId());
					cl.setEarliestTimeInDistance(cl.getEarliestTimeInDistance() + actTime);
					Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = this.distanceMeterCarLocatorMap.get(distance);
					carLocatorPerVehicleId.put(event.getVehicleId(), cl);
				}
			}
		}
	}
	
}
