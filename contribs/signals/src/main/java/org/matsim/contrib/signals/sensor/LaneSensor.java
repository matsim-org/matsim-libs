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
package org.matsim.contrib.signals.sensor;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author dgrether
 *
 */
final class LaneSensor {

	private Link link;
	private Lane lane;
	private int agentsOnLane = 0;

	private boolean doDistanceMonitoring = false;
	private boolean doAverageVehiclesPerSecondMonitoring = false;

	private double totalVehicles = 0;
	private double monitoringStartTime;

	private Map<Double, Map<Id<Vehicle>, CarLocator>> distanceMeterCarLocatorMap = null;
	private double lookBackTime;
	private LinkedList<AtomicInteger> timeBuckets;
	private double timeBucketSize;
	private double currentBucketStartTime;
	private AtomicInteger currentBucket;
	private int numOfBucketsNeededForLookback;

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
			this.doDistanceMonitoring = true;
			this.distanceMeterCarLocatorMap = new HashMap<>();
		}
		this.distanceMeterCarLocatorMap.put(distanceMeter, new HashMap<>());
	}

	public void handleEvent(LaneEnterEvent event) {
		this.agentsOnLane++;
		if(this.doAverageVehiclesPerSecondMonitoring) {
			if (lookBackTime != Double.POSITIVE_INFINITY) {
				updateBucketsUntil(event.getTime());
				currentBucket.incrementAndGet();
			}
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

	public void handleEvent(LaneLeaveEvent event) {
		this.agentsOnLane--;
		if (this.doDistanceMonitoring){
			for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
				Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = this.distanceMeterCarLocatorMap.get(distance);
				carLocatorPerVehicleId.remove(event.getVehicleId());
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
		double avgVehPerSecond = 0.;

		if (now > monitoringStartTime) {
			if (lookBackTime == Double.POSITIVE_INFINITY) {
				avgVehPerSecond = totalVehicles / (now - monitoringStartTime + 1);
			} else {
				updateBucketsUntil(now);
				if (timeBuckets.size() > 0) {
					//if we have less buckets collected than needed for lookback, we calculate the average only with the buckets we already have.
					avgVehPerSecond = timeBuckets.stream().mapToInt(AtomicInteger::intValue).sum()/(timeBuckets.size() * this.timeBucketSize);
				} 
				if((timeBuckets.size() == 0 || avgVehPerSecond == 0.0 )
					&& currentBucket != null && currentBucket.get() > 0) {
					/* if there hasn't been any vehicle in the lookback-time but in the current bucket vehicles are measured, 
					 * we take only the current bucket for evaluation 
					 * (which is not finished and therefore not part of the timeBuckets list): */
					avgVehPerSecond = currentBucket.get()/(now-currentBucketStartTime + 1);
				}
			}
		} 
		return avgVehPerSecond;
	}

    public void registerAverageVehiclesPerSecondToMonitor() {
    	registerAverageVehiclesPerSecondToMonitor(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

	public void registerAverageVehiclesPerSecondToMonitor(double lookBackTime, double timeBucketSize) {
		if (!doAverageVehiclesPerSecondMonitoring) {
			this.doAverageVehiclesPerSecondMonitoring = true;
			this.lookBackTime = lookBackTime;
			this.timeBucketSize = timeBucketSize;
			this.timeBuckets = new LinkedList<AtomicInteger>();
			this.currentBucketStartTime = 0.0;
			this.currentBucket = new AtomicInteger(0);
			this.numOfBucketsNeededForLookback = (int) Math.ceil(lookBackTime / timeBucketSize);
		}
	}
	
	/**
	 * Queues a bucket to the queue and removes an old one if already enough buckets for desired lookBackTime
	 * @param bucket The bucket to queue
	 */
	private void queueFullBucket(AtomicInteger bucket) {
		timeBuckets.add(bucket);
		if (timeBuckets.size() > numOfBucketsNeededForLookback) {
			timeBuckets.poll();
		}	
	}
	
	/**
	 * check if:
	 * - the current bucket should be closed and a new one should be created and set as currentBucket
	 * - there are empty buckets, which we need to add to the list, because there wasn't any vehicles in their collection period
	 */
	private void updateBucketsUntil(double now) {
		if (now >= currentBucketStartTime + timeBucketSize) {
			queueFullBucket(currentBucket);
			currentBucketStartTime += timeBucketSize;
			// create empty bucket in case there was a time where no vehicles have arrived
			while (currentBucketStartTime <= now - timeBucketSize) {
				queueFullBucket(new AtomicInteger(0));
				currentBucketStartTime += timeBucketSize;
			}
			currentBucket = new AtomicInteger(0);
		}
	}
	
	
}
