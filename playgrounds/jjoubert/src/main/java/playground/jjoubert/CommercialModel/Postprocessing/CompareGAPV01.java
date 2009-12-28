/* *********************************************************************** *
 * project: org.matsim.*
 * CompareGAPV01.java
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

package playground.jjoubert.CommercialModel.Postprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import playground.jjoubert.Utilities.DateString;

public class CompareGAPV01 {
	// String value that must be set
	final static String PROVINCE = "Gauteng";

	// Mac
	final static String ROOT = "~/MATSim/workspace/MATSimData/";
	// IVT-Sim0
//	final static String ROOT = "~/";
	// Derived string values
	final static String IN_ACTUAL = ROOT + PROVINCE + "/Activities/" + PROVINCE + "MinorGapStats_Normalized.txt";
	final static String IN_SIMULATED = ROOT + "Commercial/PostProcess/AverageSimulatedCommercialMinorGAP_Normalized.txt";
	final static String OUTPUT_PRE = ROOT + "Commercial/PostProcess/CompareGAP_Normalized_";
	final static String OUTPUT_POST_GIS = "_GIS.txt";
	final static String OUTPUT_POST_SCATTER = "_Scatter.txt";
	final static String OUTPUT_POST_BOXPLOT_HOUR = "_BoxPlotHour.txt";

	public static final String DELIMITER = ",";

	/**
	 * The class reads two GAP density files:
	 *   	<ul>
 	 *		<li>the normalized actual number of activities; and
	 *   	<li>the normalized simulated number of activities. This may be averages of multiple runs.
	 *   	</ul>
	 * 
	 * It then compares the activity densities of the two input files and produces three output files:
	 * 		<ul>
	 * 		<li>a file with the GAP_ID, and a column for each hour of the day. This file is suitable
	 * 			for reading it into ArcGIS for the geographic representation of the differences; and
	 *  	<li>a file with an entry for each unique GAP_ID and hour combination, and two columns: one
	 *  		one for the simulated activities, and one for the actual activities. This file is 
	 *  		suitable for generating a scatter plot comparison of the actual vs simulated activities.
	 *  	<li>a file for generating a box plot in R. It has only two values per entry: the hour, and 
	 *  		the normalized difference. There is an entry for each unique GAP_ID hour combination.
	 *		</ul>
	 * @author johanwjoubert
	 */
	public static void main(String[] args) {
		/*
		 * Just creates a date string in the format YYYYMMDDHHMMSS
		 */
		DateString date = new DateString();
		date.setTimeInMillis(System.currentTimeMillis());
		String outputStringGIS = OUTPUT_PRE + date.toString() + OUTPUT_POST_GIS;
		String outputStringScatter = OUTPUT_PRE + date.toString() + OUTPUT_POST_SCATTER;
		String outputStringBoxHour = OUTPUT_PRE + date.toString() + OUTPUT_POST_BOXPLOT_HOUR;
		
		try {
			Scanner inputActual = new Scanner(new BufferedReader(new FileReader(new File(IN_ACTUAL))));
			Scanner inputSim = new Scanner(new BufferedReader(new FileReader(new File(IN_SIMULATED))));
			String header = inputActual.nextLine();
			header = inputSim.nextLine();

			BufferedWriter outputGIS = new BufferedWriter(new FileWriter(new File (outputStringGIS)));
			BufferedWriter outputScatter = new BufferedWriter(new FileWriter(new File (outputStringScatter)));
			BufferedWriter outputBoxHour = new BufferedWriter(new FileWriter(new File (outputStringBoxHour)));
			try{
				/*
				 * For the GIS output file, it writes the same header string as the input file(s). The 
				 * header of the two files should be the same anyway.
				 */
				outputGIS.write(header);
				outputGIS.newLine();
				/*
				 * For the scatter plot out file, it writes a basic header. 
				 */
				outputScatter.write("Name" + DELIMITER + "Hour" + DELIMITER + "Actual" + DELIMITER + "Simulated");
				outputScatter.newLine();
				/*
				 * For the box plot output file, it writes a header similar to the input file.
				 */
				outputBoxHour.write(header);
				outputBoxHour.newLine();
				
				while(inputActual.hasNextLine() && inputSim.hasNextLine()){
					String [] lineActual = inputActual.nextLine().split( DELIMITER );
					String [] lineSim = inputSim.nextLine().split( DELIMITER );
					if(!lineActual[0].equalsIgnoreCase(lineSim[0])){
						/*
						 * Checks at every line if the two input files have the same GAP_ID; aborts the 
						 * run if not. 
						 */
						throw new RuntimeException("Two GAP_IDs are not the same!!");
					} else{
						/*
						 * Writes the GAP_ID
						 */
						outputGIS.write( lineActual[0] + DELIMITER );
						outputScatter.write( lineActual[0] + DELIMITER );
						outputBoxHour.write( lineActual[0] + DELIMITER );
						Double valueActual = Double.NEGATIVE_INFINITY;
						Double valueSim = Double.POSITIVE_INFINITY;
						Double difference = Double.POSITIVE_INFINITY; 
						/*
						 * Repeat for hours 0 through 22
						 */
						for (int a = 1; a < lineActual.length - 1; a++) {
							valueActual = Double.parseDouble(lineActual[a]);
							valueSim = Double.parseDouble(lineSim[a]);
							difference = valueSim - valueActual;
							/*
							 * Writes the difference for the specific hour to the GIS file. 
							 */
							outputGIS.write(difference.toString() + DELIMITER);
							/*
							 * Writes the simulated, then the actual value to the scatter plot output
							 * file for THIS hour only. Afterwards, a new line is created, and the current
							 * GAP_ID is written to the new line.
							 */
							String writeActual = valueActual.toString();
							String writeSimulated = valueSim.toString();
							outputScatter.write( String.valueOf(a-1));
							outputScatter.write(DELIMITER);
							outputScatter.write(writeActual);
							outputScatter.write(DELIMITER);
							outputScatter.write(writeSimulated);
							outputScatter.newLine();
							outputScatter.write( String.valueOf(lineActual[0]));
							outputScatter.write(DELIMITER);
							/*
							 * Writes the difference to the box plot output file; similar to the GIS file. 
							 * BUT, if both the actual and the simulated values are zero, write 'NA'. 
							 */
							String writeDifference = (valueActual == 0 && valueSim == 0) ? "NA" : difference.toString();
							outputBoxHour.write( writeDifference + DELIMITER );
						}
						/*
						 * Repeat only for hour 23 (no delimiter at the end).
						 */
						valueActual = Double.parseDouble(lineActual[lineActual.length - 1]);
						valueSim = Double.parseDouble(lineSim[lineSim.length - 1]);
						difference = valueSim - valueActual;
						String diff = difference.toString();
						/*
						 * GIS output
						 */
						outputGIS.write(diff);
						outputGIS.newLine();
						/*
						 * Scatter plot
						 */
						String writeActual = valueActual.toString();
						String writeSimulated = valueSim.toString();
						outputScatter.write( String.valueOf(lineActual.length - 2 ) + DELIMITER );
						outputScatter.write(writeActual + DELIMITER + writeSimulated );
						outputScatter.newLine();
						/*
						 * Boxplot
						 */
						String writeDifference23 = (valueActual == 0 && valueSim == 0) ? "NA" : difference.toString();
						outputBoxHour.write( writeDifference23 );
						outputBoxHour.newLine();
					}
				}					
			} finally{
				outputGIS.close();
				outputScatter.close();
				outputBoxHour.close();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
