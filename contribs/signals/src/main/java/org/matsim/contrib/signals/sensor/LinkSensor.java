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
package org.matsim.contrib.signals.sensor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;


/**
 * @author dgrether
 *
 */
final class LinkSensor {
	
	private static final Logger log = Logger.getLogger(LinkSensor.class);
	
	private Link link = null;
	public int vehiclesOnLink = 0;
	private double totalVehicles = 0;
	
	private boolean doDistanceMonitoring = false;
	private boolean doAverageVehiclesPerSecondMonitoring = false;
	private Map<Double, Map<Id<Vehicle>, CarLocator>> distanceMeterCarLocatorMap = null;
	private double monitoringStartTime;

	private double lookBackTime;
	private double timeBucketCollectionDuration;
	private Queue<AtomicInteger> timeBuckets;
	private double currentBucketStartTime;
	private AtomicInteger currentBucket;

	private boolean hasCollectedEnoughBuckets = false;

	private int numOfBucketsNeededForLookback;

	/**
	 * Calculate the average number of vehicles per second accourding to the number of vehicles which passed the link from the beginning of time. Average is calculated from the first time a vehicle entered the link on.
	 * @param link
	 * @author dgrether
	 */
	public LinkSensor(Link link){
		this(link, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	/**
	 * Calculate the average number of vehicles per second accourding to the number of vehicles, which entered the link in the of the last n seconds. Not-closed buckets will not be used to calculate the average.
	 * @param link
	 * @param lookBackTime duration for which the average vehicles per second are calculated. If it isn't divisible by timeBucketSize without a remainder it will be extended to make it divisible by timeBucketDuration
	 * @param timeBucketDuration size of each bucket. It should be big enough to save runtime compared to collecting vehicles every second or simstep and small enough to get quick enough results about changes in the vehicle flow.
	 * @author pschade
	 */
	public LinkSensor(Link link, double lookBackTime, double timeBucketDuration){
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

	private void enableDistanceMonitoring() {
		this.doDistanceMonitoring = true;
		this.distanceMeterCarLocatorMap = new HashMap<Double, Map<Id<Vehicle>, CarLocator>>();
	}


	public int getNumberOfCarsOnLink() {
		return this.vehiclesOnLink;
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

	/**
	 * 
	 * @author pschade
	 */
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
	
	public void handleEvent(LinkEnterEvent event) {
		this.vehiclesOnLink++;
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
				carLocatorPerVehicleId.put(event.getVehicleId(), new CarLocator(this.link, event.getTime(), distance));
			}
		}
	}
	
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.vehiclesOnLink++;
		/* the sensor so far may not work for doAverageVehiclesPerSecondMonitoring when vehicles enter traffic at this link.
		 * the following is a quick suggestion how to fix it, but has to be tested. theresa, may'16 */
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
			for (Double distance : this.distanceMeterCarLocatorMap.keySet()) {
				Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = this.distanceMeterCarLocatorMap.get(distance);
				// as vehicles enter links at their downstream end, assume enter time was freespeed travel time ago
				double fs_tt = this.link.getLength() / this.link.getFreespeed();
				carLocatorPerVehicleId.put(event.getVehicleId(), new CarLocator(this.link, event.getTime() - fs_tt, distance));
			}				
		}
	}

	public void handleEvent(LinkLeaveEvent event) {
		vehicleLeftLink(event.getVehicleId());
	}

	public void handleEvent(VehicleLeavesTrafficEvent event) {
		vehicleLeftLink(event.getVehicleId());
		
		if(this.doAverageVehiclesPerSecondMonitoring) {
			/*
			 * Note: the sensor detects cars that will arrive at the link end, i.e. the traffic signal.
			 * Vehicles that leave traffic at the link will not reach the link end and are, therefore, not counted here.
			 * theresa, feb'17
			 */
			totalVehicles--;
		}
	}

	private void vehicleLeftLink(Id<Vehicle> vehId) {
		this.vehiclesOnLink--;
		if (this.doDistanceMonitoring){
			for (Double distance : this.distanceMeterCarLocatorMap.keySet()){
				Map<Id<Vehicle>, CarLocator> carLocatorPerVehicleId = this.distanceMeterCarLocatorMap.get(distance);
				carLocatorPerVehicleId.remove(vehId);
			}
		}
	}
}
