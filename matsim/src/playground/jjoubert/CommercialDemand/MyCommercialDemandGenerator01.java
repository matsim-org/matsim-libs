/* *********************************************************************** *
 * project: org.matsim.*
 * MyCommercialDemandGenerator01.java
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

package playground.jjoubert.CommercialDemand;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import playground.jjoubert.Utilities.MyVehicleIdentifier;

public class MyCommercialDemandGenerator01 {
	private final Logger log;
	private final String plansFolder;
	private final int numberOfSamples;
	private final double activityThreshold;
	private ArrayList<Integer> withinList;
	private ArrayList<Integer> throughList;
	
	

	public MyCommercialDemandGenerator01(String plansFilefolder, int numberOfSamples, double activitythreshold) {
		log = Logger.getLogger(MyCommercialDemandGenerator01.class);
		this.plansFolder = plansFilefolder;
		this.numberOfSamples = numberOfSamples;
		this.activityThreshold = activitythreshold;
		withinList = null;
	}
	
	/**
	 * 
	 * @param vehicleStatistics
	 * @throws RuntimeException when not being able to create <i>within</i> or 
	 * 		<i>through</i> vehicle lists using the <tt>MyVehicleIdentifier</tt> class.
	 */
	public void createPlans(String vehicleStatistics){
		log.info("Start creating plans.");
		
		// Build the lists for 'within' and 'through' vehicles.
		this.buildVehicleLists(vehicleStatistics);
		
		
		log.info("Plans completed successfully.");
	}
	
	/**
	 * First, create two <code>ArrayList</code>s, one for <i>within</i> vehicles and one for 
	 * <i>through</i> vehicles.
	 */
	private void buildVehicleLists(String vehicleStatistics){
		log.info("Building 'within' and 'through' vehicle lists.");
		MyVehicleIdentifier mvi = new MyVehicleIdentifier(activityThreshold);
		try{
			ArrayList<ArrayList<Integer>> lists = mvi.buildVehicleLists(vehicleStatistics, ",");
			withinList = lists.get(0);
			throughList = lists.get(1);
		} finally{
			if(withinList==null || throughList==null){
				throw new RuntimeException("Could not create 'within' or 'through' vehicle lists!!");
			}
		}
		log.info("Completed buidling vehicle lists.");		
	}
	
	public String getPlansFolder() {
		return plansFolder;
	}
	
	public int getNumberOfSamples() {
		return numberOfSamples;
	}
	
	public double getActivityThreshold() {
		return activityThreshold;
	}
	
	public ArrayList<Integer> getWithinList() {
		return withinList;
	}
	
	public ArrayList<Integer> getThroughList() {
		return throughList;
	}

}
