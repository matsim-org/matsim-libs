/* *********************************************************************** *
 * project: org.matsim.*
 * Merge.java
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
package playground.johannes.socialnetworks.snowball2.sim.postprocess;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author illenberger
 *
 */
public class Merge {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String rootDir = args[0];
		
		int dumpStart = Integer.parseInt(args[1]);
		int dumpEnd = Integer.parseInt(args[2]);;
		int dumpStep = Integer.parseInt(args[3]);
		
		String constParamKey = args[4];
		int constParam = Integer.parseInt(args[5]);
		String property = args[6];
		
		String output = args[7];
		
		TIntObjectHashMap<TIntDoubleHashMap> table = new TIntObjectHashMap<TIntDoubleHashMap>();
		SortedSet<Integer> dumpKeys = new TreeSet<Integer>();
		
		for(int dumpKey = dumpStart; dumpKey <= dumpEnd; dumpKey += dumpStep) {
			TIntDoubleHashMap row = new TIntDoubleHashMap();
			
			BufferedReader reader;
			if(constParamKey.equalsIgnoreCase("alpha"))
				reader = new BufferedReader(new FileReader(String.format("%1$s/seed.%2$s/alpha.%3$s/%4$s.avr.txt", rootDir, dumpKey, constParam, property)));
			else if(constParamKey.equalsIgnoreCase("seed"))
				reader = new BufferedReader(new FileReader(String.format("%1$s/seed.%2$s/alpha.%3$s/%4$s.avr.txt", rootDir, constParam, dumpKey, property)));
			else
				throw new IllegalArgumentException(String.format("Constant parameter %1$s unknown.", constParamKey));
			
			String line;
			line = reader.readLine();
			while((line = reader.readLine()) != null) {
				String tokens[] = line.split("\t");
				int key = (int) Double.parseDouble(tokens[0]);
				double val = Double.parseDouble(tokens[1]);
				
				row.put(key, val);
				dumpKeys.add(key);
			}
			reader.close();
			table.put(dumpKey, row);
		}

		write(table, output, dumpKeys);
	}
	
	private static void write(TIntObjectHashMap<TIntDoubleHashMap> table, String output, SortedSet<Integer> dumpKeys) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
		
		int[] paramVals = table.keys();
		Arrays.sort(paramVals);
		
		for(int val : paramVals) {
			writer.write(String.valueOf(val));
			writer.write("\t");
		}
		writer.newLine();
		
		for(Integer key : dumpKeys) {
			writer.write(key.toString());
			writer.write("\t");
			for(int seed : paramVals) {
				TIntDoubleHashMap row = table.get(seed);
				if(row.containsKey(key)) {
					writer.write(String.valueOf(row.get(key)));
				} else {
					writer.write("NA");
				}
				writer.write("\t");
			}
			writer.newLine();
		}
		writer.close();
	}

}
