/* *********************************************************************** *
 * project: org.matsim.*
 * DiaryAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.ivt;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class DiaryAnalyzer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("/Users/jillenberger/Work/socialnets/data/ivt2009/05-2011/raw/Diary.csv"));
		
		String header = reader.readLine();
		String[] colNames = header.split(";");
		int typeIdx = getIndex(colNames, "\"Activity\"");
		int accHhIdx = getIndex(colNames, "\"Activity_Acc_HH\"");
		int accOtherIdx = getIndex(colNames, "\"Activity_Acc_Others\"");
		
		DescriptiveStatistics accHhStats = new DescriptiveStatistics();
		DescriptiveStatistics accOtherStats = new DescriptiveStatistics();
		
		String line;
		while((line = reader.readLine()) != null) {
			String[] tokens = line.split(";");
			String type = tokens[typeIdx];
			if(type.equals("\"Freizeit (offen)\"")) {
				String accHh = tokens[accHhIdx];
				String accOther = tokens[accOtherIdx];
				
				if(!accHh.equals("NA")) {
					int n = Integer.parseInt(accHh);
					if(n > 0)
						accHhStats.addValue(n);
				}
				
				if(!accOther.equals("NA")) {
					int n = Integer.parseInt(accOther);
					if(n > 0)
						accOtherStats.addValue(n);
				}
			}
		}
		System.out.println("Houshold members:");
		System.out.println(accHhStats.toString());
		System.out.println("Others:");
		System.out.println(accOtherStats.toString());
	}

	private static int getIndex(String[] colNames, String match) {
		for(int i = 0; i < colNames.length; i++) {
			if(colNames[i].equals(match)) {
				return i;
			}
		}
		
		return -1;
	}
}
