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

/**
 * @author dziemke
 * 
 * extracts weekday and weekend specific entries of SrV household/person/trip dataset and writes then to separate files
 */
public class SrVFileSeparator {

	// specify in- and output files
	private static String inputFile = new String("D:/VSP/Masterarbeit/Data/SrV2008/Scientific/Neukodiert/H2008_Berlin2.csv");
	private static String outputFileWeekday = new String("D:/VSP/Masterarbeit/Data/SrV2008/Scientific/Neukodiert/H2008_Berlin_Weekday.dat");
	private static String outputFileWeekend = new String("D:/VSP/Masterarbeit/Data/SrV2008/Scientific/Neukodiert/H2008_Berlin_Weekend.dat");
	
		
	public static void main(String[] args) {
		FileReader fileReader;
		BufferedReader bufferedReader;
		
		int lineCounter = 0;
				
		BufferedWriter bufferedWriterWeekday = null;
		BufferedWriter bufferedWriterWeekend = null;
		
		try {
			fileReader = new FileReader(inputFile);
			bufferedReader = new BufferedReader(fileReader);
					
			File outputWeekday = new File(outputFileWeekday);
    		FileWriter fileWriterWeekday = new FileWriter(outputWeekday);
    		bufferedWriterWeekday = new BufferedWriter(fileWriterWeekday);
    		
    		
    		File outputWeekend = new File(outputFileWeekend);
    		FileWriter fileWriterWeekend = new FileWriter(outputWeekend);
    		bufferedWriterWeekend = new BufferedWriter(fileWriterWeekend);
			
			String line = null;
						
			
			while ((line = bufferedReader.readLine()) != null) {
				String[] entry = line.split(";");
				String entryLine = line;
				
				lineCounter++;
				
				if (lineCounter == 1) {
					bufferedWriterWeekday.write(entryLine);
					bufferedWriterWeekday.newLine();
					bufferedWriterWeekend.write(entryLine);
					bufferedWriterWeekend.newLine();
				} else {
					// first entry is "ST_CODE"
					// "1" = weekday, "111" = weekend
					if (Integer.parseInt(entry[0]) == 1) {
						bufferedWriterWeekday.write(entryLine);
						bufferedWriterWeekday.newLine();
					} else if (Integer.parseInt(entry[0]) == 111) {
						bufferedWriterWeekend.write(entryLine);
						bufferedWriterWeekend.newLine();
					} else {
						System.err.println("Error!");
					}
				}
				
        			
			}
		
			
		} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the BufferedWriter
            try {
                if (bufferedWriterWeekday != null) {
                    // bufferedWriter.flush();
                    bufferedWriterWeekday.close();
                }
                
                if (bufferedWriterWeekend != null) {
                    // bufferedWriter.flush();
                    bufferedWriterWeekend.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		System.out.println("Fertig.");	
		
	}
}
	

