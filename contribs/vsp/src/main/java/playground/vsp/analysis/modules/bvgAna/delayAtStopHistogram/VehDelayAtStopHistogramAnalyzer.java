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

/**
 * 
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.bvgAna.delayAtStopHistogram;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * Evaluates the delay a vehicle reports at a stop when arriving or departing. Writes a histogram to file. A negative delay is counted as no delay (0s).
 * 
 * @author ikaddoura, aneumann
 *
 */
public class VehDelayAtStopHistogramAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(VehDelayAtStopHistogramAnalyzer.class);
	private MutableScenario scenario;
	private VehDelayAtStopHistogramEventHandler delayHandler;
	private int numberOfDetailedSlots;
	
	private int[] arrivalDelay;
	private int[] departureDelay;
	
	/**
	 * 
	 * @param numberOfDetailedSlots Number of separate slots. Each slot aggregates the delay within that minute, e.g. slot 1 delay from 1s to 59s.
	 * Delays <= 0s are counted in the first slot, delays >= <code>numberOfDetailedSlots</code> in the last slot.
	 */
	public VehDelayAtStopHistogramAnalyzer(int numberOfDetailedSlots) {
		super(VehDelayAtStopHistogramAnalyzer.class.getSimpleName());
		this.numberOfDetailedSlots = numberOfDetailedSlots;
	}
	
	public void init(MutableScenario scenario) {
		this.scenario = scenario;
		this.delayHandler = new VehDelayAtStopHistogramEventHandler(this.numberOfDetailedSlots);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.delayHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
		// nothing to do
	}

	@Override
	public void postProcessData() {
		this.departureDelay = this.delayHandler.getDepartureDelay();
		this.arrivalDelay = this.delayHandler.getArrivalDelay();
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName1 = outputFolder + "vehDelayAtStopHistogram.txt";
		File file1 = new File(fileName1);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file1));
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

	public int[] getArrivalDelay() {
		return arrivalDelay;
	}

	public int[] getDepartureDelay() {
		return departureDelay;
	}
	
	

}
