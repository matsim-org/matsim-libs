/* *********************************************************************** *
 * project: org.matsim.*
 * NormalizeGAPDensity.java
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

package playground.jjoubert.CommercialTraffic;

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

import playground.jjoubert.CommercialModel.Postprocessing.EventsToGAP;
import playground.jjoubert.Utilities.DateString;

public class NormalizeGAPDensity {

	// String value that must be set
	final static String PROVINCE = "Gauteng";
	// Mac
	final static String ROOT = "~/MATSim/workspace/MATSimData/";
	// IVT-Sim0
//	final static String ROOT = "~/";
	// Derived string values
	final static String INPUT = ROOT + PROVINCE + "/Activities/" + PROVINCE + "MinorGapStats.txt";
	final static String OUTPUT_PRE = ROOT + PROVINCE + "/Activities/" + PROVINCE + "MinorGapStats_Normalized_";
	final static String OUTPUT_POST = ".txt";

	public static final String DELIMITER = ",";
	private final static Logger log = Logger.getLogger(NormalizeGAPDensity.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		DateString date = new DateString();
		date.setTimeInMillis(System.currentTimeMillis());
		String now = date.toString();
		String OUTPUT = OUTPUT_PRE + now + OUTPUT_POST;

		log.info("==================================================================");
		log.info("Normalizing the GAP Density for " + PROVINCE);
		log.info("==================================================================\n\n");
		
		ArrayList<ArrayList<Double>> allLists = new ArrayList<ArrayList<Double>>();
		double maxValue = Double.NEGATIVE_INFINITY;
		/*
		 * Read the GAP statistics file.
		 */
		log.info("Reading original GAP density");
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File( INPUT ))));
			@SuppressWarnings("unused")
			String header = input.nextLine();
			ArrayList<Double> list = null;
			while(input.hasNextLine()){
				String [] line = input.nextLine().split(DELIMITER);
				list = new ArrayList<Double>();
				if( line.length > 25){
					list.add( Double.parseDouble( line[0] ));		// Add the GAP ID
					for(int i = 2; i <= 25; i++){
						list.add( Double.parseDouble( line[i] )); // Add each of the 24-hour bin values 
						maxValue = Math.max(maxValue, Double.parseDouble( line[i] ));
					}
					allLists.add(list);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		log.info("Done reading GAP density.");
		
		/*
		 * Normalize the GAP statistics.
		 */
		log.info("Normalizing the data... ");
		if( (allLists.size() > 0) && maxValue > Double.NEGATIVE_INFINITY ){
			for (int a = 0; a < allLists.size(); a++) {
				for (int b = 1; b < allLists.get(a).size(); b++) { // Only from index 1; not the GAP ID
					double dummy = allLists.get(a).get(b);
					allLists.get(a).set(b, (( ( (double) dummy )/ ( (double) maxValue) )*100) ); 
				}
			}
		}
		log.info("Done normalising the data.");
		
		/*
		 * Write the normalized GAP statistics to file.
		 */
		log.info("Writing the normalized GAP statistics to file... ");
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(OUTPUT)));
			try{
				String header = EventsToGAP.createHeaderString();
				output.write( header );
				output.newLine();
				StringBuffer sb = new StringBuffer();
				for (ArrayList<Double> arrayList : allLists) {
					Integer gapID = (int) Math.floor(arrayList.get(0));
					sb.append(gapID.toString());
					sb.append(DELIMITER);
					for(int i = 1; i < arrayList.size()-1; i++){
						sb.append(arrayList.get(i).toString());
						sb.append(DELIMITER);
					}
					sb.append(arrayList.get(arrayList.size()-1 ));
					
					output.write(sb.toString());
					output.newLine();
				}
			} finally{
				output.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Done writing GAP statistics to file.");

	}

}
