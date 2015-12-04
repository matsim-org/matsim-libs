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
package playground.johannes.studies.snowball.postprocess;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author illenberger
 *
 */
public class ConfidenceInterval {

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
		String dumpProperty = args[7];
		String output = args[8];
		
		final double mean = Double.parseDouble(args[9]);
		double confProba = Double.parseDouble(args[10]);
		String mode = args[11];
		
		TIntObjectHashMap<TIntDoubleHashMap> table = new TIntObjectHashMap<TIntDoubleHashMap>();
		SortedSet<Integer> dumpKeys = new TreeSet<Integer>();
		
		for(int dumpKey = dumpStart; dumpKey <= dumpEnd; dumpKey += dumpStep) {
			TIntDoubleHashMap row = new TIntDoubleHashMap();
			
			BufferedReader valueReader;
			BufferedReader dumpReader;
			if(constParamKey.equalsIgnoreCase("alpha")) {
				String path = String.format("%1$s/seed.%2$s/alpha.%3$s/%4$s.txt", rootDir, dumpKey, constParam, property);
				valueReader = new BufferedReader(new FileReader(path));
				System.out.println("Loading file "+path);
				
				path = String.format("%1$s/seed.%2$s/alpha.%3$s/%4$s.avr.txt", rootDir, dumpKey, constParam, dumpProperty);
				dumpReader = new BufferedReader(new FileReader(path));
				System.out.println("Loading file "+path);
			} else if(constParamKey.equalsIgnoreCase("seed")) {
				String path = String.format("%1$s/seed.%2$s/alpha.%3$s/%4$s.txt", rootDir, constParam, dumpKey, property);
				valueReader = new BufferedReader(new FileReader(path));
				System.out.println("Loading file "+path);
				
				path = String.format("%1$s/seed.%2$s/alpha.%3$s/%4$s.avr.txt", rootDir, constParam, dumpKey, dumpProperty);
				dumpReader = new BufferedReader(new FileReader(path));
				System.out.println("Loading file "+path);
			} else
				throw new IllegalArgumentException(String.format("Constant parameter %1$s unknown.", constParamKey));
			
			String header = valueReader.readLine();
			String keys[] = header.split("\t");
			int cols = keys.length;
			String valueLine;
			Map<String, TDoubleArrayList> matrix = new HashMap<String, TDoubleArrayList>();
			while((valueLine = valueReader.readLine()) != null) {
				String[] tokens = valueLine.split("\t");
				for(int i = 0; i < cols; i++) {
					TDoubleArrayList list = matrix.get(keys[i]);
					if(list == null) {
						list = new TDoubleArrayList();
						matrix.put(keys[i], list);
					}
					
					list.add(Double.parseDouble(tokens[i]));
				}
			}
			
			String dumpLine;
			Map<String, String> dumpMapping = new HashMap<String, String>();
			while((dumpLine = dumpReader.readLine()) != null) {
				String[] tokens = dumpLine.split("\t");
				dumpMapping.put(tokens[0], tokens[1]);
			}
			
			for(Entry<String, TDoubleArrayList> entry : matrix.entrySet()) {
				DescriptiveStatistics stats = new DescriptiveStatistics();
				
				double vals[] = entry.getValue().toArray();
				for(double val : vals) {	
					if(!Double.isNaN(val)) {
						double relerr;
						if(mode.equals("abs")) {
							relerr = Math.abs((val - mean)/mean);
						} else {
							relerr = (val - mean)/mean;
						}
						stats.addValue(relerr);
				
					}
				}
				if(stats.getN() < 50) {
					System.err.println("Less than 50 samples. Ignoring dump.");
				} else {
					double conf;
					if(mode.equals("abs"))
						conf = stats.getPercentile(confProba);
					else if(mode.equals("pos")) {
						confProba = (100 - confProba)/2.0;
						conf = stats.getPercentile(100 - confProba);
					} else if(mode.equals("neg")) {
						confProba = (100 - confProba)/2.0;
						conf = stats.getPercentile(confProba);
					} else {
						throw new IllegalArgumentException(String.format("Mode %1$s unknown.", mode));
					}
					// int key = Integer.parseInt(keys[i]);
					String keyStr = entry.getKey();
					if(!dumpMapping.get(keyStr).equals("null")) {
						int key = (int) Double.parseDouble(dumpMapping.get(keyStr));
						row.put(key, conf);
						dumpKeys.add(key);
					} else {
						System.err.println("Null key");
					}
				}
			}
			table.put(dumpKey, row);
		}

		write(table, output, dumpKeys);
	}
	
//	private static void write(TIntObjectHashMap<TIntDoubleHashMap> table, String output, SortedSet<Integer> dumpKeys) throws IOException {
//		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
//		
//		int[] paramVals = table.keys();
//		Arrays.sort(paramVals);
//		
//		for(int val : paramVals) {
//			writer.write(String.valueOf(val));
//			writer.write("\t");
//		}
//		writer.newLine();
//		
//		for(Integer key : dumpKeys) {
//			writer.write(key.toString());
//			writer.write("\t");
//			for(int seed : paramVals) {
//				TIntDoubleHashMap row = table.get(seed);
//				if(row.containsKey(key)) {
//					writer.write(String.valueOf(row.get(key)));
//				} else {
//					writer.write("NA");
//				}
//				writer.write("\t");
//			}
//			writer.newLine();
//		}
//		writer.close();
//	}
	
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
			for(int alpha : paramVals) {
				TIntDoubleHashMap row = table.get(alpha);
				if(row.containsKey(key)) {
					writer.write(String.valueOf(row.get(key)));
				} else {
					int[] keys = row.keys();
					Arrays.sort(keys);
					int idx = Arrays.binarySearch(keys, key);
					if(idx < 0) {
						idx = -idx-2;
					}
					if(idx == keys.length - 1 || idx == -1)
						writer.write("NA");
					else
						writer.write(String.valueOf(row.get(keys[idx])));
				}
				writer.write("\t");
			}
			writer.newLine();
		}
		writer.close();
	}


}
