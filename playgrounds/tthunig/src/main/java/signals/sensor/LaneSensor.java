/* *********************************************************************** *
 * project: org.matsim.*
 * DgLaneSensor
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package signals.sensor;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.lanes.data.Lane;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;


/**
 * @author dgrether
 *
 */
public final class LaneSensor {

	private Link link;
	private Lane lane;
	private int agentsOnLane = 0;

	private boolean doDistanceMonitoring = false;
	private boolean doAverageVehiclesPerSecondMonitoring = false;

	private double totalVehicles = 0;
	private double monitoringStartTime;

	private Map<Double, Map<Id<Vehicle>, CarLocator>> distanceMeterCarLocatorMap = null;

	public LaneSensor(Link link, Lane lane) {
		this.link = link;
		this.lane = lane;
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
//		this.inActivityDistanceCarLocatorMap.put(distanceMeter, new HashMap<Id<Vehicle>, CarLocator>());
	}

	private void enableDistanceMonitoring() {
		this.doDistanceMonitoring = true;
		this.distanceMeterCarLocatorMap = new HashMap<Double, Map<Id<Vehicle>, CarLocator>>();
//		this.link2WaitEventPerVehicleId = new HashMap<Id<Vehicle>, VehicleLeavesTrafficEvent>();
//		this.inActivityDistanceCarLocatorMap = new HashMap<Double, Map<Id<Vehicle>, CarLocator>>();
	}

	public void handleEvent(LaneLeaveEvent event) {
		this.agentsOnLane--;
		if (this.doDistanceMonitoring){
			for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
				Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = this.distanceMeterCarLocatorMap.get(distance);
				carLocatorPerVehicleId.remove(event.getVehicleId());
			}
		}
	}

	public void handleEvent(LaneEnterEvent event) {
		this.agentsOnLane++;
		if(this.doAverageVehiclesPerSecondMonitoring) {
			totalVehicles ++;
			if(totalVehicles == 1) {
				monitoringStartTime = event.getTime();
			}
		}
		if (this.doDistanceMonitoring){
			for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
				Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = this.distanceMeterCarLocatorMap.get(distance);
				carLocatorPerVehicleId.put(event.getVehicleId(), new CarLocator(this.lane, this.link, event.getTime(), distance));
			}
		}
	}

	public int getNumberOfCarsOnLane() {
		return this.agentsOnLane;
	}

	public int getNumberOfCarsInDistance(Double distanceMeter, double now) {
		Map<Id<Vehicle>, CarLocator> distSpecificCarLocators = this.distanceMeterCarLocatorMap.get(distanceMeter);
		int count = 0;
		for (CarLocator cl : distSpecificCarLocators.values()){
			if (cl.isCarinDistance(now)){
				count++;
			}
		}
		return count;
	}

	public double getAvgVehiclesPerSecond(double now) {
		if(now > monitoringStartTime) {
			return totalVehicles / (now - monitoringStartTime);
		} else {
			return 0;
		}
	}

    public void registerAverageVehiclesPerSecondToMonitor() {
		this.doAverageVehiclesPerSecondMonitoring = true;
    }
}
