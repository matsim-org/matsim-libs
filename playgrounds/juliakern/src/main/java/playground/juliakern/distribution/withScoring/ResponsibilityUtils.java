/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAveragingForLinkEmissions.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.juliakern.distribution.withScoring;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.juliakern.distribution.EmActivity;
import playground.juliakern.distribution.EmPerCell;
import playground.juliakern.distribution.ResponsibilityEvent;
import playground.juliakern.distribution.withScoringFast.ResponsibilityEventImpl;

public class ResponsibilityUtils {

	
	/*
	 * calculate responsibility and exposure 
	 * from activities
	 * 
	 * -> generate responsibility(and exposure) events
	 */
	public void addExposureAndResponsibilityBinwise(
			ArrayList<EmActivity> activities,
			Map<Double, ArrayList<EmPerCell>> emissionPerBin,
			ArrayList<ResponsibilityEvent> responsibility, Double timeBinSize, Double simulationEndTime) {
		
		/*
		 * for each activity:
		 * split into time intervalls accoriding to time bin sizes
		 * check emissions in corresponding cell and time bin
		 * 
		 * -> generate responsibility (and exposure) events
		 */
		for(EmActivity ema: activities){
			
			//receiving/exposed person id
			Id exposedPersonId = ema.getPersonId();
			
			// activity location
			int xBin = ema.getXBin();
			int yBin = ema.getYBin();
			
			// all responsibility events for this activity
			ArrayList<ResponsibilityEvent> currentREvents = new ArrayList<ResponsibilityEvent>();
			
			// split activity according to time bins 
			Double startTime = ema.getStartTime();
			Double endTime = ema.getEndTime();
			
			// number of time bins matching activity time
			int firstTimeBin = (int) Math.ceil(startTime/timeBinSize);
			if(firstTimeBin==0)firstTimeBin=1;
			int lastTimeBin;
			if(ema.getEndTime()>0.0){
					lastTimeBin = (int) Math.ceil(endTime/timeBinSize);
			}else{
					lastTimeBin = (int) Math.ceil(simulationEndTime/timeBinSize);
			}
			
			// calculate responsibility events
			// case distinction - number of time bins
			if (firstTimeBin<lastTimeBin){
				
				//first bin
				Double firstStartTime = startTime;
				Double firstEndTime = firstTimeBin * timeBinSize;
				currentREvents.addAll(generateResponsibilityEventsForCell(exposedPersonId, emissionPerBin.get(firstTimeBin*timeBinSize), firstTimeBin, xBin, yBin, firstStartTime, firstEndTime));
				// inner time bins
				for (int i = firstTimeBin + 1; i < lastTimeBin; i++) {
					Double currentStartTime = (i-1)*timeBinSize; // TODO check!
					Double currentEndTime = i*timeBinSize;
					currentREvents.addAll(generateResponsibilityEventsForCell(exposedPersonId, emissionPerBin.get(i*timeBinSize), i, xBin, yBin, currentStartTime, currentEndTime));
				}
				// last bin
				Double lastStartTime = (lastTimeBin-1)*timeBinSize;
				Double lastEndTime = endTime;
				currentREvents.addAll(generateResponsibilityEventsForCell(exposedPersonId, emissionPerBin.get(lastTimeBin*timeBinSize), lastTimeBin, xBin, yBin, lastStartTime, lastEndTime));
				
			}else{ // activity entirely in one interval
				currentREvents.addAll(generateResponsibilityEventsForCell(exposedPersonId, emissionPerBin.get(firstTimeBin*timeBinSize), firstTimeBin, xBin, yBin, startTime, endTime));
			}
			
			// calculate exposure
			responsibility.addAll(currentREvents);
	
		}		
	}

	
	/*
	 * In: exposed person Id, start time of activity, end time of activity, location of activity
	 * 	list of generated emissions of corresponding time bin 
	 * 
	 * -> for each generated emission matching location and time of that activity
	 * generate a responsibility (and exposure) event
	 * (responsible person, exposed person, start time of exposure, end time of exposure, location of exposure,
	 *  emission concentration)
	 */
	private ArrayList<ResponsibilityEvent> generateResponsibilityEventsForCell(
			Id exposedPersonId, ArrayList<EmPerCell> emissionPerBinOfCurrentTimeBin, int firstTimeBin,
			int xBin, int yBin, Double startTime, Double endTime) {
		
		ArrayList<ResponsibilityEvent> rEvents= new ArrayList<ResponsibilityEvent>();
		
		if (emissionPerBinOfCurrentTimeBin!=null) {
			
			for (EmPerCell epb : emissionPerBinOfCurrentTimeBin) {
				if (epb.getXbin().equals(xBin) && epb.getYbin().equals(yBin)) {					
					String location = "x = " + epb.getXbin().toString()	+ ", y = " + epb.getYbin();
					ResponsibilityEvent ree = new ResponsibilityEventImpl(epb.getPersonId(), exposedPersonId, startTime, endTime, epb.getConcentration(), location);
					System.out.println("epb conc" + epb.getConcentration() + "x " + epb.getXbin() + "y" + epb.getYbin());
					rEvents.add(ree);
				}
			}
		}
		return rEvents;
	}
}
