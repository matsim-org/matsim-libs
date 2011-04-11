/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.bvgAna.level1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;

/**
 * Evaluates the delay a vehicle reports at a stop when arriving or departing. Writes a histogram to console or file. A negative delay is counted as no delay (0s).
 * 
 * @author aneumann
 *
 */
public class VehDelayAtStopHistogram implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler{
	
	private final Logger log = Logger.getLogger(VehDelayAtStopHistogram.class);
	private final Level logLevel = Level.DEBUG;
	
	private int[] arrivalDelay;
	private int[] departureDelay;
	
	/**
	 * 
	 * @param numberOfDetailedSlots Number of separate slots. Each slot aggregates the delay within that minute, e.g. slot 1 delay from 1s to 59s.
	 * Delays <= 0s are counted in the first slot, delays >= <code>numberOfDetailedSlots</code> in the last slot.
	 */
	public VehDelayAtStopHistogram(int numberOfDetailedSlots){
		this.log.setLevel(this.logLevel);
		this.arrivalDelay = new int[numberOfDetailedSlots + 2];
		this.departureDelay = new int[numberOfDetailedSlots + 2];
	}
	
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		int delay = (int) event.getDelay() / 60;
		if(delay < 0){
			delay = 0;
		}
		
		if(delay > this.arrivalDelay.length - 2){
			delay = this.arrivalDelay.length - 1;
		}
		
		this.arrivalDelay[delay]++;		
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		int delay = (int) event.getDelay() / 60;
		if(delay < 0){
			delay = 0;
		}
		
		if(delay > this.departureDelay.length - 2){
			delay = this.departureDelay.length -1;
		}
		
		this.departureDelay[delay]++;	
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}
	
	/**
	 * Write the resulting histogram to the console.
	 */
	public void dumpToConsole() {
		this.log.info("Dumping arrival delay histogram...");
		System.out.println("delay; count");
		for (int i = 0; i < this.arrivalDelay.length; i++) {
			System.out.println(i + "; " + this.arrivalDelay[i]);
		}
		this.log.info("Dumping departure delay histogram...");
		System.out.println("delay; count");
		for (int i = 0; i < this.departureDelay.length; i++) {
			System.out.println(i + "; " + this.departureDelay[i]);
		}
	}
	
	/**
	 * Write the resulting histogram to file.
	 *
	 * @param filename The file the histogram is written to.
	 */
	public void write(String filename) {
		this.log.info("Writing results to " + filename);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
			writer.write("Dumping arrival delay histogram..."); writer.newLine();
			writer.write("delay; count"); writer.newLine();
			for (int i = 0; i < this.arrivalDelay.length; i++) {
				writer.write(i + "; " + this.arrivalDelay[i]); writer.newLine();
			}
			
			writer.write("Dumping departure delay histogram..."); writer.newLine();
			writer.write("delay; count"); writer.newLine();
			for (int i = 0; i < this.departureDelay.length; i++) {
				writer.write(i + "; " + this.departureDelay[i]); writer.newLine();
			}
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
