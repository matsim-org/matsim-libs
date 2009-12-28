/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractWithinActivityDurations.java
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

package playground.jjoubert.TemporaryCode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.log4j.Logger;

import playground.jjoubert.Utilities.MyVehicleIdentifier;


public class ExtractWithinActivityDurations {

	/**
	 * @param args
	 */
	private static final String ROOT = "~/MATSim/workspace/MATSimData/";
	private final static Logger log = Logger.getLogger(ExtractWithinActivityDurations.class);

	public static void main(String[] args) {
		ArrayList<Integer> withinVehicles = new ArrayList<Integer>();
		ArrayList<Integer> withinDurations = new ArrayList<Integer>();
		
		// build ArrayList of 'within' vehicles
		String fileToRead = ROOT + "Gauteng/Activities/GautengVehiclestats.txt";
		
		MyVehicleIdentifier mvi = new MyVehicleIdentifier(0.9, 1.0);
		withinVehicles = mvi.buildVehicleList(fileToRead, ",");
		
		log.info("Building ArrayList of 'within' activity durations... ");
		
		try {
			File activityFile = new File(ROOT + "Gauteng/Activities/GautengMinorLocations.txt");
//			File activityFile = new File(ROOT + "CommercialDemand/InputData/Test.txt");
			Scanner durationScan = new Scanner(new BufferedReader(new FileReader(activityFile)));
			durationScan.nextLine();

			while(durationScan.hasNextLine()){
				String [] line = durationScan.nextLine().split(",");
				if(line.length == 5){
					int vehicleId = Integer.parseInt(line[0]);
					int duration = Integer.parseInt(line[4]);
					if(withinVehicles.contains(vehicleId)){
						withinDurations.add(duration);
					}
				}
			}		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		log.info("Done. (" + withinDurations.size() + " activities)");
		
		log.info("Writing 'within' activity durations to file... ");
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(ROOT + "CommercialDemand/Inputdata/gautengWithindurations.txt")));
			try{
				for (int i = 0; i < withinDurations.size(); i++) {
					output.write(withinDurations.get(i).toString());
					output.newLine();
				}
			}finally{
				output.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Done.");
		log.info("Completed successfully!");
	}

}
