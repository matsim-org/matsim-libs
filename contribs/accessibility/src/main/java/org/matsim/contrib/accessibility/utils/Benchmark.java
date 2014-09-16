/* *********************************************************************** *
 * project: org.matsim.*
 * Benchmark.java
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

/**
 * 
 */
package org.matsim.contrib.accessibility.utils;

import java.util.ArrayList;

import org.apache.log4j.Logger;



/**
 * @author thomas
 *
 */
public final class Benchmark {
	
	// logger
	private static final Logger log = Logger.getLogger(Benchmark.class);

	// counter in order to separate measurement tasks
	private static int measureID = 0;
	
	private ArrayList<MeasurementObject> measurements = null;
	
	public Benchmark(){
		log.info("Initializing ...");
		measurements = new ArrayList<MeasurementObject>();
	}
	
	public int addMeasure(String name){
		
		long startTime = System.currentTimeMillis();
		
		log.info("Added new measurement item (id=" + measureID + ").");
		MeasurementObject mo = new MeasurementObject(name, startTime, measureID);
		measurements.add(mo);
		measureID++;
		
		return measureID-1;
	}
	
	public void stoppMeasurement(int id){
		
		long endTime = System.currentTimeMillis();
		
		log.info("Stopping measurement (id=" + measureID + ").");
		if(id < measurements.size())
			measurements.get( id ).stopMeasurement( endTime );
	}
	
	public double getDurationInMilliSeconds(int id){
		if(measurements != null && measurements.size() > id)
			return measurements.get(id).getDuration();
		return -1.;
	}
	public double getDurationInSeconds(int id){
		double duration = getDurationInMilliSeconds(id);
		if(duration < 0.)
			return -1.;
		return duration/1000.;
	}
	
	private class MeasurementObject {
		
		private long startTime, endtime, duration;	// in milliseconds
		
		public MeasurementObject(String name, long startTime, int id){
			this.startTime = startTime;
		}
		
		public void stopMeasurement(long endTime){
			this.endtime = endTime;
			this.duration = this.endtime - this.startTime;
		}
		
		// getter methods
		public long getDuration(){
			return this.duration;
		}
	}
}

