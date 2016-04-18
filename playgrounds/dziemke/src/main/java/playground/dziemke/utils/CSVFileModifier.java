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
import java.util.LinkedList;
import java.util.List;


public class CSVFileModifier {

	private static String inputFile = new String("../../../shared-svn/projects/tum-with-moeckel/data/mstm_annapolis/microData_annapolis/pp_2000.csv");
	private static String outputFile = new String("../../../shared-svn/projects/tum-with-moeckel/data/mstm_annapolis/microData_annapolis/pp_2000_annapolis.csv");
	private static String keyFile = new String("../../../shared-svn/projects/tum-with-moeckel/data/mstm_annapolis/microData_annapolis/hh_2000.csv");
	
	private static String seperator = ",";
	private static int keyColumnNumber = 0;
	
	public static void main(String[] args) {
//		List<Integer> validEntries = createListOfValidEntries(keyFile, seperator, keyColumnNumber);
		List<Integer> validEntries = new LinkedList<>();
		validEntries.add(221);
		
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
						
			// header
			bufferedWriter.write(bufferedReader.readLine());
			bufferedWriter.newLine();
			
			while ((line = bufferedReader.readLine()) != null) {
				String[] entry = line.split(seperator);
				
				int householdId = Integer.parseInt(entry[1]);
				
				if(validEntries.contains(householdId)) {
					// (re-)write original entries if condition fullfilled
					for (int i=0; i<entry.length-1; i++) {
						bufferedWriter.write(entry[i] + seperator);
					}
					// write last entry without seperator
					bufferedWriter.write(entry[entry.length-1]);				
	        		bufferedWriter.newLine();
				}
			}
		} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the BufferedWriter
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		System.out.println("Done with writing the reduced file.");	
	}

	private static List<Integer> createListOfValidEntries(String keyFile, String seperator, int keyColumnNumber) {
		List<Integer> validEntries = new LinkedList<>();
		FileReader fileReader;
		BufferedReader bufferedReader;
				
		try {
			fileReader = new FileReader(inputFile);
			bufferedReader = new BufferedReader(fileReader);
					
			bufferedReader.readLine(); // skip header;
			
			String line = null;
						
			while ((line = bufferedReader.readLine()) != null) {
				String[] entry = line.split(seperator);
				int key = Integer.parseInt(entry[keyColumnNumber]);
				validEntries.add(key);
			}
			
		} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
		System.out.println("Done reading the key file.");	
		
		return validEntries;
	}
}