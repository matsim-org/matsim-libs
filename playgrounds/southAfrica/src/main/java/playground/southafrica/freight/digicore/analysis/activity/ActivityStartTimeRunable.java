/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.freight.digicore.analysis.activity;

import java.io.File;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;


/**
 * A {@link Runnable} class to analyse the activity start times of (possibly) 
 * specified activity types. Start time is based on the hour of the day.
 *  
 * @author jwjoubert
 */
public class ActivityStartTimeRunable implements Runnable {
	private final Counter counter;
	private final File vehicleFile;
	private final String activityType;
	private Map<String, Integer> startTimeMap;
	
	/**
	 * Constructor.
	 * @param vehicleFile the vehicle to read and analyse;
	 * @param counter an overall counter;
	 * @param activityType the specified activity type to analyse. If all
	 * 	      activity types is analysed, the this argument should be 
	 * 		  <code>null</code>.
	 */
	public ActivityStartTimeRunable(File vehicleFile, Counter counter, String activityType) {
		this.vehicleFile = vehicleFile;
		this.counter = counter;
		
		if(activityType != null){
			if(activityType.equalsIgnoreCase("null")){
				this.activityType = null;
			} else{
				this.activityType = activityType;
			}
		} else{
			this.activityType = null;
		}
		
		this.startTimeMap = new TreeMap<String, Integer>();
	}

	@Override
	public void run() {
		/* Read the vehicle file. */
		DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
		dvr.parse(this.vehicleFile.getAbsolutePath());
		DigicoreVehicle vehicle = dvr.getVehicle();
		
		/* Analyse all the activities. */
		for(DigicoreChain chain : vehicle.getChains()){
			for(DigicoreActivity activity : chain.getAllActivities()){
				/* Check if a specific activity type was requested. */
				boolean analyse = true;
				if(this.activityType != null){
					if(!activity.getType().equalsIgnoreCase(this.activityType)){
						analyse = false;
					}
				}
				
				if(analyse){
					/* Convert hour of the day to string. */
					String hour = String.format("%02d", activity.getStartTimeGregorianCalendar().get(Calendar.HOUR_OF_DAY));
					
					/* Add the start hour to the map, or increment if it already
					 * exists. */
					if(startTimeMap.containsKey(hour)){
						int oldCount = startTimeMap.get(hour);
						startTimeMap.put(hour, oldCount+1);
					} else{
						startTimeMap.put(hour, 1);
					}
				}
			}
		}
		this.counter.incCounter();
	}
	
	public Map<String, Integer> getStartTimeMap(){
		return this.startTimeMap;
	}

}
