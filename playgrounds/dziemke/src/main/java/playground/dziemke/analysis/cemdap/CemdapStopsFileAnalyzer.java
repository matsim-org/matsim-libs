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


public class CemdapStopsFileAnalyzer {

	private static String inputFile = new String("D:/Workspace/cemdap/Output/59/stops.out1");
		
	public static void main(String[] args) {
		FileReader fileReader;
		BufferedReader bufferedReader;
				
		int minimumValue = Integer.MAX_VALUE;
		int maximumValue = Integer.MIN_VALUE;
		
		
		int counter0 = 0;
		int counter1 = 0;
		int counter2 = 0;
		int counter3 = 0;
		int counter4 = 0;
		int counter5 = 0;
		int counter6 = 0;
		int counter7 = 0;
		int counter8 = 0;
		int counter9 = 0;
		int counter10 = 0;
		int counter11 = 0;
		int counter12 = 0;
		int counter13 = 0;
		int counter14 = 0;
		int counter15 = 0;
		int counter16 = 0;
		int counter17 = 0;
		int counter18 = 0;
		int counter19 = 0;
		int counter20 = 0;
		int counter21 = 0;
				
		try {
			fileReader = new FileReader(inputFile);
			bufferedReader = new BufferedReader(fileReader);
					
			String line = null;
						
			
			while ((line = bufferedReader.readLine()) != null) {
				String[] entry = line.split("\t");
				int activityType = Integer.parseInt(entry[11]);
        		
				if (activityType < minimumValue) {
					minimumValue = activityType;
				}
				
				if (activityType > maximumValue) {
					maximumValue = activityType;
				}
				
				if (activityType == 0) {
					counter0++;
				}
				if (activityType == 1) {
					counter1++;
				}
				if (activityType == 2) {
					counter2++;
				}
				if (activityType == 3) {
					counter3++;
				}
				if (activityType == 4) {
					counter4++;
				}
				if (activityType == 5) {
					counter5++;
				}
				if (activityType == 6) {
					counter6++;
				}
				if (activityType == 7) {
					counter7++;
				}
				if (activityType == 8) {
					counter8++;
				}
				if (activityType == 9) {
					counter9++;
				}
				if (activityType == 10) {
					counter10++;
				}
				if (activityType == 11) {
					counter11++;
				}
				if (activityType == 12) {
					counter12++;
				}
				if (activityType == 13) {
					counter13++;
				}
				if (activityType == 14) {
					counter14++;
				}
				if (activityType == 15) {
					counter15++;
				}
				if (activityType == 16) {
					counter16++;
				}
				if (activityType == 17) {
					counter17++;
				}
				if (activityType == 18) {
					counter18++;
				}				
				if (activityType == 19) {
					counter19++;
				}
				if (activityType == 20) {
					counter20++;
				}
				if (activityType == 21) {
					counter21++;
				}
			}
		
			
		} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } 
		System.out.println("Minimum Value: " + minimumValue);
		System.out.println("Maximum Value: " + maximumValue);	
		System.out.println("Counter0: " + counter0);
		System.out.println("Counter1: " + counter1);
		System.out.println("Counter2: " + counter2);
		System.out.println("Counter3: " + counter3);
		System.out.println("Counter4: " + counter4);
		System.out.println("Counter5: " + counter5);
		System.out.println("Counter6: " + counter6);
		System.out.println("Counter7: " + counter7);
		System.out.println("Counter8: " + counter8);
		System.out.println("Counter9: " + counter9);
		System.out.println("Counter10: " + counter10);
		System.out.println("Counter11: " + counter11);
		System.out.println("Counter12: " + counter12);
		System.out.println("Counter13: " + counter13);
		System.out.println("Counter14: " + counter14);
		System.out.println("Counter15: " + counter15);
		System.out.println("Counter16: " + counter16);
		System.out.println("Counter17: " + counter17);
		System.out.println("Counter18: " + counter18);
		System.out.println("Counter19: " + counter19);
		System.out.println("Counter20: " + counter20);
		System.out.println("Counter21: " + counter21);
		
		
	}
}
	

