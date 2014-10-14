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


public class LOSSampleFileAnalyzer {


//	private static String inputFile = new String("D:/Workspace/container/demand/input/cemdap_samples/losoffpkam.dat");
//	private static String inputFile = new String("D:/Workspace/cemdap/CEMDAP_Sample_Data/losoffpkam.dat");
//	private static String inputFile = new String("D:/Workspace/cemdap/CEMDAP_Sample_Data/losoffpkpm.dat");
	private static String inputFile = new String("D:/Workspace/cemdap/CEMDAP_Sample_Data/lospeakam.dat");
//	private static String inputFile = new String("D:/Workspace/cemdap/CEMDAP_Sample_Data/lospeakpm.dat");
	
	public static void main(String[] args) {
		int lineCount = 0;
		
		double aggregateDistance = 0.;
		double aggregateAutoIVTT = 0.;
		double aggregateAutoOVTT = 0.;
		double aggregateCost = 0.;
		
		double aggregateDistanceCostRatio = 0.;
		double aggregateDistanceAutoIVTTRatio = 0.;
		
		FileReader fileReader;
		BufferedReader bufferedReader;
				
		try {
			fileReader = new FileReader(inputFile);
			bufferedReader = new BufferedReader(fileReader);
			
			String line = null;
			
			while ((line = bufferedReader.readLine()) != null) {
				lineCount++;
				
				String[] entry = line.split("\t");
								
				double distance = Double.parseDouble(entry[4]);
				double autoIVTT = Double.parseDouble(entry[5]);
				double autoOVTT = Double.parseDouble(entry[6]);
				double cost = Double.parseDouble(entry[11]);
				
				double distanceCostRatio = distance/cost;
				double distanceAutoIVTTRatio = distance/autoIVTT;
				
				aggregateDistance = aggregateDistance + distance;
				aggregateAutoIVTT = aggregateAutoIVTT + autoIVTT;
				aggregateAutoOVTT = aggregateAutoOVTT + autoOVTT;
				aggregateCost = aggregateCost + cost;
				
				aggregateDistanceCostRatio = aggregateDistanceCostRatio + distanceCostRatio;
				aggregateDistanceAutoIVTTRatio = aggregateDistanceAutoIVTTRatio + distanceAutoIVTTRatio;
			}
			
		} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
		
		double averageDistance = aggregateDistance/lineCount;
		double averageAutoIVTT = aggregateAutoIVTT/lineCount;
		double averageAutoOVTT = aggregateAutoOVTT/lineCount;
		double averageCost = aggregateCost/lineCount;
		
		double averageDistanceCostRatio = aggregateDistanceCostRatio/lineCount;
		double averageDistanceAutoIVTTRatio = aggregateDistanceAutoIVTTRatio/lineCount;
		
		System.out.println("averageDistance: " + averageDistance);
		System.out.println("averageAutoIVTT: " + averageAutoIVTT);
		System.out.println("averageAutoOVTT: " + averageAutoOVTT);
		System.out.println("averageCost : " + averageCost);
				
		System.out.println("averageDistanceCostRatio: " + averageDistanceCostRatio);
		System.out.println("averageDistanceAutoIVTTRatio: " + averageDistanceAutoIVTTRatio);
		System.out.println("averageAutoIVTTDistanceRatio: " + 1/averageDistanceAutoIVTTRatio);
	}

	
}
	

