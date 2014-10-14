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


public class CemdapLOSFileModifier {

	// specify in- and output files and number of last column that stays unchanged
	// start counting column with 1 (not with 0).
	private static String inputFile = new String("D:/Workspace/container/demand/input/cemdap_samples/losoffpkam.dat");
	private static String outputFile = new String("D:/Workspace/container/demand/input/cemdap_samples_reduced/losoffpkam.dat");
	
	private static int numberOfLastUnchangedColumn = 2; 
	
	
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
				for (int i=0; i<numberOfLastUnchangedColumn; i++) {
					bufferedWriter.write(entry[i] + "\t" );
				}
				
				//	write zeros
				for (int i=numberOfLastUnchangedColumn; i<entry.length-1; i++) {
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
	

