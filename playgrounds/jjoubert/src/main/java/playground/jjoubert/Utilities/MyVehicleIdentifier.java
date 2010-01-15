/* *********************************************************************** *
 * project: org.matsim.*
 * MyWithinThroughVehicleSplitter.java
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

package playground.jjoubert.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

public class MyVehicleIdentifier {
	
	private final Logger log;
	private final double lowerThreshold;
	private final double upperThreshold;
	private final double threshold;
	
	/**
	 * This utility class is useful in creating a list of <code>Integer</code> vehicle
	 * IDs based on activity percentages from a vehicle statistics file, usually created 
	 * from running the <tt>playground.jjoubert.CommercialTraffic.ActivityLocations</tt> 
	 * class.
	 * @param lowerThreshold of type <code>double</code>, the lowest (exclusive) percentage
	 * 		  of activities within the study area that will allow the vehicle to be 
	 * 		  included. 
	 * @param upperThreshold of type <code>double</code>, the highest (inclusive) percentage
	 * 		  of activities within the study area that will allow the vehicle to be 
	 * 		  included. 
	 * @author jwjoubert
	 */
	public MyVehicleIdentifier(double lowerThreshold, double upperThreshold) {
		log = Logger.getLogger(MyVehicleIdentifier.class);
		this.lowerThreshold = lowerThreshold;
		this.upperThreshold = upperThreshold;
		this.threshold = Double.MIN_VALUE;
	}
	/**
	 * This utility class is useful in creating a list of <code>Integer</code> vehicle
	 * IDs based on activity percentages from a vehicle statistics file, usually created 
	 * from running the <tt>playground.jjoubert.CommercialTraffic.ActivityLocations</tt> 
	 * class.
	 * @param a single threshold of type <code>double</code>, indicating the highest 
	 * 		percentage (inclusive) of vehicle activity that will be considered as a
	 * 		'through' traffic vehicle. Any activity percentage higher will be considered
	 * 		a 'within' traffic vehicle.
	 * @author jwjoubert
	 */	
	public MyVehicleIdentifier(double threshold){
		log = Logger.getLogger(MyVehicleIdentifier.class);
		this.lowerThreshold = Double.MIN_VALUE;
		this.upperThreshold = Double.MIN_VALUE;
		this.threshold = threshold;
	}
	
	/**
	 * The method reads a vehicle statistics file, usually created from running the 
	 * <tt>playground.jjoubert.CommercialTraffic.ActivityAnalysis.RunCommercialActivityAnalyser</tt> 
	 * class, and extracts all the vehicles with activity percentages between the 
	 * lower and the upper threshold as stipulated in the constructor.
	 * <h5>File format:</h5>
	 * 		<ul>
	 * 		The file should have seven fields, of which the significant ones for this 
	 * 		method is the first field (vehicle ID), and the fifth field (percentage
	 * 		of 'minor' activities in the study area). An example of the file is given here:
	 * 		<br><br> 
	 * 		<tt>
	 * 		VehicleId,a,b,c,PercentageMinor,PercentageMajor,d<br>
	 * 		129976,6,6,1486,0.10666666666666667,0.234,1682364<br>
	 *		118739,53,123,1338,0.1716833890746934,0.15,18084400<br>
	 *		94535,2,4,1672,0.0,0.0,0<br>
	 *		...
	 * 		</tt></ul>
	 * @param fileToRead the <code>String</code> containing the absolute path of the 
	 * 		vehicle statistics file.
	 * @param delimiter the <code>String</code> delimiter that is used in the input file.
	 * @return A <code>List</code><<code>Integer</code>> where each 
	 * 		<code>Integer</code> represents a vehicle ID. 
	 * @throws RuntimeException when the constructor was created with a single threshold; 
	 * 		implying that the <code>buildVehicleLists(...)</code> method should rather be 
	 * 		used.  
	 */
	public List<Integer> buildVehicleList(String fileToRead, String delimiter) {
		if(threshold==Double.MIN_VALUE && lowerThreshold!=Double.MIN_VALUE && upperThreshold!=Double.MIN_VALUE){
			log.info("Building a List of vehicle IDs");
			log.info("   Lower threshold for inclusion: " + lowerThreshold);
			log.info("   Upper threshold for inclusion: " + upperThreshold);			
		} else{
			log.warn("Thresholds not specified correctly!");
			throw new RuntimeException("Consider using 'buildVehicleLists(...)' method!");
		}
		List<Integer> list = new ArrayList<Integer>();
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(fileToRead))));
			input.nextLine();
			while(input.hasNextLine()){
				String [] line = input.nextLine().split(delimiter);
				if(line.length == 7){
					int vehicleId = Integer.parseInt(line[0]);
					double percentage = Double.parseDouble(line[4]);
					if(percentage > lowerThreshold && percentage <= upperThreshold){
						list.add(vehicleId);
					}
				} else{
					log.warn("A line read from " + fileToRead + "  was not the correct length!");
				}
			}		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		log.info("Done (" + list.size() + " vehicles)");
		return list;
	}
	
	/**
	 * The method reads a vehicle statistics file, usually created from running the 
	 * <tt>playground.jjoubert.CommercialTraffic.ActivityLocations</tt> class, and 
	 * extracts all 'through' traffic vehicles as those with activity percentages
	 * greater than zero, and less than or equal to a given threshold; and 'within'
	 * traffic vehicles as those with activity percentages greater than the given
	 * threshold.
	 * <h5>File format:</h5>
	 * 		<ul>
	 * 		The file should have seven fields, of which the significant ones for this 
	 * 		method is the first field (vehicle ID), and the fifth field (percentage
	 * 		of 'minor' activities in the study area). An example of the file is given here:
	 * 		<br><br> 
	 * 		<tt>
	 * 		VehicleId,a,b,c,PercentageMinor,PercentageMajor,d<br>
	 * 		129976,6,6,1486,0.10666666666666667,0.234,1682364<br>
	 *		118739,53,123,1338,0.1716833890746934,0.15,18084400<br>
	 *		94535,2,4,1672,0.0,0.0,0<br>
	 *		...
	 * 		</tt></ul>
	 * @param fileToRead the <code>String</code> containing the absolute path of the 
	 * 		vehicle statistics file.
	 * @param delimiter the <code>String</code> delimiter that is used in the input file.
	 * @return A <code>List</code><<code>List</code><<code>Integer</code>>> 
	 * 		where each <code>Integer</code> represents a vehicle ID. The first 
	 * 		<code>List</code><<code>Integer</code>> represents the 'within' 
	 * 		traffic vehicles; and the second represents the 'through' traffic vehicles.
	 * @throws RuntimeException when the constructor was created with separate <i>lower</i>
	 * 		and <i>upper</i> thresholds; implying that the <code>buildVehicleList(...)</code>
	 * 		method should rather be used.    
	 */	
	public List<List<Integer>> buildVehicleLists(String fileToRead, String delimiter) {
		if(threshold!=Double.MIN_VALUE && lowerThreshold==Double.MIN_VALUE && upperThreshold==Double.MIN_VALUE){
			log.info("Building Lists of vehicle IDs");
			log.info("   Threshold distinguishing between 'within' and 'through' traffic: " + threshold);
		} else{
			log.warn("Thresholds not specified correctly!");
			throw new RuntimeException("Consider using 'buildVehicleList(...)' method");
		}
		List<List<Integer>> lists = new ArrayList<List<Integer>>(2);
		List<Integer> withinList = new ArrayList<Integer>();
		List<Integer> throughList = new ArrayList<Integer>();
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(fileToRead))));
			input.nextLine();
			while(input.hasNextLine()){
				String [] line = input.nextLine().split(delimiter);
				if(line.length == 7){
					int vehicleId = Integer.parseInt(line[0]);
					double percentage = Double.parseDouble(line[4]);
					if(percentage > threshold){
						withinList.add(vehicleId);
					} else if(percentage > 0){
						throughList.add(vehicleId);
					}
				} else{
					log.warn("A line read from " + fileToRead + "  was not the correct length!");
				}
			}					
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		lists.add(withinList);
		lists.add(throughList);
		return lists;
	}
	

	/**
	 * @return The method returns the lower threshold (percentage) of vehicle activities in the
	 * study area for a vehicle to be considered.
	 */
	public Double getLowerThreshold() {
		return this.lowerThreshold;
	}
	
	/**
	 * @return The method returns the upper threshold (percentage) of vehicle activities in the
	 * study area for a vehicle to be considered.
	 */	
	public Double getUpperThreshold() {
		return this.upperThreshold;
	}
	
	/**
	 * @return The method returns the threshold (percentage) of vehicle activities in the 
	 * study area below (and including) which a vehicle is considered a <i>through</i> 
	 * traffic  vehicle. Vehicles with activities exceeding (not including) the threshold 
	 * are considered <i>within</i> traffic vehicles.
	 */
	public Double getThreshold() {
		return threshold;
	}


}
