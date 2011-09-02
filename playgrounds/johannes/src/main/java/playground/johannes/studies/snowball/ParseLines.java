/* *********************************************************************** *
 * project: org.matsim.*
 * ParseLines.java
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
package playground.johannes.studies.snowball;

import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class ParseLines {

	private static int maxKey = 0;
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		TIntObjectHashMap<String> lines1 = readFile("/Users/jillenberger/Work/shared-svn/projects/socialnets/papers/drafts/small-world/fig/data/condmat/apl-sampled.alpha30.conf80.txt");
		TIntObjectHashMap<String> lines2 = readFile("/Users/jillenberger/Work/shared-svn/projects/socialnets/papers/drafts/small-world/fig/data/condmat/apld-sampled.alpha30.conf80.txt");
		
		BufferedWriter writer1 = new BufferedWriter(new FileWriter("/Users/jillenberger/Work/shared-svn/projects/socialnets/papers/drafts/small-world/fig/data/condmat/apl-sampled.alpha30.conf80.filtered.txt"));
		BufferedWriter writer2 = new BufferedWriter(new FileWriter("/Users/jillenberger/Work/shared-svn/projects/socialnets/papers/drafts/small-world/fig/data/condmat/apld-sampled.alpha30.conf80.filtered.txt"));
		
		writer1.write("10\t20\t30\t40\t50\t60\t70\t80\t90\t100\t");
		writer1.newLine();
		
		writer2.write("10\t20\t30\t40\t50\t60\t70\t80\t90\t100\t");
		writer2.newLine();
		
		for(int i = 1; i <= maxKey; i++) {
			String line1 = lines1.get(i);
			String line2 = lines2.get(i);
			
			if(line1 != null && line2 != null) {
				writer1.write(line1);
				writer1.newLine();
				
				writer2.write(line2);
				writer2.newLine();
			}
		}
		
		writer1.close();
		writer2.close();

	}

	private static TIntObjectHashMap<String> readFile(String file) throws IOException {
		TIntObjectHashMap<String> lines = new TIntObjectHashMap<String>();
		BufferedReader reader1 = new BufferedReader(new FileReader(file));
		String line = reader1.readLine();
		while((line = reader1.readLine()) != null) {
			int idx = Integer.parseInt(line.split("\t")[0]);
			lines.put(idx, line);
			
			maxKey = Math.max(maxKey, idx);
		}
		reader1.close();
		return lines;
	}
}
