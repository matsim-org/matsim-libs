/* *********************************************************************** *
 * project: org.matsim.*
 * MYWithinTrafficDemandGenerator01.java
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

package playground.jjoubert.CommercialDemandGenerator.WithinTraffic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import org.apache.log4j.Logger;

public class MyWithinTrafficDemandGenerator01 {
	private final Logger log;
	private final String studyArea;
	private final String root;
	
	public MyWithinTrafficDemandGenerator01(String root, String studyArea){
		log = Logger.getLogger(MyWithinTrafficDemandGenerator01.class);
		this.studyArea = studyArea;
		this.root = root;
		
		/*
		 * There are a number of parameters that can be set. These parameters influence 
		 * the extent of the cumulative distribution functions created. 
		 */
		final int dimensionStart = 24; 			// values 00h00m00 - 23h59m59
		final int dimensionActivities = 21; 	// index '0' should never be used
		final int dimensionDuration = 49; 		// index '0' should never be used

		/*
		 * TODO Establish list of 'within' vehicles.
		 */
	}
	
	private Collection<Integer> findVehicleList(){
		Collection<Integer> vehicleList = new ArrayList<Integer>();
		String vehicleStatsFilename = root + studyArea + "/Activities/" + studyArea + "VehicleStats.txt";
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(vehicleStatsFilename))));
			input.nextLine();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return vehicleList;
	}
	
	/**
	 * TODO Build cumulative distribution function.
	 */
	
	public void createWithinDemand(Integer populationSize, Integer firstIndex){
		
	}
}
