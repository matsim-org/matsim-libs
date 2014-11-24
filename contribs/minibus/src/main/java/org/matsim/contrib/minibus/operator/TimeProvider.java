/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.operator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

/**
 * Will consider all activities up to 30 o'clock. All remaining activities are dropped.
 * 
 * @author aneumann
 *
 */
public final class TimeProvider implements ActivityStartEventHandler, ActivityEndEventHandler{
	
	private final static Logger log = Logger.getLogger(TimeProvider.class);

    private final double timeSlotSize;
	private int[] weights = null;
	private BufferedWriter writer = null;
	
	public TimeProvider(PConfigGroup pConfig, String outputDir){
		this.timeSlotSize = pConfig.getTimeSlotSize();

        double maxTime = 30.0 * 3600.0;
        int numberOfSlots = TimeProvider.getSlotForTime(maxTime, this.timeSlotSize);
		if (numberOfSlots == 0) {
			log.warn("Calculated number of slots is zero. MaxTime: " + maxTime + ", timeSlotSize: " + this.timeSlotSize);
			numberOfSlots = 1;
			log.warn("Number of slots is increased to " + numberOfSlots);
		}
		this.weights = new int[numberOfSlots];
		
		new File(outputDir + PConstants.statsOutputFolder).mkdir();
		this.writer = IOUtils.getBufferedWriter(outputDir + PConstants.statsOutputFolder + "timeSlots2weight.txt");
		StringBuffer strB = new StringBuffer();
		
		for (int i = 0; i < weights.length; i++) {
			strB.append("; " + i * timeSlotSize);
		}
		
		try {
			this.writer.write("# iteration" + strB.toString()); this.writer.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	@Override
	public void reset(int iteration) {
		// New Iteration - write the old weights to file and set the new ones as current
		this.writeToFile(this.writer, this.weights, iteration);
		this.weights = new int[this.weights.length];
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// Any activity will be tracked
		this.addOneToTimeSlot(event.getTime());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		// Any activity will be tracked
		this.addOneToTimeSlot(event.getTime());
	}

	public double getRandomTimeInInterval(double startTime, double endTime) {
		int startSlot = TimeProvider.getSlotForTime(startTime, this.timeSlotSize);
		int endSlot = TimeProvider.getSlotForTime(endTime, this.timeSlotSize);
		
		if (startSlot >= this.weights.length) {
			log.info("Resetting start slot from " + startSlot + " to " + (this.weights.length - 1));
			startSlot = this.weights.length -1;
		}
			
		if (endSlot >= this.weights.length) {
			log.info("Resetting end slot from " + endSlot + " to " + (this.weights.length - 1));
			endSlot = this.weights.length - 1;
		}
		
		int numberOfValidSlots = endSlot - startSlot + 1;
		
		// get total weight of all valid time slots
		int totalWeight = 0;
		for (int i = startSlot; i <= endSlot; i++) {
			totalWeight += this.weights[i];
		}
		
		if (totalWeight == 0.0) {
			log.info("Total weight is zero. Probably first iteration. Will pick time slots randomly.");
			double rnd = MatsimRandom.getRandom().nextDouble() * numberOfValidSlots;
			double accumulatedWeight = 0.0;
			for (int i = startSlot; i <= endSlot; i++) {
				accumulatedWeight += 1.0;
				if(accumulatedWeight >= rnd){
					return i * this.timeSlotSize;
				}
			}
			// TODO Double-check
			log.warn("Could not find a random time slot between: " + startSlot + " and " + endSlot);
			
		} else {
			double rnd = MatsimRandom.getRandom().nextDouble() * totalWeight;
			double accumulatedWeight = 0.0;
			for (int i = startSlot; i <= endSlot; i++) {
				accumulatedWeight += this.weights[i];
				if (accumulatedWeight >= rnd) {
					return i * this.timeSlotSize;
				}
			}
			// TODO Double-check for null weights 
		}
		
		log.warn("Could not find any time slot. This should not happen. Check time slot size in config. Will return the start slot time");
		return startSlot * this.timeSlotSize;
	}
	
	private void writeToFile(BufferedWriter writer, int[] weights, int currentIteration) {
		StringBuffer strB = new StringBuffer();
		strB.append(currentIteration);

        for (int weight : weights) {
            strB.append("; " + weight);
        }
		
		try {
			writer.write(strB.toString()); writer.newLine();
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void addOneToTimeSlot(double time) {
		int timeSlot = getSlotForTime(time, this.timeSlotSize);
		if(timeSlot < this.weights.length) {
			this.weights[timeSlot]++;
		}
	}
	
	public double getTimeSlotSize(){
		return this.timeSlotSize;
	}

	public static int getSlotForTime(double time, double timeSlotSize){
		return (int) (time / timeSlotSize);
	}
}
