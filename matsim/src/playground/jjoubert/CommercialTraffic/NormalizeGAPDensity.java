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

import playground.jjoubert.CommercialModel.Postprocessing.EventsToGAP;

public class NormalizeGAPDensity {

	// String value that must be set
	final static String PROVINCE = "Gauteng";
	// Mac
	final static String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";
	// IVT-Sim0
//	final static String ROOT = "/home/jjoubert/";
	// Derived string values
	final static String INPUT = ROOT + PROVINCE + "/Activities/" + PROVINCE + "MinorGapStats.txt";
	final static String OUTPUT = ROOT + PROVINCE + "/Activities/" + PROVINCE + "MinorGapStats_Normalized.txt";

	public static final String DELIMITER = ",";

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		System.out.println("==================================================================");
		System.out.printf("Normalizing the GAP Density for %s.\n", PROVINCE);
		System.out.printf("==================================================================\n\n");
		
		ArrayList<ArrayList<Integer>> allLists = new ArrayList<ArrayList<Integer>>();
		double maxValue = Double.NEGATIVE_INFINITY;
		/*
		 * Read the GAP statistics file.
		 */
		System.out.printf("Reading original GAP density... ");
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File( INPUT ))));
			@SuppressWarnings("unused")
			String header = input.nextLine();
			ArrayList<Integer> list = null;
			while(input.hasNextLine()){
				String [] line = input.nextLine().split(DELIMITER);
				list = new ArrayList<Integer>();
				if( line.length > 25){
					list.add( Integer.parseInt( line[0] ));		// Add the GAP ID
					for(int i = 2; i <= 25; i++){
						list.add( Integer.parseInt( line[i] )); // Add each of the 24-hour bin values 
						maxValue = Math.max(maxValue, Double.parseDouble( line[i] ));
					}
					allLists.add(list);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.printf("Done.\n");
		
		/*
		 * Normalize the GAP statistics.
		 */
		System.out.printf("Normalizing the data... ");
		if( (allLists.size() > 0) && maxValue > Double.NEGATIVE_INFINITY ){
			for (int a = 0; a < allLists.size(); a++) {
				for (int b = 1; b < allLists.get(a).size(); b++) { // Only from index 1; not the GAP ID
					int dummy = allLists.get(a).get(b);
					allLists.get(a).set(b, (int) (( ( (double) dummy )/ ( (double) maxValue) )*100) ); 
				}
			}
		}
		System.out.printf("Done.\n");
		
		/*
		 * Write the normalized GAP statistics to file.
		 */
		System.out.printf("Writing the normalized GAP statistics to file... ");
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(OUTPUT)));
			try{
				String header = EventsToGAP.createHeaderString();
				output.write( header );
				output.newLine();

				String line = null;
				for (ArrayList<Integer> arrayList : allLists) {
					line = new String();
					for(int i = 0; i < arrayList.size()-1; i++){
						line += arrayList.get(i).toString();
						line += DELIMITER;
					}
					line += arrayList.get(arrayList.size()-1 );
					output.write(line);
					output.newLine();
				}
			} finally{
				output.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.printf("Done.\n");

	}

}
