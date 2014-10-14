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

package playground.dziemke.analysis.cemdap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Zone2ZoneSampleFileAnalyzer {

	private static String inputFile = new String("D:/Workspace/cemdap/CEMDAP_Sample_Data/zone2zone.dat");
	
	public static void main(String[] args) {
		int lineCount = 0;
		int interiorCount = 0;
		
		double aggregateInteriorDistance = 0;
		
		FileReader fileReader;
		BufferedReader bufferedReader;
				
		try {
			fileReader = new FileReader(inputFile);
			bufferedReader = new BufferedReader(fileReader);
			
			String line = null;
			
			while ((line = bufferedReader.readLine()) != null) {
				lineCount++;
				
				String[] entry = line.split("\t");
				
				int origin = Integer.parseInt(entry[0]);
				int destination = Integer.parseInt(entry[1]);
				double distance = Double.parseDouble(entry[3]);
				
				if (origin == destination) {
					interiorCount++;
					aggregateInteriorDistance = aggregateInteriorDistance + distance;
				}
			}
			
		} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
		
		double averageInteriorDistance = aggregateInteriorDistance/interiorCount;

		System.out.println("lineCount: " + lineCount);
		System.out.println("interiorCount: " + interiorCount);
		System.out.println("averageInteriorDistance: " + averageInteriorDistance);
	}

}
