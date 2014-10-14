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

package playground.dziemke.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class CemdapZoneFileModifier {

	// specify in- and output files and number of last column that stays unchanged
	// start counting column with 1 (not with 0).
	private static String inputFile = new String("D:/Workspace/container/demand/input/cemdap_samples/zones.dat");
	private static String outputFile = new String("D:/Workspace/container/demand/input/cemdap_samples_reduced/zones.dat");
	
	// private int numberOfLastUnchangedColumn = 14; 
	
	
	public static void main(String[] args) {
		FileReader fileReader;
		BufferedReader bufferedReader;
				
		BufferedWriter bufferedWriter = null;
		
		try {
			fileReader = new FileReader(inputFile);
			bufferedReader = new BufferedReader(fileReader);
					
			File output = new File(outputFile);
    		FileWriter fileWriter = new FileWriter(output);
    		bufferedWriter = new BufferedWriter(fileWriter);
			
			String line = null;
						
			
			while ((line = bufferedReader.readLine()) != null) {
				String[] entry = line.split("\t");
				
				// (re-)write original entries
				
				// gradually put non-required (according to documentation) variables to zero
				// which are in columns before column 14
				
				// for (int i=0; i<numberOfLastUnchangedColumn; i++) {
					// bufferedWriter.write(entry[0] + "\t" + 0 + "\t" + entry[2] + "\t" + entry[3] + "\t" + entry[4]
					//		+ "\t" + entry[5] + "\t" + entry[6] + "\t" + entry[7] + "\t" + 0 + "\t" + 0
					//		+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t");
					bufferedWriter.write(entry[0] + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
								+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
								+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t");
				//}
				
				// write zeros
				// here number hard-coded
				for (int i=14; i<entry.length-1; i++) {
					bufferedWriter.write(0 + "\t");
				}
				// write last entry without tab split
				bufferedWriter.write("0");				
        		bufferedWriter.newLine();
        			
			}
		
			
		} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the BufferedWriter
            try {
                if (bufferedWriter != null) {
                    // bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		System.out.println("Fertig.");	
		
	}
}
	

