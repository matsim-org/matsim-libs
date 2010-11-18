/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.osmBB.rohdata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map.Entry;

public class CheckRohdatenIntegrity {
	
	public static void main(String[] args) {
		TreeMap<String, TreeMap<String, ArrayList<RohDataBox>>> out = ReadRohData.readCountDataForWeek("F:\\rohdaten\\tsv\\");
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File("F:\\rohdaten\\results.txt")));
			writer.write("ID\tDate\tTotal\tValid\tInvalid\tDate\tTotal\tValid\tInvalid\tDate\tTotal\tValid\tInvalid\tDate\tTotal\tValid\tInvalid\tDate\tTotal\tValid\tInvalid\tDate\tTotal\tValid\tInvalid\tDate\tTotal\tValid\tInvalid\tDate\tTotal\tValid\tInvalid\t");
			writer.newLine();
			
			for (Entry<String, TreeMap<String, ArrayList<RohDataBox>>> countStation : out.entrySet()) {
				StringBuffer strB = new StringBuffer();
				strB.append(countStation.getKey() + "\t");
				
				for (Entry<String, ArrayList<RohDataBox>> date : countStation.getValue().entrySet()) {
					strB.append(date.getKey() + "\t");
					
					int validEntries = 0;
					int invalidEntries = 0;
					
					for (RohDataBox rohDataBox : date.getValue()) {
						if(rohDataBox.isValid()){
							validEntries++;
						} else {
							invalidEntries++;
						}
					}
					
					strB.append((validEntries + invalidEntries) + "\t" + validEntries + "\t" + invalidEntries + "\t");
				}
				
				writer.write(strB.toString());
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		System.out.print("Wait");
		
	}

}
