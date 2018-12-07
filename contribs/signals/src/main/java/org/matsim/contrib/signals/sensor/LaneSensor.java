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
	private double timeBucketCollectionDuration;
	private double currentBucketStartTime;
	private AtomicInteger currentBucket;
	private int numOfBucketsNeededForLookback;
	private boolean hasCollectedEnoughBuckets;

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
		if (now > monitoringStartTime) {
			if (lookBackTime == Double.POSITIVE_INFINITY) {
				return totalVehicles / ((now - monitoringStartTime)+1);
			} else {
				updateBucketsUntil(now);
				//if we have less buckets collected than needed for lookback, we calculate the average only with the buckets we already have.
				if (timeBuckets.size() > 0) {
					double avgVehPerSecond = timeBuckets.stream().mapToInt(AtomicInteger::intValue).sum()/(timeBuckets.size() * this.timeBucketCollectionDuration);
					//if there wasn't any vehicles in the lookback-time but now vehicles are measured, the number of vehicles is expected on the currend, not finished bucket.
					if (avgVehPerSecond == 0.0 && currentBucket != null && currentBucket.get() > 0) {
						return currentBucket.get()/((now-currentBucketStartTime)+1);
					} else {
						return avgVehPerSecond;
					}
				} else if(timeBuckets.size() == 0 && currentBucket != null && currentBucket.get() > 0) {
					//if there wasn't any vehicles since now but now vehicles are measured, the number of vehicles is expected on the currend, not finished bucket.
					return currentBucket.get()/((now-currentBucketStartTime)+1);
				}
				else {
					return 0.0;
				}
			}
		} else {
			return 0.0;
		}
	}

    public void registerAverageVehiclesPerSecondToMonitor() {
    	registerAverageVehiclesPerSecondToMonitor(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

	public void registerAverageVehiclesPerSecondToMonitor(double lookBackTime, double timeBucketCollectionDuration) {
		if (!doAverageVehiclesPerSecondMonitoring) {
			this.doAverageVehiclesPerSecondMonitoring = true;
			this.lookBackTime = lookBackTime;
			this.timeBucketCollectionDuration = timeBucketCollectionDuration;
			this.timeBuckets = new LinkedList<AtomicInteger>();
			this.currentBucketStartTime = 0.0;
			this.currentBucket = new AtomicInteger(0);
			this.numOfBucketsNeededForLookback = (int) Math.ceil(lookBackTime / timeBucketCollectionDuration);
		}
	}
	
	/**
	 * Queues a bucket to the queue and removes an old one if already enough buckets for desired lookBackTime
	 * @param bucket The bucket to queue
	 */
	private void queueFullBucket(AtomicInteger bucket) {
		timeBuckets.add(bucket);
		if (this.hasCollectedEnoughBuckets ) {
			timeBuckets.poll();
		} else if (timeBuckets.size() >= numOfBucketsNeededForLookback) {
			hasCollectedEnoughBuckets = true;
		}	
	}
	
	/**
	 * look if:
	 * - the current bucket should be closed and a new one shpould be created and set as currentBucket
	 * - there are empty buckets, which we need to add to the list, because there wasn't any vehicles in their collection period
	 * @param time timestamp until wich the bucketqueue should be updated
	 */
	private void updateBucketsUntil(double time) {
		if (time >= currentBucketStartTime + timeBucketCollectionDuration) {
			queueFullBucket(currentBucket);
			currentBucketStartTime += timeBucketCollectionDuration;
			//look if we need to create some empty buckets which queueing we missed in the meantime because no vehicle came until last update
			for (double i = currentBucketStartTime; i <= time-this.timeBucketCollectionDuration; i += this.timeBucketCollectionDuration) {
				queueFullBucket(new AtomicInteger(0));
				currentBucketStartTime += timeBucketCollectionDuration;
			}
			currentBucket = new AtomicInteger(0);
		}
	}
	
	
}
