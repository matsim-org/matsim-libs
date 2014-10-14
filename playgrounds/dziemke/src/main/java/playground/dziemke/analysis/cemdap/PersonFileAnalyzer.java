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


public class PersonFileAnalyzer {


	private static String inputFile = new String("D:/Workspace/container/demand/input/cemdap_berlin/19/persons1.dat");
	
	public static void main(String[] args) {
		int lineCount = 0;
		
		int aggregateEmployed = 0;
		int aggregateStudent = 0;
		int aggregateLicense = 0;
		int noWorkTSZ = 0;
		int noSchTSZ = 0;
		int aggregateFemale = 0;
		int aggregateAge = 0;
		int aggregateParent = 0;
		
		FileReader fileReader;
		BufferedReader bufferedReader;
				
		try {
			fileReader = new FileReader(inputFile);
			bufferedReader = new BufferedReader(fileReader);
			
			String line = null;
			
			while ((line = bufferedReader.readLine()) != null) {
				lineCount++;
				
				String[] entry = line.split("\t");
				
				int employed = Integer. parseInt(entry[2]);
				int student = Integer. parseInt(entry[3]);
				int license = Integer. parseInt(entry[4]);
				int workTSZ = Integer. parseInt(entry[5]);
				int schTSZ = Integer. parseInt(entry[6]);
				int female = Integer. parseInt(entry[7]);
				int age = Integer. parseInt(entry[8]);
				int parent = Integer. parseInt(entry[9]);
				
				aggregateEmployed = aggregateEmployed + employed;
				aggregateStudent = aggregateStudent + student;
				aggregateLicense = aggregateLicense + license;
				
				if (workTSZ == -99) {
					noWorkTSZ++;
				}
				
				if (schTSZ == -99) {
					noSchTSZ++;
				}
				
				aggregateFemale = aggregateFemale + female;
				aggregateAge = aggregateAge + age;
				aggregateParent = aggregateParent + parent;
			}
			
		} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
		
		double averageAge = aggregateAge/lineCount;
				
		System.out.println("Line Count: " + lineCount);
		System.out.println("Empoloyed: " + aggregateEmployed);
		System.out.println("Student: " + aggregateStudent);
		System.out.println("License: " + aggregateLicense);
		System.out.println("NoWorkTSZ: " + noWorkTSZ);
		System.out.println("NoSchTSZ: " + noSchTSZ);
		System.out.println("Female: " + aggregateFemale);
		System.out.println("Average Age: " + averageAge);
		System.out.println("Parent: " + aggregateParent);		
	}

}
	

